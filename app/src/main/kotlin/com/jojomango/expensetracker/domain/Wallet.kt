package com.jojomango.expensetracker.domain

/**
 * SPEC.md §3.4 三種預算模式。`budgetAmount` 是否可為 null 由這個模式決定
 * （見 [Wallet] 的 init 檢查）。
 */
enum class BudgetMode { NONE, WEEKLY, TOTAL }

/**
 * SPEC.md §3.1 錢包。`currency` 建立後不可修改——這條規則在 UI/Repository 層
 * 執行（不提供「修改幣別」的操作），型別本身不需要額外表達不可變性。
 */
data class Wallet(
    val id: String,
    val name: String,
    val currency: String,
    val budgetMode: BudgetMode,
    val budgetAmount: Long?,
    val archived: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Wallet name must not be blank" }
        when (budgetMode) {
            BudgetMode.NONE ->
                require(budgetAmount == null) { "budgetAmount must be null when budgetMode is NONE" }
            BudgetMode.WEEKLY, BudgetMode.TOTAL ->
                require(budgetAmount != null && budgetAmount >= 0) {
                    "budgetAmount must be non-negative when budgetMode is $budgetMode"
                }
        }
    }
}

/** 嘗試刪除最後一個錢包時拋出——SPEC.md §3.1「不可刪除最後一個錢包」。 */
class LastWalletException(
    message: String,
) : IllegalStateException(message)

/** [existingWalletCount] 是刪除前的錢包總數（含即將被刪的那一個）。 */
fun assertCanDeleteWallet(existingWalletCount: Int) {
    if (existingWalletCount <= 1) {
        throw LastWalletException("Cannot delete the last remaining wallet")
    }
}
