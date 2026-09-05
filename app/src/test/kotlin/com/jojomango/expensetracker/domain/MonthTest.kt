package com.jojomango.expensetracker.domain

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T5 — Month.
 *
 * 網頁版原本有一條「非法格式輸入拋錯」的測案——`monthRangeOf` 的參數型別是
 * `LocalDate`，非法格式在呼叫端解析字串時就已經被擋掉，這裡不需要重複測試
 * （型別系統省下的一個測案，見 TESTCASES.md T5 附註）。
 */
class MonthTest {
    @Test
    @DisplayName("T5.1.1 — 2026-08-11（一般月份，31 天）-> 2026-08-01 ~ 2026-08-31")
    fun `T5 1 1`() {
        val range = Month.rangeOf(LocalDate(2026, 8, 11))
        assertEquals(LocalDate(2026, 8, 1), range.start)
        assertEquals(LocalDate(2026, 8, 31), range.end)
    }

    @Test
    @DisplayName("T5.1.2 — 2026-04-15（小月，30 天）-> 2026-04-01 ~ 2026-04-30")
    fun `T5 1 2`() {
        val range = Month.rangeOf(LocalDate(2026, 4, 15))
        assertEquals(LocalDate(2026, 4, 1), range.start)
        assertEquals(LocalDate(2026, 4, 30), range.end)
    }

    @Test
    @DisplayName("T5.1.3 — 2026-02-15（平年 2 月，28 天）-> 2026-02-01 ~ 2026-02-28")
    fun `T5 1 3`() {
        val range = Month.rangeOf(LocalDate(2026, 2, 15))
        assertEquals(LocalDate(2026, 2, 1), range.start)
        assertEquals(LocalDate(2026, 2, 28), range.end)
    }

    @Test
    @DisplayName("T5.1.4 — 2028-02-15（閏年 2 月，29 天）-> 2028-02-01 ~ 2028-02-29")
    fun `T5 1 4`() {
        val range = Month.rangeOf(LocalDate(2028, 2, 15))
        assertEquals(LocalDate(2028, 2, 1), range.start)
        assertEquals(LocalDate(2028, 2, 29), range.end)
    }

    @Test
    @DisplayName("T5.1.5 — 2026-08-01（月首當天）本身即為區間起點")
    fun `T5 1 5`() {
        val range = Month.rangeOf(LocalDate(2026, 8, 1))
        assertEquals(LocalDate(2026, 8, 1), range.start)
    }

    @Test
    @DisplayName("T5.1.6 — 2026-08-31（月尾當天）本身即為區間終點")
    fun `T5 1 6`() {
        val range = Month.rangeOf(LocalDate(2026, 8, 31))
        assertEquals(LocalDate(2026, 8, 31), range.end)
    }

    @Test
    @DisplayName("T5.1.7 — 2026-12-25（12 月）-> 2026-12-01 ~ 2026-12-31，跨年邊界不受影響")
    fun `T5 1 7`() {
        val range = Month.rangeOf(LocalDate(2026, 12, 25))
        assertEquals(LocalDate(2026, 12, 1), range.start)
        assertEquals(LocalDate(2026, 12, 31), range.end)
    }
}
