package com.jojomango.expensetracker.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** SPEC.md §3.2：方向由 [type] 決定，`amount` 永遠為正。 */
enum class TransactionType { EXPENSE, INCOME }

/**
 * SPEC.md §3.2 交易。（v2.12）每筆交易自己存 [currency]，可以跟錢包的預設幣別不同；金額的小數位數依它決定。
 * [date] 只含曆日（[LocalDate]），不含時間／時區——記帳本不需要時分秒。
 */
data class Transaction(
    val id: String,
    val walletId: String,
    val type: TransactionType,
    val amount: Long,
    val currency: String,
    val categoryId: String?,
    val date: LocalDate,
    val note: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(amount > 0) { "Transaction amount must be > 0, was $amount" }
    }
}
