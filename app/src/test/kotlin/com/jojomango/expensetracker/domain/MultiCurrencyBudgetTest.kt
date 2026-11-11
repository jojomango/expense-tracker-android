package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T3.9 — 一個錢包多幣別（SPEC.md §3.4 v2.12）。
 * 情境：錢包「宿霧」預設 PHP、週預算 ₱10,000.00；今天 2026-11-11（三），週一起算，本週 11/9～11/15。
 */
class MultiCurrencyBudgetTest {
    private val fixedInstant = Instant.fromEpochMilliseconds(0)
    private var seq = 0
    private val weekStart = DayOfWeek.MONDAY
    private val today = LocalDate(2026, 11, 11)
    private val lastWeek = LocalDate(2026, 11, 4)

    private val cebu = Wallet("cebu", "宿霧", "PHP", BudgetMode.WEEKLY, 1_000_000L)

    private fun tx(
        amount: Long,
        currency: String,
        type: TransactionType = TransactionType.EXPENSE,
        date: LocalDate = today,
    ) = Transaction(
        id = "tx${seq++}",
        walletId = cebu.id,
        type = type,
        amount = amount,
        currency = currency,
        categoryId = null,
        date = date,
        createdAt = fixedInstant,
        updatedAt = fixedInstant,
    )

    private val transactions =
        listOf(
            tx(120_000, "PHP"),
            tx(30_000, "PHP"),
            tx(4_000, "USD"),
            tx(500, "USD"),
            tx(1_000, "USD", type = TransactionType.INCOME),
            tx(10_000, "USD", date = lastWeek),
        )

    @Test
    @DisplayName("T3.9.1 — 本週餘額只算預設幣別（PHP）的支出：₱8,500.00")
    fun `T3 9 1`() {
        val status = Budget.calculateWeeklyBalance(cebu, transactions, weekStart, today)!!
        assertEquals(Money.of(850_000, "PHP"), status.balance)
    }

    @Test
    @DisplayName("T3.9.2 — 本週「另有」只有 US\$45.00（不含收入與上週）")
    fun `T3 9 2`() {
        assertEquals(listOf(Money.of(4_500, "USD")), MultiCurrency.otherCurrencyExpenses(cebu, transactions, weekStart, today))
    }

    @Test
    @DisplayName("T3.9.3 — 總預算模式的「另有」算全部期間：US\$145.00")
    fun `T3 9 3`() {
        val total = cebu.copy(budgetMode = BudgetMode.TOTAL, budgetAmount = 2_000_000L)
        assertEquals(listOf(Money.of(14_500, "USD")), MultiCurrency.otherCurrencyExpenses(total, transactions, weekStart, today))
    }

    @Test
    @DisplayName("T3.9.4 — 只有 PHP 交易時「另有」是空清單")
    fun `T3 9 4`() {
        val phpOnly = transactions.filter { it.currency == "PHP" }
        assertEquals(emptyList<Money>(), MultiCurrency.otherCurrencyExpenses(cebu, phpOnly, weekStart, today))
    }

    @Test
    @DisplayName("T3.9.5 — 本週小計依幣別各算淨額：PHP -150000、USD -3500，預設幣別在前")
    fun `T3 9 5`() {
        val thisWeek = transactions.filter { it.date != lastWeek }
        assertEquals(
            listOf(Money.of(-150_000, "PHP"), Money.of(-3_500, "USD")),
            MultiCurrency.netTotalsByCurrency(thisWeek, cebu.currency),
        )
    }

    @Test
    @DisplayName("T3.9.6 — 其他幣別有 USD 與 JPY 時：PHP、USD、JPY")
    fun `T3 9 6`() {
        // 刻意先放 JPY 再放 USD：順序要靠「預設幣別在前，其餘依內建順序」，不是交易出現的順序
        val mixed = listOf(tx(100, "JPY"), tx(4_000, "USD"), tx(120_000, "PHP"))
        assertEquals(listOf("PHP", "JPY", "USD"), MultiCurrency.netTotalsByCurrency(mixed, "PHP").map { it.currency.code })
    }

    @Test
    @DisplayName("T3.9.7 — 預設幣別改為 USD、週預算 US\$500.00：餘額 US\$455.00，PHP 改列「另有」₱1,500.00")
    fun `T3 9 7`() {
        val usd = cebu.copy(currency = "USD", budgetAmount = 50_000L)
        assertEquals(Money.of(45_500, "USD"), Budget.calculateWeeklyBalance(usd, transactions, weekStart, today)!!.balance)
        assertEquals(listOf(Money.of(150_000, "PHP")), MultiCurrency.otherCurrencyExpenses(usd, transactions, weekStart, today))
    }

    @Test
    @DisplayName("T3.9.8 — 統計選取 USD 的本週分類彙總只含 USD 支出：US\$45.00")
    fun `T3 9 8`() {
        val range = Week.rangeOf(today, weekStart)
        val summary =
            Budget.summarizeByCategory(
                transactions.filter { it.date in range },
                emptyList(),
                TransactionType.EXPENSE,
                "USD",
                ZhTestCategoryNames,
            )
        assertEquals(listOf(Money.of(4_500, "USD")), summary.map { it.total })
    }

    @Test
    @DisplayName("T3.9.9 — PHP 與 USD 金額不能相加（跨幣別照 T1.3.3 拋錯）")
    fun `T3 9 9`() {
        assertThrows(IllegalArgumentException::class.java) { Money.of(100, "PHP").plus(Money.of(100, "USD")) }
    }

    @Test
    @DisplayName("T3.9.10 — 統計預設選取：預設幣別 PHP 沒有交易、只有 USD 與 JPY 時選 JPY")
    fun `T3 9 10`() {
        val noPhp = listOf(tx(4_000, "USD"), tx(500, "JPY"))
        assertEquals("JPY", MultiCurrency.statsCurrency(noPhp, "PHP"))
    }

    @Test
    @DisplayName("T3.9.10 補充 — 預設幣別有交易時選預設幣別；完全沒有交易時也是預設幣別")
    fun `T3 9 10 default`() {
        assertEquals("PHP", MultiCurrency.statsCurrency(transactions, "PHP"))
        assertEquals("PHP", MultiCurrency.statsCurrency(emptyList(), "PHP"))
    }

    @Test
    @DisplayName("UI-SPEC §15.4 — 統計 chip：用到的幣別，預設幣別在前、其餘依內建順序")
    fun `stats chips order`() {
        val mixed = transactions + tx(300, "JPY")
        assertEquals(listOf("PHP", "JPY", "USD"), MultiCurrency.currenciesUsed(mixed, "PHP"))
    }

    @Test
    @DisplayName("T8.5.2 — 幣別 sheet 順序：預設幣別 → 用過的其他幣別 → 其餘內建幣別，不重複")
    fun `picker order`() {
        val order = MultiCurrency.pickerOrder("PHP", transactions + tx(300, "JPY"))
        assertEquals(listOf("PHP", "JPY", "USD"), order.take(3))
        assertEquals(Currencies.builtIn.keys.toSet(), order.toSet())
        assertEquals(order.size, order.distinct().size)
    }
}
