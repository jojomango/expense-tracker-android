package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T3 — Budget. Domain-only, no mocks, referenceDate 一律注入
 * （見 CLAUDE.md 禁令 2）。
 */
class BudgetTest {
    private val fixedInstant = Instant.fromEpochMilliseconds(0)
    private var seq = 0

    private fun tx(
        walletId: String,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        categoryId: String? = null,
    ) = Transaction(
        id = "tx${seq++}",
        walletId = walletId,
        type = type,
        amount = amount,
        categoryId = categoryId,
        date = date,
        createdAt = fixedInstant,
        updatedAt = fixedInstant,
    )

    // ---- T3.1 週預算（budgetMode = WEEKLY） ----
    // 情境：日常錢包，TWD，週預算 3000.00（300000L），weekStart=MONDAY，今天 2026-08-11（二）
    // 週區間（MONDAY 起）：2026-08-10 ~ 2026-08-16

    private val dailyWallet = Wallet("daily", "日常", "TWD", BudgetMode.WEEKLY, 300000L)
    private val referenceDate = LocalDate(2026, 8, 11)
    private val weekStart = DayOfWeek.MONDAY

    private fun weeklyBalance(transactions: List<Transaction>) =
        Budget.calculateWeeklyBalance(dailyWallet, transactions, weekStart, referenceDate)!!

    @Test
    @DisplayName("T3.1.1 — 無交易 -> 300000（NT\$3,000.00）")
    fun `T3 1 1`() {
        assertEquals(300000L, weeklyBalance(emptyList()).balance.amount)
    }

