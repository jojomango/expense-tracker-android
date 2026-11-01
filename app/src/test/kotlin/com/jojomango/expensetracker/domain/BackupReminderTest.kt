package com.jojomango.expensetracker.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.hours

/** TESTCASES.md T4.4（SPEC.md §3.6 v2.8）與備份檔名格式。 */
class BackupReminderTest {
    private val taipei = TimeZone.of("Asia/Taipei")

    /** 當地那天中午：這些案例要測的是日期邊界，不是一天內的時刻。 */
    private fun day(iso: String) = LocalDate.parse(iso).atStartOfDayIn(taipei).plus(12.hours)

    private fun state(
        first: String? = null,
        backup: String? = null,
        reminded: String? = null,
    ) = BackupReminderState(first?.let(::day), backup?.let(::day), reminded?.let(::day))

    private fun assertBoth(
        expected: Boolean,
        state: BackupReminderState,
        now: Instant,
    ) {
        assertEquals(expected, isBackupDue(state, now, taipei), "isBackupDue")
        assertEquals(expected, shouldSendBackupReminder(state, now, taipei), "shouldSendBackupReminder")
    }

    @Test
    @DisplayName("T4.4.1 — 從未啟動過不提醒")
    fun `T4 4 1`() = assertBoth(false, state(), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.2 — 還沒到月底，最近月底距首次 29 天")
    fun `T4 4 2`() = assertBoth(false, state(first = "2026-09-01"), day("2026-10-30"))

    @Test
    @DisplayName("T4.4.3 — 月底當天提醒")
    fun `T4 4 3`() = assertBoth(true, state(first = "2026-09-01"), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.4 — 恰滿 30 天提醒（邊界）")
    fun `T4 4 4`() = assertBoth(true, state(first = "2026-10-01"), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.5 — 29 天不提醒")
    fun `T4 4 5`() = assertBoth(false, state(first = "2026-10-02"), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.6 — 接 T4.4.5，下一個月底提醒")
    fun `T4 4 6`() = assertBoth(true, state(first = "2026-10-02"), day("2026-11-30"))

    @Test
    @DisplayName("T4.4.7 — 當月第一天備份過就略過（邊界）")
    fun `T4 4 7`() = assertBoth(false, state(first = "2026-01-01", backup = "2026-10-01"), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.8 — 上個月底備份不算當月")
    fun `T4 4 8`() = assertBoth(true, state(first = "2026-01-01", backup = "2026-09-30"), day("2026-10-31"))

    @Test
    @DisplayName("T4.4.9 — 月底沒檢查到，下次補發")
    fun `T4 4 9`() = assertBoth(true, state(first = "2026-01-01", reminded = "2026-09-30"), day("2026-11-02"))

    @Test
    @DisplayName("T4.4.10 — 月底之後才備份，不再補發")
    fun `T4 4 10`() = assertBoth(false, state(first = "2026-01-01", backup = "2026-11-01", reminded = "2026-09-30"), day("2026-11-02"))

    @Test
    @DisplayName("T4.4.11 — 同一個月底補發過就不再發，但設定頁提示還在")
    fun `T4 4 11`() {
        val state = state(first = "2026-01-01", reminded = "2026-11-01")
        assertTrue(isBackupDue(state, day("2026-11-02"), taipei))
        assertFalse(shouldSendBackupReminder(state, day("2026-11-02"), taipei))
    }

    @Test
    @DisplayName("T4.4.12 — 月底當天發過就不再發")
    fun `T4 4 12`() {
        val state = state(first = "2026-01-01", reminded = "2026-10-31")
        assertTrue(isBackupDue(state, day("2026-10-31"), taipei))
        assertFalse(shouldSendBackupReminder(state, day("2026-10-31"), taipei))
    }

    @Test
    @DisplayName("T4.4.13 — 平年 2/28 是月底")
    fun `T4 4 13`() = assertBoth(true, state(first = "2026-01-01"), day("2026-02-28"))

    @Test
    @DisplayName("T4.4.14 — 閏年 2/28 還不是月底")
    fun `T4 4 14`() = assertFalse(shouldSendBackupReminder(state(first = "2027-01-01", reminded = "2028-01-31"), day("2028-02-28"), taipei))

    @Test
    @DisplayName("T4.4.15 — 閏年 2/29 才是月底")
    fun `T4 4 15`() = assertTrue(shouldSendBackupReminder(state(first = "2027-01-01", reminded = "2028-01-31"), day("2028-02-29"), taipei))

    @Test
    @DisplayName("T4.4.16 — 用當地日期判斷月底（台北 10/31 23:59）")
    fun `T4 4 16`() = assertBoth(true, state(first = "2026-01-01"), Instant.parse("2026-10-31T15:59:00Z"))

    @Test
    @DisplayName("T4.4.17 — 用當地日期判斷備份日（UTC 9/30、台北 10/1）")
    fun `T4 4 17`() {
        val state = BackupReminderState(day("2026-01-01"), Instant.parse("2026-09-30T16:30:00Z"))
        assertBoth(false, state, day("2026-10-31"))
    }

    @Test
    @DisplayName("備份檔名格式為 expense-backup-YYYYMMDD-HHmm.json，個位數月日時分補零")
    fun `backup file name zero padded`() {
        assertEquals(
            "expense-backup-20260805-0907.json",
            backupFileName(LocalDateTime(2026, 8, 5, 9, 7)),
        )
    }

    @Test
    @DisplayName("備份檔名用 24 小時制")
    fun `backup file name 24 hour clock`() {
        assertEquals(
            "expense-backup-20261231-2359.json",
            backupFileName(LocalDateTime(2026, 12, 31, 23, 59)),
        )
    }
}
