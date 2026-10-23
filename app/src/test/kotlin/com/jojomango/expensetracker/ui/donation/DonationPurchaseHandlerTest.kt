package com.jojomango.expensetracker.ui.donation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T9.2 — 贊助流程（SPEC.md §3.8）。購買結果先轉成 [DonationPurchase] 這個純資料型別，
 * 「要不要 consume、要不要謝謝」的判斷不碰 Play Billing SDK，所以可以在純 JVM 測。
 */
class DonationPurchaseHandlerTest {
    private fun purchased(
        token: String,
        product: String = DonationTier.SMALL.productId,
    ) = DonationPurchase(purchaseToken = token, productIds = listOf(product), state = DonationPurchase.State.PURCHASED)

    @Test
    @DisplayName("T9.2.1 — 已付款：consume 這筆購買並顯示感謝")
    fun `T9 2 1`() {
        val handler = DonationPurchaseHandler()
        val actions = handler.onPurchasesUpdated(DonationBillingResult.Ok, listOf(purchased("tok-1")))
        assertEquals(
            listOf(DonationAction.Consume("tok-1", DonationTier.SMALL), DonationAction.ShowMessage(DonationMessage.THANKS)),
            actions,
        )
    }

    @Test
    @DisplayName("T9.2.2 — 使用者取消付款：不顯示任何訊息")
    fun `T9 2 2`() {
        val actions = DonationPurchaseHandler().onPurchasesUpdated(DonationBillingResult.UserCanceled, emptyList())
        assertTrue(actions.isEmpty())
    }

    @Test
    @DisplayName("T9.2.3 — Play 無法連線：顯示「目前無法連線到 Google Play，請稍後再試」")
    fun `T9 2 3`() {
        val actions = DonationPurchaseHandler().onPurchasesUpdated(DonationBillingResult.Unavailable, emptyList())
        assertEquals(listOf(DonationAction.ShowMessage(DonationMessage.UNAVAILABLE)), actions)
    }

    @Test
    @DisplayName("T9.2.4 — 待處理（PENDING，例如超商代碼繳費）：不 consume、不感謝")
    fun `T9 2 4`() {
        val pending = purchased("tok-p").copy(state = DonationPurchase.State.PENDING)
        val actions = DonationPurchaseHandler().onPurchasesUpdated(DonationBillingResult.Ok, listOf(pending))
        assertTrue(actions.isEmpty())
    }

    @Test
    @DisplayName("T9.2.4 — 待處理的購買之後變成已付款，才 consume 並感謝")
    fun `pending then purchased`() {
        val handler = DonationPurchaseHandler()
        handler.onPurchasesUpdated(DonationBillingResult.Ok, listOf(purchased("tok-p").copy(state = DonationPurchase.State.PENDING)))
        val actions = handler.onPurchasesUpdated(DonationBillingResult.Ok, listOf(purchased("tok-p")))
        assertEquals(DonationAction.Consume("tok-p", DonationTier.SMALL), actions.first())
    }

    @Test
    @DisplayName("T9.2.5 — 啟動時查到已付款但還沒 consume 的購買：補做 consume（不然消耗型商品不能再買）")
    fun `T9 2 5`() {
        val actions = DonationPurchaseHandler().onUnconsumedPurchasesFound(listOf(purchased("tok-old", DonationTier.LARGE.productId)))
        assertEquals(listOf(DonationAction.Consume("tok-old", DonationTier.LARGE)), actions)
    }

    @Test
    @DisplayName("T9.2.6 — 同一筆購買被回報兩次：只 consume 一次、只感謝一次")
    fun `T9 2 6`() {
        val handler = DonationPurchaseHandler()
        val first = handler.onPurchasesUpdated(DonationBillingResult.Ok, listOf(purchased("tok-1")))
        val second = handler.onPurchasesUpdated(DonationBillingResult.Ok, listOf(purchased("tok-1")))
        assertEquals(2, first.size)
        assertTrue(second.isEmpty())
    }

    @Test
    @DisplayName("不是贊助商品的購買一律忽略（這個 App 沒有其他內購，防止誤 consume）")
    fun `unknown product ignored`() {
        val other = purchased("tok-x", product = "something_else")
        assertTrue(DonationPurchaseHandler().onPurchasesUpdated(DonationBillingResult.Ok, listOf(other)).isEmpty())
    }

    @Test
    @DisplayName("三個商品 id 與 SPEC.md §3.8 一致")
    fun `product ids match spec`() {
        assertEquals(
            listOf("donation_small", "donation_medium", "donation_large"),
            DonationTier.entries.map { it.productId },
        )
    }

    @Test
    @DisplayName("T9.2.3 — 三個商品都查到才顯示選項；查不到任何一個、或 Play 回錯誤，都改顯示無法連線")
    fun `offers need all three products`() {
        val all = mapOf(DonationTier.SMALL to "NT$30", DonationTier.MEDIUM to "NT$90", DonationTier.LARGE to "NT$300")
        assertEquals(DonationOffers.Available(all), donationOffers(DonationBillingResult.Ok, all))
        assertEquals(DonationOffers.Unavailable, donationOffers(DonationBillingResult.Ok, all - DonationTier.LARGE))
        assertEquals(DonationOffers.Unavailable, donationOffers(DonationBillingResult.Unavailable, all))
    }
}