    @Test
    @DisplayName("T3.1.2 — 本週支出 500 + 300 -> 300000 - 80000 = 220000")
    fun `T3 1 2`() {
        val txs =
            listOf(
                tx("daily", TransactionType.EXPENSE, 50000, LocalDate(2026, 8, 12)),
                tx("daily", TransactionType.EXPENSE, 30000, LocalDate(2026, 8, 13)),
            )
        assertEquals(220000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.3 — 本週支出 3500 -> -50000（超支），isOverBudget = true")
    fun `T3 1 3`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 350000, LocalDate(2026, 8, 12)))
        val status = weeklyBalance(txs)
        assertEquals(-50000L, status.balance.amount)
        assertTrue(status.isOverBudget)
    }

    @Test
    @DisplayName("T3.1.4 — 本週支出 3000（剛好用完）-> 0，isOverBudget = false（邊界）")
    fun `T3 1 4`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 300000, LocalDate(2026, 8, 12)))
        val status = weeklyBalance(txs)
        assertEquals(0L, status.balance.amount)
        assertFalse(status.isOverBudget)
    }

    @Test
    @DisplayName("T3.1.5 — 本週有一筆收入 5000，餘額不受影響（D1：只計支出）")
    fun `T3 1 5`() {
        val txs = listOf(tx("daily", TransactionType.INCOME, 500000, LocalDate(2026, 8, 12)))
        assertEquals(300000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.6 — 上週支出 2000，本週 0 -> 本週餘額 300000（不結轉、不累計）")
    fun `T3 1 6`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 200000, LocalDate(2026, 8, 3)))
        assertEquals(300000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.7 — 交易日期為本週週首當天，計入")
    fun `T3 1 7`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 10)))
        assertEquals(290000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.8 — 交易日期為本週週尾當天，計入")
    fun `T3 1 8`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 16)))
        assertEquals(290000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.9 — 交易日期為週首前一天，不計入")
    fun `T3 1 9`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 9)))
        assertEquals(300000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.10 — 交易日期為週尾後一天，不計入")
    fun `T3 1 10`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 17)))
        assertEquals(300000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.11 — 未來日期（本週內）的交易，計入（預先記帳）")
    fun `T3 1 11`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 15)))
        assertEquals(290000L, weeklyBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.1.12 — 其他錢包的交易，不計入（錢包隔離）")
    fun `T3 1 12`() {
        val txs = listOf(tx("travel", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12)))
        assertEquals(300000L, weeklyBalance(txs).balance.amount)
    }

    // ---- T3.2 總預算（budgetMode = TOTAL） ----
    // 情境：日本旅遊錢包，JPY，總預算 200000

    private val travelWallet = Wallet("travel", "日本旅遊", "JPY", BudgetMode.TOTAL, 200000L)

    private fun totalBalance(transactions: List<Transaction>): TotalBudgetStatus =
        Budget.calculateTotalBalance(travelWallet, transactions)!!

    @Test
    @DisplayName("T3.2.1 — 無交易 -> 餘額 200000，已用 0%")
    fun `T3 2 1`() {
        val status = totalBalance(emptyList())
        assertEquals(200000L, status.balance.amount)
        assertEquals(0.0, status.usedPercent)
    }

    @Test
    @DisplayName("T3.2.2 — 支出 50000 -> 餘額 150000，已用 25%")
    fun `T3 2 2`() {
        val txs = listOf(tx("travel", TransactionType.EXPENSE, 50000, LocalDate(2026, 8, 12)))
        val status = totalBalance(txs)
        assertEquals(150000L, status.balance.amount)
        assertEquals(25.0, status.usedPercent)
    }

    @Test
    @DisplayName("T3.2.3 — 支出跨越多週（3 週共 180000）-> 餘額 20000，週界完全無關")
    fun `T3 2 3`() {
        val txs =
            listOf(
                tx("travel", TransactionType.EXPENSE, 60000, LocalDate(2026, 8, 1)),
                tx("travel", TransactionType.EXPENSE, 60000, LocalDate(2026, 8, 10)),
                tx("travel", TransactionType.EXPENSE, 60000, LocalDate(2026, 8, 20)),
            )
        assertEquals(20000L, totalBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.2.4 — 支出 250000 -> 餘額 -50000，已用 125%，isOverBudget = true")
    fun `T3 2 4`() {
        val txs = listOf(tx("travel", TransactionType.EXPENSE, 250000, LocalDate(2026, 8, 12)))
        val status = totalBalance(txs)
        assertEquals(-50000L, status.balance.amount)
        assertEquals(125.0, status.usedPercent)
        assertTrue(status.isOverBudget)
    }

    @Test
    @DisplayName("T3.2.5 — 有一筆收入 30000，餘額不受影響（D1）")
    fun `T3 2 5`() {
        val txs = listOf(tx("travel", TransactionType.INCOME, 30000, LocalDate(2026, 8, 12)))
        assertEquals(200000L, totalBalance(txs).balance.amount)
    }

    @Test
    @DisplayName("T3.2.6 — 總預算為 0，已用百分比回傳 0.0，不得 NaN")
    fun `T3 2 6`() {
        val zeroBudgetWallet = Wallet("zero", "零預算", "JPY", BudgetMode.TOTAL, 0L)
        val status = Budget.calculateTotalBalance(zeroBudgetWallet, emptyList())!!
        assertEquals(0.0, status.usedPercent)
        assertFalse(status.usedPercent.isNaN())
    }

    // ---- T3.3 無預算（budgetMode = NONE） ----

    private val noBudgetWallet = Wallet("none", "無預算", "TWD", BudgetMode.NONE, null)

    @Test
    @DisplayName("T3.3.1 — 呼叫餘額計算，回傳 null")
    fun `T3 3 1`() {
        assertNull(Budget.calculateWeeklyBalance(noBudgetWallet, emptyList(), weekStart, referenceDate))
        assertNull(Budget.calculateTotalBalance(noBudgetWallet, emptyList()))
    }

    @Test
    @DisplayName("T3.3.2 — 呼叫本週支出總額，仍正常回傳數值")
    fun `T3 3 2`() {
        val txs = listOf(tx("none", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12)))
        val total = Budget.calculateWeeklyExpenseTotal(noBudgetWallet, txs, weekStart, referenceDate)
        assertEquals(10000L, total.amount)
    }

    // ---- T3.4 多錢包隔離 ----

    @Test
    @DisplayName("T3.4.1/2 — 兩錢包餘額計算各自獨立，回傳各自幣別")
    fun `T3 4 1 and 2`() {
        val txs =
            listOf(
                tx("daily", TransactionType.EXPENSE, 50000, LocalDate(2026, 8, 12)),
                tx("travel", TransactionType.EXPENSE, 80000, LocalDate(2026, 8, 12)),
            )
        val dailyStatus = weeklyBalance(txs)
        val travelStatus = totalBalance(txs)
        assertEquals("TWD", dailyStatus.balance.currency.code)
        assertEquals("JPY", travelStatus.balance.currency.code)
        assertEquals(250000L, dailyStatus.balance.amount)
        assertEquals(120000L, travelStatus.balance.amount)
    }

    @Test
    @DisplayName("T3.4.4 — 修改 JPY 交易後 TWD 餘額不變")
    fun `T3 4 4`() {
        val before = weeklyBalance(listOf(tx("travel", TransactionType.EXPENSE, 999999, LocalDate(2026, 8, 12))))
        val after =
            weeklyBalance(
                listOf(
                    tx("travel", TransactionType.EXPENSE, 999999, LocalDate(2026, 8, 12)),
                    tx("travel", TransactionType.EXPENSE, 1, LocalDate(2026, 8, 12)),
                ),
            )
        assertEquals(before.balance, after.balance)
    }

    // ---- T3.5 分類彙總 ----

    @Test
    @DisplayName("T3.5.1 — 5 筆分屬 3 分類的支出，3 組，依金額倒序，含各組佔比")
    fun `T3 5 1`() {
        val food = Category("food", "飲食", CategoryType.EXPENSE, "🍜", "#C1502E")
        val transport = Category("transport", "交通", CategoryType.EXPENSE, "🚗", "#3F8F6A")
        val shopping = Category("shopping", "購物", CategoryType.EXPENSE, "🛒", "#2F6F9F")
        val categories = listOf(food, transport, shopping)

        val txs =
            listOf(
                tx("daily", TransactionType.EXPENSE, 30000, LocalDate(2026, 8, 12), food.id),
                tx("daily", TransactionType.EXPENSE, 20000, LocalDate(2026, 8, 12), food.id),
                tx("daily", TransactionType.EXPENSE, 30000, LocalDate(2026, 8, 12), transport.id),
                tx("daily", TransactionType.EXPENSE, 15000, LocalDate(2026, 8, 12), shopping.id),
                tx("daily", TransactionType.EXPENSE, 5000, LocalDate(2026, 8, 12), shopping.id),
            )
        val result = Budget.summarizeByCategory(txs, categories, TransactionType.EXPENSE, "TWD")
        assertEquals(3, result.size)
        // 依金額倒序：飲食 50000 > 交通 30000 > 購物 20000
        assertEquals(listOf(food.id, transport.id, shopping.id), result.map { it.categoryId })
        assertEquals(50000L, result[0].total.amount)
        assertEquals(50.0, result[0].percent, 0.01)
        assertEquals(30.0, result[1].percent, 0.01)
        assertEquals(20.0, result[2].percent, 0.01)
    }

    @Test
    @DisplayName("T3.5.2 — 混合收入與支出，支出與收入分開彙總，不混算")
    fun `T3 5 2`() {
        val food = Category("food", "飲食", CategoryType.EXPENSE, "🍜", "#C1502E")
        val salary = Category("salary", "薪資", CategoryType.INCOME, "💰", "#2F8F63")
        val txs =
            listOf(
                tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12), food.id),
                tx("daily", TransactionType.INCOME, 500000, LocalDate(2026, 8, 12), salary.id),
            )
        val expenseSummary = Budget.summarizeByCategory(txs, listOf(food, salary), TransactionType.EXPENSE, "TWD")
        val incomeSummary = Budget.summarizeByCategory(txs, listOf(food, salary), TransactionType.INCOME, "TWD")
        assertEquals(1, expenseSummary.size)
        assertEquals(100.0, expenseSummary[0].percent)
        assertEquals(1, incomeSummary.size)
        assertEquals(100.0, incomeSummary[0].percent)
    }

    @Test
    @DisplayName("T3.5.3 — 有交易的分類被刪除（categories 清單裡找不到），該筆歸入未分類，交易不遺失")
    fun `T3 5 3`() {
        val txs = listOf(tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12), "deleted-category-id"))
        val result = Budget.summarizeByCategory(txs, emptyList(), TransactionType.EXPENSE, "TWD")
        assertEquals(1, result.size)
        assertNull(result[0].categoryId)
        assertEquals("未分類", result[0].name)
        assertEquals(10000L, result[0].total.amount)
    }

    @Test
    @DisplayName("T3.5.4 — 空清單，空 list，佔比計算不 crash")
    fun `T3 5 4`() {
        val result = Budget.summarizeByCategory(emptyList(), emptyList(), TransactionType.EXPENSE, "TWD")
        assertEquals(emptyList<CategorySummary>(), result)
    }

    // ---- T3.6 近 N 週支出趨勢 ----
    // 情境同 T3.1（日常錢包，TWD，週預算 3000，weekStart=MONDAY，今天 2026-08-11）

    @Test
    @DisplayName("T3.6.1 — weeksCount=8，回傳 8 筆，依週首由舊到新排序，最後一筆即為本週")
    fun `T3 6 1`() {
        val trend = Budget.summarizeWeeklyTrend(dailyWallet, emptyList(), weekStart, referenceDate, weeksCount = 8)
        assertEquals(8, trend.size)
        assertEquals(LocalDate(2026, 8, 10), trend.last().range.start)
        for (i in 0 until trend.size - 1) {
            assertTrue(trend[i].range.start < trend[i + 1].range.start)
        }
    }

    @Test
    @DisplayName("T3.6.2 — 本週支出 10000、上週支出 20000，各自只計入落在該週區間內的支出")
    fun `T3 6 2`() {
        val txs =
            listOf(
                tx("daily", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12)),
                tx("daily", TransactionType.EXPENSE, 20000, LocalDate(2026, 8, 4)),
            )
        val trend = Budget.summarizeWeeklyTrend(dailyWallet, txs, weekStart, referenceDate, weeksCount = 2)
        assertEquals(20000L, trend[0].total.amount)
        assertEquals(10000L, trend[1].total.amount)
    }

    @Test
    @DisplayName("T3.6.3 — 本週有一筆收入 50000，該週總額不受影響")
    fun `T3 6 3`() {
        val txs = listOf(tx("daily", TransactionType.INCOME, 50000, LocalDate(2026, 8, 12)))
        val trend = Budget.summarizeWeeklyTrend(dailyWallet, txs, weekStart, referenceDate, weeksCount = 1)
        assertEquals(0L, trend[0].total.amount)
    }

    @Test
    @DisplayName("T3.6.4 — 其他錢包的交易，不計入")
    fun `T3 6 4`() {
        val txs = listOf(tx("travel", TransactionType.EXPENSE, 50000, LocalDate(2026, 8, 12)))
        val trend = Budget.summarizeWeeklyTrend(dailyWallet, txs, weekStart, referenceDate, weeksCount = 1)
        assertEquals(0L, trend[0].total.amount)
    }

    @Test
    @DisplayName("T3.6.5 — budgetMode = NONE 的錢包，仍可正常計算")
    fun `T3 6 5`() {
        val txs = listOf(tx("none", TransactionType.EXPENSE, 10000, LocalDate(2026, 8, 12)))
        val trend = Budget.summarizeWeeklyTrend(noBudgetWallet, txs, weekStart, referenceDate, weeksCount = 1)
        assertEquals(10000L, trend[0].total.amount)
    }

    @Test
    @DisplayName("T3.6.6 — weeksCount = 0，回傳空 list")
    fun `T3 6 6`() {
        assertEquals(
            emptyList<WeeklyTrendPoint>(),
            Budget.summarizeWeeklyTrend(dailyWallet, emptyList(), weekStart, referenceDate, weeksCount = 0),
        )
    }

    // ---- T3.7 每週剩餘天數（daysLeftInWeek） ----

    @Test
    @DisplayName("T3.7.1 — MONDAY, 2026-09-03（週四）-> 4")
    fun `T3 7 1`() {
        assertEquals(4, Budget.daysLeftInWeek(DayOfWeek.MONDAY, LocalDate(2026, 9, 3)))
    }

    @Test
    @DisplayName("T3.7.2 — MONDAY, 2026-09-06（週日，最後一天）-> 1")
    fun `T3 7 2`() {
        assertEquals(1, Budget.daysLeftInWeek(DayOfWeek.MONDAY, LocalDate(2026, 9, 6)))
    }

    @Test
    @DisplayName("T3.7.3 — MONDAY, 2026-08-31（週一，第一天）-> 7")
    fun `T3 7 3`() {
        assertEquals(7, Budget.daysLeftInWeek(DayOfWeek.MONDAY, LocalDate(2026, 8, 31)))
    }

    @Test
    @DisplayName("T3.7.4 — SUNDAY, 2026-09-06（週日）-> 7")
    fun `T3 7 4`() {
        assertEquals(7, Budget.daysLeftInWeek(DayOfWeek.SUNDAY, LocalDate(2026, 9, 6)))
    }

    @Test
    @DisplayName("T3.7.5 — SUNDAY, 2026-09-05（週六）-> 1")
    fun `T3 7 5`() {
        assertEquals(1, Budget.daysLeftInWeek(DayOfWeek.SUNDAY, LocalDate(2026, 9, 5)))
    }

    @Test
    @DisplayName("T3.7.6 — WEDNESDAY, 2026-09-03（週四）-> 6")
    fun `T3 7 6`() {
        assertEquals(6, Budget.daysLeftInWeek(DayOfWeek.WEDNESDAY, LocalDate(2026, 9, 3)))
    }

    // ---- T3.8 日均可用額（dailyAllowance） ----

    @Test
    @DisplayName("T3.8.1 — remaining=1146, daysLeft=4 -> 286（向下取整）")
    fun `T3 8 1`() {
        assertEquals(286L, Budget.dailyAllowance(1146, 4))
    }

    @Test
    @DisplayName("T3.8.2 — remaining=0, daysLeft=4 -> 0")
    fun `T3 8 2`() {
        assertEquals(0L, Budget.dailyAllowance(0, 4))
    }

    @Test
    @DisplayName("T3.8.3 — remaining=-500（已超支）, daysLeft=4 -> 0")
    fun `T3 8 3`() {
        assertEquals(0L, Budget.dailyAllowance(-500, 4))
    }

    @Test
    @DisplayName("T3.8.4 — remaining=1000, daysLeft=1 -> 1000")
    fun `T3 8 4`() {
        assertEquals(1000L, Budget.dailyAllowance(1000, 1))
    }

    @Test
    @DisplayName("T3.8.5 — remaining=1000（JPY，0 位小數）, daysLeft=3 -> 333")
    fun `T3 8 5`() {
        assertEquals(333L, Budget.dailyAllowance(1000, 3))
    }

    @Test
    @DisplayName("T3.8.6 — daysLeft=0，拋 IllegalArgumentException")
    fun `T3 8 6`() {
        assertThrows(IllegalArgumentException::class.java) { Budget.dailyAllowance(1000, 0) }
    }
}
