package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.random.Random

/**
 * TESTCASES.md T2 — Week. Domain-only, no mocks, no Clock.System.now()
 * (referenceDate is always injected — see CLAUDE.md 禁令 2).
 */
class WeekTest {
    // ---- T2.1 週區間（weekStartDay = MONDAY） ----

    @Test
    @DisplayName("T2.1.1 — 2026-08-11（週二）-> 2026-08-10 ~ 2026-08-16")
    fun `T2 1 1`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 10), range.start)
        assertEquals(LocalDate(2026, 8, 16), range.end)
    }

    @Test
    @DisplayName("T2.1.2 — 2026-08-10（週一，週首邊界）-> 2026-08-10 ~ 2026-08-16")
    fun `T2 1 2`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 10), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 10), range.start)
        assertEquals(LocalDate(2026, 8, 16), range.end)
    }

    @Test
    @DisplayName("T2.1.3 — 2026-08-16（週日，週尾邊界）-> 2026-08-10 ~ 2026-08-16")
    fun `T2 1 3`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 16), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 10), range.start)
        assertEquals(LocalDate(2026, 8, 16), range.end)
    }

    @Test
    @DisplayName("T2.1.4 — 2026-08-09（週日）-> 前一週 2026-08-03 ~ 2026-08-09")
    fun `T2 1 4`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 9), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 3), range.start)
        assertEquals(LocalDate(2026, 8, 9), range.end)
    }

    @Test
    @DisplayName("T2.1.5 — 2026-08-17（週一）-> 下一週 2026-08-17 ~ 2026-08-23")
    fun `T2 1 5`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 17), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 8, 17), range.start)
        assertEquals(LocalDate(2026, 8, 23), range.end)
    }

    // ---- T2.2 週區間（weekStartDay = SUNDAY） ----

    @Test
    @DisplayName("T2.2.1 — 2026-08-11（週二）-> 2026-08-09 ~ 2026-08-15")
    fun `T2 2 1`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.SUNDAY)
        assertEquals(LocalDate(2026, 8, 9), range.start)
        assertEquals(LocalDate(2026, 8, 15), range.end)
    }

    @Test
    @DisplayName("T2.2.2 — 2026-08-09（週日，週首）-> 2026-08-09 ~ 2026-08-15")
    fun `T2 2 2`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 9), DayOfWeek.SUNDAY)
        assertEquals(LocalDate(2026, 8, 9), range.start)
        assertEquals(LocalDate(2026, 8, 15), range.end)
    }

    @Test
    @DisplayName("T2.2.3 — 2026-08-15（週六，週尾）-> 2026-08-09 ~ 2026-08-15")
    fun `T2 2 3`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 15), DayOfWeek.SUNDAY)
        assertEquals(LocalDate(2026, 8, 9), range.start)
        assertEquals(LocalDate(2026, 8, 15), range.end)
    }

    @Test
    @DisplayName("T2.2.4 — 2026-08-16（週日）-> 2026-08-16 ~ 2026-08-22")
    fun `T2 2 4`() {
        val range = Week.rangeOf(LocalDate(2026, 8, 16), DayOfWeek.SUNDAY)
        assertEquals(LocalDate(2026, 8, 16), range.start)
        assertEquals(LocalDate(2026, 8, 22), range.end)
    }

    // ---- T2.3 週起始日全七種取值（基準日 2026-08-11，週二） ----

    @Test
    @DisplayName("T2.3.1 — weekStartDay=SUNDAY -> 週首 2026-08-09")
    fun `T2 3 1`() {
        assertEquals(LocalDate(2026, 8, 9), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.SUNDAY).start)
    }

    @Test
    @DisplayName("T2.3.2 — weekStartDay=MONDAY -> 週首 2026-08-10")
    fun `T2 3 2`() {
        assertEquals(LocalDate(2026, 8, 10), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.MONDAY).start)
    }

    @Test
    @DisplayName("T2.3.3 — weekStartDay=TUESDAY -> 週首 2026-08-11（當天即週首）")
    fun `T2 3 3`() {
        assertEquals(LocalDate(2026, 8, 11), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.TUESDAY).start)
    }

    @Test
    @DisplayName("T2.3.4 — weekStartDay=WEDNESDAY -> 週首 2026-08-05")
    fun `T2 3 4`() {
        assertEquals(LocalDate(2026, 8, 5), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.WEDNESDAY).start)
    }

    @Test
    @DisplayName("T2.3.5 — weekStartDay=THURSDAY -> 週首 2026-08-06")
    fun `T2 3 5`() {
        assertEquals(LocalDate(2026, 8, 6), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.THURSDAY).start)
    }

    @Test
    @DisplayName("T2.3.6 — weekStartDay=FRIDAY -> 週首 2026-08-07")
    fun `T2 3 6`() {
        assertEquals(LocalDate(2026, 8, 7), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.FRIDAY).start)
    }

    @Test
    @DisplayName("T2.3.7 — weekStartDay=SATURDAY -> 週首 2026-08-08")
    fun `T2 3 7`() {
        assertEquals(LocalDate(2026, 8, 8), Week.rangeOf(LocalDate(2026, 8, 11), DayOfWeek.SATURDAY).start)
    }

    // ---- T2.4 跨界情境 ----

    @Test
    @DisplayName("T2.4.1 — 跨月：2026-07-30（四），weekStart=MONDAY -> 2026-07-27 ~ 2026-08-02")
    fun `T2 4 1`() {
        val range = Week.rangeOf(LocalDate(2026, 7, 30), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2026, 7, 27), range.start)
        assertEquals(LocalDate(2026, 8, 2), range.end)
    }

    @Test
    @DisplayName("T2.4.2 — 跨年：2026-01-01（四），weekStart=MONDAY -> 2025-12-29 ~ 2026-01-04")
    fun `T2 4 2`() {
        val range = Week.rangeOf(LocalDate(2026, 1, 1), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2025, 12, 29), range.start)
        assertEquals(LocalDate(2026, 1, 4), range.end)
    }

    @Test
    @DisplayName("T2.4.3 — 閏年 2 月：2028-02-29（二），weekStart=MONDAY -> 2028-02-28 ~ 2028-03-05")
    fun `T2 4 3`() {
        val range = Week.rangeOf(LocalDate(2028, 2, 29), DayOfWeek.MONDAY)
        assertEquals(LocalDate(2028, 2, 28), range.start)
        assertEquals(LocalDate(2028, 3, 5), range.end)
    }

    @Test
    @DisplayName("T2.4.4 — 同一日期、不同 weekStartDay 回傳不同區間，且皆包含該日期")
    fun `T2 4 4`() {
        val date = LocalDate(2026, 8, 11)
        val monday = Week.rangeOf(date, DayOfWeek.MONDAY)
        val sunday = Week.rangeOf(date, DayOfWeek.SUNDAY)
        assertTrue(monday != sunday)
        assertTrue(date in monday)
        assertTrue(date in sunday)
    }

    @Test
    @DisplayName("T2.4.5 — 任意日期 x 任意 weekStartDay：區間長度恆為 7 天，且輸入日期恆落在區間內")
    fun `T2 4 5`() {
        val random = Random(seed = 42)
        val allWeekStarts = DayOfWeek.entries.toList()
        repeat(1000) {
            val epochDay = random.nextLong(-20000L, 20000L)
            val date = LocalDate.fromEpochDays(epochDay.toInt())
            val weekStartDay = allWeekStarts[random.nextInt(allWeekStarts.size)]
            val range = Week.rangeOf(date, weekStartDay)
            val lengthInDays = range.start.daysUntil(range.end) + 1
            assertEquals(7, lengthInDays, "date=$date weekStartDay=$weekStartDay range=$range")
            assertTrue(date in range, "date=$date weekStartDay=$weekStartDay range=$range")
        }
    }

    @Test
    @DisplayName("T2.4.6 — 連續 400 天，每天算一次週區間，長度恆為 7 天")
    fun `T2 4 6`() {
        var date = LocalDate(2026, 1, 1)
        repeat(400) {
            val range = Week.rangeOf(date, DayOfWeek.MONDAY)
            assertEquals(7, range.start.daysUntil(range.end) + 1)
            date = date.plus(1, DateTimeUnit.DAY)
        }
    }

    // ---- T2.5 週分組 ----

    private data class Tx(
        val date: LocalDate,
        val label: String,
    )

    @Test
    @DisplayName("T2.5.1 — 8 筆分屬 3 週的交易，分為 3 組，組內依日期排序，組間依週首倒序")
    fun `T2 5 1`() {
        val txs =
            listOf(
                Tx(LocalDate(2026, 8, 12), "a"),
                Tx(LocalDate(2026, 8, 10), "b"),
                Tx(LocalDate(2026, 8, 11), "c"),
                Tx(LocalDate(2026, 8, 3), "d"),
                Tx(LocalDate(2026, 8, 5), "e"),
                Tx(LocalDate(2026, 8, 17), "f"),
                Tx(LocalDate(2026, 8, 19), "g"),
                Tx(LocalDate(2026, 8, 20), "h"),
            )
        val groups = Week.groupByWeek(txs, DayOfWeek.MONDAY) { it.date }
        assertEquals(3, groups.size)
        // 組間依週首倒序：最新一週在前
        assertEquals(LocalDate(2026, 8, 17), groups[0].range.start)
        assertEquals(LocalDate(2026, 8, 10), groups[1].range.start)
        assertEquals(LocalDate(2026, 8, 3), groups[2].range.start)
        // 組內依日期排序
        assertEquals(listOf("f", "g", "h"), groups[0].items.map { it.label })
        assertEquals(listOf("b", "c", "a"), groups[1].items.map { it.label })
        assertEquals(listOf("d", "e"), groups[2].items.map { it.label })
    }

    @Test
    @DisplayName("T2.5.2 — 空清單，回傳空 list，不拋錯")
    fun `T2 5 2`() {
        val groups = Week.groupByWeek(emptyList<Tx>(), DayOfWeek.MONDAY) { it.date }
        assertEquals(emptyList<Any>(), groups)
    }

    @Test
    @DisplayName("T2.5.3 — 全部在同一天，1 組")
    fun `T2 5 3`() {
        val txs =
            listOf(
                Tx(LocalDate(2026, 8, 11), "a"),
                Tx(LocalDate(2026, 8, 11), "b"),
            )
        val groups = Week.groupByWeek(txs, DayOfWeek.MONDAY) { it.date }
        assertEquals(1, groups.size)
    }

    @Test
    @DisplayName("T2.5.4 — 相同資料、weekStartDay 由 MONDAY 改為 SUNDAY，分組結果改變")
    fun `T2 5 4`() {
        val txs =
            listOf(
                Tx(LocalDate(2026, 8, 9), "a"),
                Tx(LocalDate(2026, 8, 10), "b"),
            )
        val monday = Week.groupByWeek(txs, DayOfWeek.MONDAY) { it.date }
        val sunday = Week.groupByWeek(txs, DayOfWeek.SUNDAY) { it.date }
        // MONDAY: 8/9 落在上一週、8/10 落在下一週 -> 2 組
        assertEquals(2, monday.size)
        // SUNDAY: 8/9 和 8/10 同一週 -> 1 組
        assertEquals(1, sunday.size)
    }
}
