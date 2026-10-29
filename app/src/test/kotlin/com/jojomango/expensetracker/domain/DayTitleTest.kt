package com.jojomango.expensetracker.domain

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T7.3 — 首頁日小標題文案（UI-SPEC §4.3 v2.7，UX-006）。 */
class DayTitleTest {
    private val zh =
        DayTitleLabels(
            today = "今天",
            yesterday = "昨天",
            weekdays = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日"),
            weekdayFirst = false,
        )
    private val en =
        DayTitleLabels(
            today = "Today",
            yesterday = "Yesterday",
            weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
            weekdayFirst = true,
        )
    private val ref = LocalDate(2026, 10, 28)

    @Test
    @DisplayName("T7.3.1 — 繁中今天")
    fun `T7 3 1`() = assertEquals("今天 · 10/28 週三", DaySections.dayTitle(ref, ref, zh))

    @Test
    @DisplayName("T7.3.2 — 繁中昨天")
    fun `T7 3 2`() = assertEquals("昨天 · 10/27 週二", DaySections.dayTitle(LocalDate(2026, 10, 27), ref, zh))

    @Test
    @DisplayName("T7.3.3 — 繁中其他日")
    fun `T7 3 3`() = assertEquals("10/26 週一", DaySections.dayTitle(LocalDate(2026, 10, 26), ref, zh))

    @Test
    @DisplayName("T7.3.4 — 英文今天")
    fun `T7 3 4`() = assertEquals("Today · Wed 10/28", DaySections.dayTitle(ref, ref, en))

    @Test
    @DisplayName("T7.3.5 — 英文昨天")
    fun `T7 3 5`() = assertEquals("Yesterday · Tue 10/27", DaySections.dayTitle(LocalDate(2026, 10, 27), ref, en))

    @Test
    @DisplayName("T7.3.6 — 英文其他日")
    fun `T7 3 6`() = assertEquals("Mon 10/26", DaySections.dayTitle(LocalDate(2026, 10, 26), ref, en))

    @Test
    @DisplayName("T7.3.7 — 跨年的昨天不出現年份")
    fun `T7 3 7`() {
        val title = DaySections.dayTitle(LocalDate(2025, 12, 31), LocalDate(2026, 1, 1), zh)
        assertEquals("昨天 · 12/31 週三", title)
        assertFalse(title.contains("2025"))
    }
}
