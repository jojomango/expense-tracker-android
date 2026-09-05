package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 週/總預算的計算結果：餘額 + 是否已超支（餘額 < 0）。 */
data class BudgetStatus(
    val balance: Money,
    val isOverBudget: Boolean,
)

/** 總預算模式額外要顯示「已用百分比」——SPEC.md §3.4。 */
data class TotalBudgetStatus(
    val balance: Money,
    val usedPercent: Double,
    val isOverBudget: Boolean,
)

/** [summarizeByCategory] 的一組結果。刻意不含顏色資訊——顏色一律用 [colorOf] 現查，
 * 不由彙總結果決定（見 TESTCASES.md T6.3.2）。 */
data class CategorySummary(
    val categoryId: String?,
    val name: String,
    val total: Money,
    val percent: Double,
)

data class WeeklyTrendPoint(
    val range: DateRange,
    val total: Money,
)

/**
 * 預算與餘額計算——SPEC.md §3.4，全部是純函式，時間一律由 [referenceDate] 注入
 * （見 CLAUDE.md 禁令 2，不得呼叫 `Clock.System.now()`）。
 */
object Budget {
    /**
     * 本週支出總額，跟 [Wallet.budgetMode] 無關（budgetMode = none 的錢包也能正常
     * 呼叫，見 TESTCASES.md T3.3.2）。只計 [TransactionType.EXPENSE]（D1：不扣收入）。
     */
    fun calculateWeeklyExpenseTotal(
        wallet: Wallet,
        transactions: List<Transaction>,
        weekStartDay: DayOfWeek,
        referenceDate: LocalDate,
    ): Money {
        val range = Week.rangeOf(referenceDate, weekStartDay)
        return sumExpenses(wallet, transactions) { it.date in range }
    }

    /**
     * 週預算餘額 = 週預算 − 本週支出總和。`wallet.budgetMode != WEEKLY` 時回傳
     * `null`（TESTCASES.md T3.3.1）。
     */
    fun calculateWeeklyBalance(
        wallet: Wallet,
        transactions: List<Transaction>,
        weekStartDay: DayOfWeek,
        referenceDate: LocalDate,
    ): BudgetStatus? {
        if (wallet.budgetMode != BudgetMode.WEEKLY) return null
        val budget = Money.of(requireNotNull(wallet.budgetAmount), wallet.currency)
        val spent = calculateWeeklyExpenseTotal(wallet, transactions, weekStartDay, referenceDate)
        val balance = budget.minus(spent)
        return BudgetStatus(balance, isOverBudget = balance.amount < 0)
    }

    /**
     * 總預算餘額 = 總預算 − 該錢包全部支出總和，不受週期影響。`wallet.budgetMode
     * != TOTAL` 時回傳 `null`（TESTCASES.md T3.3.1）。
     */
    fun calculateTotalBalance(
        wallet: Wallet,
        transactions: List<Transaction>,
    ): TotalBudgetStatus? {
        if (wallet.budgetMode != BudgetMode.TOTAL) return null
        val budget = Money.of(requireNotNull(wallet.budgetAmount), wallet.currency)
        val spent = sumExpenses(wallet, transactions) { true }
        val balance = budget.minus(spent)
        return TotalBudgetStatus(
            balance = balance,
            usedPercent = spent.percentOf(budget),
            isOverBudget = balance.amount < 0,
        )
    }

    /**
     * 依分類彙總（支出／收入分開彙總，不混算——TESTCASES.md T3.5.2）。已刪除或
     * 找不到的 categoryId 會併入「未分類」，交易不遺失（T3.5.3）。依金額倒序。
     */
    fun summarizeByCategory(
        transactions: List<Transaction>,
        categories: List<Category>,
        type: TransactionType,
        currencyCode: String,
        registry: CurrencyRegistry = CurrencyRegistry(),
    ): List<CategorySummary> {
        val filtered = transactions.filter { it.type == type }
        if (filtered.isEmpty()) return emptyList()

        val categoryById = categories.associateBy { it.id }

        fun resolvedCategoryId(tx: Transaction) = tx.categoryId?.takeIf { categoryById.containsKey(it) }

        val grandTotal = Money.sum(filtered.map { Money.of(it.amount, currencyCode, registry) }, currencyCode, registry)

        return filtered
            .groupBy(::resolvedCategoryId)
            .map { (categoryId, txs) ->
                val total = Money.sum(txs.map { Money.of(it.amount, currencyCode, registry) }, currencyCode, registry)
                CategorySummary(
                    categoryId = categoryId,
                    name = categoryId?.let { categoryById.getValue(it).name } ?: "未分類",
                    total = total,
                    percent = total.percentOf(grandTotal),
                )
            }.sortedByDescending { it.total.amount }
    }

    /**
     * 近 [weeksCount] 週支出趨勢，依週首由舊到新排序，最後一筆即為本週
     * （TESTCASES.md T3.6）。`weeksCount <= 0` 回傳空 list。
     */
    fun summarizeWeeklyTrend(
        wallet: Wallet,
        transactions: List<Transaction>,
        weekStartDay: DayOfWeek,
        referenceDate: LocalDate,
        weeksCount: Int,
    ): List<WeeklyTrendPoint> {
        if (weeksCount <= 0) return emptyList()
        val currentWeekStart = Week.rangeOf(referenceDate, weekStartDay).start
        return (weeksCount - 1 downTo 0).map { weeksAgo ->
            val start = currentWeekStart.minus(weeksAgo * 7, DateTimeUnit.DAY)
            val range = DateRange(start, start.plus(6, DateTimeUnit.DAY))
            val total = sumExpenses(wallet, transactions) { it.date in range }
            WeeklyTrendPoint(range, total)
        }
    }

    /**
     * 本週剩餘天數，含今天（TESTCASES.md T3.7）。時間由 [referenceDate] 注入。
     */
    fun daysLeftInWeek(
        weekStartDay: DayOfWeek,
        referenceDate: LocalDate,
    ): Int {
        val range = Week.rangeOf(referenceDate, weekStartDay)
        return referenceDate.daysUntil(range.end) + 1
    }

    /**
     * 日均可用額 = 剩餘 / 剩餘天數，向下取整（TESTCASES.md T3.8）。
     * [remaining] 已超支（<= 0）時回傳 0；[daysLeft] 必須 > 0。
     */
    fun dailyAllowance(
        remaining: Long,
        daysLeft: Int,
    ): Long {
        require(daysLeft > 0) { "daysLeft must be > 0, was $daysLeft" }
        if (remaining <= 0) return 0
        return remaining / daysLeft
    }

    private fun sumExpenses(
        wallet: Wallet,
        transactions: List<Transaction>,
        predicate: (Transaction) -> Boolean,
    ): Money {
        val matching =
            transactions.filter {
                it.walletId == wallet.id && it.type == TransactionType.EXPENSE && predicate(it)
            }
        return Money.sum(matching.map { Money.of(it.amount, wallet.currency) }, wallet.currency)
    }
}
