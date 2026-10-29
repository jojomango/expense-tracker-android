package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T2.6 — 週內依日分段與年份分隔（UI-SPEC §4.3 v2.7，UX-006）。 */
class DaySectionsTest {
    private var n = 0

    private fun tx(
        date: LocalDate,
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 100,
    ) = Transaction(
        id = "t${n++}",
        walletId = "w",
        type = type,
        amount = amount,
        categoryId = null,
        date = date,
        note = null,
        createdAt = Instant.fromEpochMilliseconds(n.toLong()),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun entries(
        items: List<Transaction>,
        referenceDate: LocalDate,
    ): List<List<WeekEntry<Transaction>>> {
        val weeks =
            Week.groupByWeek(items, DayOfWeek.MONDAY, compareByDescending { it.createdAt }) { it.date }
        return DaySections.entriesOf(weeks, referenceDate) { it.date }
    }

    private fun shape(week: List<WeekEntry<Transaction>>): List<String> =
        week.map {
            when (it) {
                is WeekEntry.Year -> "Y${it.year}"
                is WeekEntry.Day -> "${it.date.monthNumber}/${it.date.dayOfMonth}x${it.items.size}"
            }
        }

    private val oct28 = LocalDate(2026, 10, 28)

    @Test
    @DisplayName("T2.6.1 — 同週 10/28 兩筆、10/26 一筆：分 2 段，由新到舊，段內依建立時間由新到舊")
    fun `T2 6 1`() {
        val a = tx(oct28)
        val b = tx(oct28)
        val c = tx(LocalDate(2026, 10, 26))
        val weeks = entries(listOf(c, a, b), oct28)
        assertEquals(listOf(listOf("10/28x2", "10/26x1")), weeks.map(::shape))
        assertEquals(listOf(b, a), (weeks[0][0] as WeekEntry.Day).items)
    }

    @Test
    @DisplayName("T2.6.2 — 10/27 沒有交易：不產生空白段")
    fun `T2 6 2`() {
        val weeks = entries(listOf(tx(oct28), tx(LocalDate(2026, 10, 26))), oct28)
        assertEquals(listOf("10/28x1", "10/26x1"), shape(weeks.single()))
    }

    @Test
    @DisplayName("T2.6.3 — 同一天支出 300、收入 1000：日小計 +700")
    fun `T2 6 3`() {
        val day =
            entries(listOf(tx(oct28, amount = 30000), tx(oct28, TransactionType.INCOME, 100000)), oct28)
                .single()
                .single() as WeekEntry.Day
        assertEquals("+NT$700.00", Budget.netTotal(day.items, "TWD").formatSigned())
    }

    @Test
    @DisplayName("T2.6.4 — 同一天支出 500、收入 500：日小計 0，不帶正負號")
    fun `T2 6 4`() {
        val day =
            entries(listOf(tx(oct28, amount = 50000), tx(oct28, TransactionType.INCOME, 50000)), oct28)
                .single()
                .single() as WeekEntry.Day
        assertEquals("NT$0.00", Budget.netTotal(day.items, "TWD").formatSigned())
    }

    @Test
    @DisplayName("T2.6.5 — 一週的日小計加總等於週小計")
    fun `T2 6 5`() {
        val items =
            listOf(
                tx(oct28, amount = 12000),
                tx(LocalDate(2026, 10, 27), TransactionType.INCOME, 50000),
                tx(LocalDate(2026, 10, 26), amount = 33000),
            )
        val week = entries(items, oct28).single()
        val sumOfDays = week.filterIsInstance<WeekEntry.Day<Transaction>>().sumOf { Budget.netTotal(it.items, "TWD").amount }
        assertEquals(Budget.netTotal(items, "TWD").amount, sumOfDays)
    }

    @Test
    @DisplayName("T2.6.6 — 跨年週：1/3 段 → 年份分隔 2025 → 12/30 段，不拆週")
    fun `T2 6 6`() {
        val weeks = entries(listOf(tx(LocalDate(2026, 1, 3)), tx(LocalDate(2025, 12, 30))), LocalDate(2026, 1, 3))
        assertEquals(listOf(listOf("1/3x1", "Y2025", "12/30x1")), weeks.map(::shape))
    }

    @Test
    @DisplayName("T2.6.7 — 所有交易都在去年：列表最上方先出現年份分隔")
    fun `T2 6 7`() {
        val weeks = entries(listOf(tx(LocalDate(2025, 12, 10))), LocalDate(2026, 3, 1))
        assertEquals(listOf("Y2025", "12/10x1"), shape(weeks.single()))
    }

    @Test
    @DisplayName("T2.6.8 — 全部在同一年：不出現年份分隔")
    fun `T2 6 8`() {
        val weeks = entries(listOf(tx(oct28), tx(LocalDate(2026, 3, 2)), tx(LocalDate(2026, 1, 5))), oct28)
        assertEquals(0, weeks.flatten().count { it is WeekEntry.Year })
    }

    @Test
    @DisplayName("T2.6.9 — 跨 2024、2025、2026：依序出現 2025、2024，各在該年最新的日段之前")
    fun `T2 6 9`() {
        val weeks =
            entries(
                listOf(tx(LocalDate(2026, 2, 4)), tx(LocalDate(2025, 6, 4)), tx(LocalDate(2025, 3, 5)), tx(LocalDate(2024, 9, 4))),
                LocalDate(2026, 2, 4),
            )
        assertEquals(listOf("2/4x1", "Y2025", "6/4x1", "3/5x1", "Y2024", "9/4x1"), weeks.flatMap(::shape))
    }
}
