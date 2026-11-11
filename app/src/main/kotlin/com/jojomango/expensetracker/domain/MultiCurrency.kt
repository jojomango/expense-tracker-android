package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * 一個錢包多幣別（SPEC.md §3.4 v2.12、D19）。不同幣別永遠不相加、不換算，
 * 所以這裡的結果一律是「每種幣別一個 [Money]」的清單。
 */
object MultiCurrency {
    /**
     * 每種有交易的幣別各算一個淨額（UI-SPEC.md §15.3，T3.9.5、T3.9.6）。不同幣別不相加、不換算；
     * [defaultCurrency] 在前，其餘依內建順序。
     */
    fun netTotalsByCurrency(
        transactions: List<Transaction>,
        defaultCurrency: String,
    ): List<Money> = orderCurrencies(transactions.map { it.currency }, defaultCurrency).map { Budget.netTotal(transactions, it) }

    /**
     * 預算卡的「另有」：預設幣別以外的各幣別支出總和（SPEC.md §3.4 v2.12，T3.9.2～T3.9.4、T3.9.7）。
     * 期間跟預算一致：`total` 是全部，`weekly` 與 `none` 是本週。沒有其他幣別的支出就是空清單。
     */
    fun otherCurrencyExpenses(
        wallet: Wallet,
        transactions: List<Transaction>,
        weekStartDay: DayOfWeek,
        referenceDate: LocalDate,
    ): List<Money> {
        val range = Week.rangeOf(referenceDate, weekStartDay)
        val others =
            transactions.filter {
                it.walletId == wallet.id &&
                    it.type == TransactionType.EXPENSE &&
                    it.currency != wallet.currency &&
                    (wallet.budgetMode == BudgetMode.TOTAL || it.date in range)
            }
        return orderCurrencies(others.map { it.currency }, wallet.currency).map { code ->
            Money.sum(others.filter { it.currency == code }.map { Money.of(it.amount, code) }, code)
        }
    }

    /** 預設幣別在前，其餘依 [Currencies.builtIn] 的順序；不在內建清單的排最後（UI-SPEC.md §15）。 */
    private fun orderCurrencies(
        codes: List<String>,
        defaultCurrency: String,
    ): List<String> {
        val builtInOrder = Currencies.builtIn.keys.toList()
        return codes.distinct().sortedWith(
            compareBy<String> { it != defaultCurrency }.thenBy { builtInOrder.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } },
        )
    }
}
