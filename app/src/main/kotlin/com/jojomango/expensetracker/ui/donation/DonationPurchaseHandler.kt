package com.jojomango.expensetracker.ui.donation

/** UI-SPEC.md §10.2 的兩則訊息；文字在 string resources（`donation_thanks`、`donation_unavailable`）。 */
enum class DonationMessage { THANKS, UNAVAILABLE }

@get:androidx.annotation.StringRes
val DonationMessage.stringRes: Int
    get() =
        when (this) {
            DonationMessage.THANKS -> com.jojomango.expensetracker.R.string.donation_thanks
            DonationMessage.UNAVAILABLE -> com.jojomango.expensetracker.R.string.donation_unavailable
        }

/** SPEC.md §3.8 的三個消耗型商品。實際金額在 Play Console 設定，App 只認 id。 */
enum class DonationTier(
    val productId: String,
    /** 送給分析的值（SPEC.md §3.7 `donation_completed` 的 `tier`）。 */
    val analyticsValue: String,
) {
    SMALL("donation_small", "small"),
    MEDIUM("donation_medium", "medium"),
    LARGE("donation_large", "large"),
    ;

    companion object {
        fun fromProductId(productId: String): DonationTier? = entries.firstOrNull { it.productId == productId }
    }
}

/** Play Billing 的 `Purchase` 轉成的純資料——讓下面的判斷邏輯可以在純 JVM 測（T9.2）。 */
data class DonationPurchase(
    val purchaseToken: String,
    val productIds: List<String>,
    val state: State,
) {
    enum class State { PURCHASED, PENDING, UNSPECIFIED }

    val tier: DonationTier? get() = productIds.firstNotNullOfOrNull(DonationTier::fromProductId)
}

/** Play Billing 回應碼的歸類；只分出畫面需要區別對待的幾種。 */
sealed interface DonationBillingResult {
    data object Ok : DonationBillingResult

    /** 使用者在 Play 付款畫面按取消：不是錯誤，什麼都不顯示（T9.2.2）。 */
    data object UserCanceled : DonationBillingResult

    /** Play 連不上、帳號不能付款、查不到商品——一律顯示 [DonationMessage.UNAVAILABLE]（T9.2.3）。 */
    data object Unavailable : DonationBillingResult
}

/** 「支持開發者」展開後顯示什麼（UI-SPEC.md §10.2）。 */
sealed interface DonationOffers {
    /** 價格還沒從 Play 回來：按鈕顯示「—」並 disabled。 */
    data object Loading : DonationOffers

    data object Unavailable : DonationOffers

    /** Play 回傳的在地化價格字串（例如「NT$30.00」），App 不自己算金額（SPEC.md §3.8）。 */
    data class Available(
        val prices: Map<DonationTier, String>,
    ) : DonationOffers
}

/** 只查到一部分商品（例如 Play Console 有一個還沒啟用）也當成無法使用：只顯示兩個按鈕會讓人以為介面壞了。 */
fun donationOffers(
    result: DonationBillingResult,
    prices: Map<DonationTier, String>,
): DonationOffers =
    if (result == DonationBillingResult.Ok && prices.keys.containsAll(DonationTier.entries)) {
        DonationOffers.Available(prices)
    } else {
        DonationOffers.Unavailable
    }

sealed interface DonationAction {
    data class Consume(
        val purchaseToken: String,
        val tier: DonationTier,
    ) : DonationAction

    data class ShowMessage(
        val message: DonationMessage,
    ) : DonationAction
}

/**
 * 收到購買結果之後要做什麼。消耗型商品如果沒有 consume，使用者**就不能再買同一個商品**，
 * 所以 consume 這件事要做得穩：
 * - 待處理（PENDING，例如超商代碼繳費）的不能 consume——錢還沒進來（T9.2.4）
 * - App 在 consume 之前被關掉的話，下次啟動要從查到的購買補做（T9.2.5）
 * - Play 偶爾會把同一筆購買回報不只一次，同一個 token 只處理一次（T9.2.6）
 */
class DonationPurchaseHandler {
    private val handledTokens = mutableSetOf<String>()

    fun onPurchasesUpdated(
        result: DonationBillingResult,
        purchases: List<DonationPurchase>,
    ): List<DonationAction> =
        when (result) {
            DonationBillingResult.UserCanceled -> emptyList()
            DonationBillingResult.Unavailable -> listOf(DonationAction.ShowMessage(DonationMessage.UNAVAILABLE))
            DonationBillingResult.Ok ->
                claimNewPurchases(purchases).flatMap { (token, tier) ->
                    listOf(DonationAction.Consume(token, tier), DonationAction.ShowMessage(DonationMessage.THANKS))
                }
        }

    /** 啟動時查到的「已付款但還沒 consume」：補做 consume，不重複感謝（使用者付款當下可能已經看過）。 */
    fun onUnconsumedPurchasesFound(purchases: List<DonationPurchase>): List<DonationAction> =
        claimNewPurchases(purchases).map { (token, tier) -> DonationAction.Consume(token, tier) }

    private fun claimNewPurchases(purchases: List<DonationPurchase>): List<Pair<String, DonationTier>> =
        purchases.mapNotNull { purchase ->
            val tier = purchase.tier
            if (purchase.state != DonationPurchase.State.PURCHASED || tier == null || !handledTokens.add(purchase.purchaseToken)) {
                null
            } else {
                purchase.purchaseToken to tier
            }
        }
}
