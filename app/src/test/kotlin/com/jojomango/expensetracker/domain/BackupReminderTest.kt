package com.jojomango.expensetracker.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SPEC.md §3.6「App 首次啟動後每 7 天提醒一次備份」與備份檔名格式。
 *
 * 這些都不是 TESTCASES.md 的契約項目（§3.6 沒有對應的正式測案編號），比照網頁版
 * `tests/domain/backup-reminder.test.ts` 用描述性名稱。`isBackupDue` 的 6 個案例
 * 是從網頁版逐條移植的，數值完全相同。
 */
class BackupReminderTest {
    private val now = Instant.parse("2026-08-11T00:00:00Z")

    // ---- isBackupDue：逐條移植自網頁版 shouldRemindBackup ----

    @Test
    @DisplayName("從未啟動過（firstLaunchAt 與 lastBackupAt 皆為 null）不提醒")
    fun `never launched`() {
        assertFalse(isBackupDue(BackupReminderState(firstLaunchAt = null, lastBackupAt = null), now))
    }

    @Test
    @DisplayName("首次啟動未滿 7 天不提醒")
    fun `first launch under 7 days`() {
        val state = BackupReminderState(firstLaunchAt = Instant.parse("2026-08-05T00:00:00Z"), lastBackupAt = null)
        assertFalse(isBackupDue(state, now))
    }

    @Test
    @DisplayName("首次啟動恰滿 7 天提醒（邊界）")
    fun `first launch exactly 7 days`() {
        val state = BackupReminderState(firstLaunchAt = Instant.parse("2026-08-04T00:00:00Z"), lastBackupAt = null)
        assertTrue(isBackupDue(state, now))
    }

    @Test
    @DisplayName("首次啟動超過 7 天提醒")
    fun `first launch over 7 days`() {
        val state = BackupReminderState(firstLaunchAt = Instant.parse("2026-07-01T00:00:00Z"), lastBackupAt = null)
        assertTrue(isBackupDue(state, now))
    }

    @Test
    @DisplayName("已備份過，以 lastBackupAt 而非 firstLaunchAt 為計時起點")
    fun `last backup is the baseline`() {
        val state =
            BackupReminderState(
                firstLaunchAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastBackupAt = Instant.parse("2026-08-10T00:00:00Z"),
            )
        assertFalse(isBackupDue(state, now))
    }

    @Test
    @DisplayName("距最近一次備份滿 7 天後重新提醒")
    fun `due again 7 days after last backup`() {
        val state =
            BackupReminderState(
                firstLaunchAt = Instant.parse("2026-01-01T00:00:00Z"),
                lastBackupAt = Instant.parse("2026-08-01T00:00:00Z"),
            )
        assertTrue(isBackupDue(state, Instant.parse("2026-08-08T00:00:00Z")))
    }

    // ---- shouldSendBackupReminder：Android 版特有 ----
    // 網頁版的提醒是頁內 banner，出現幾次都無所謂；Android 版是系統通知，WorkManager
    // 每天檢查一次，如果只看 isBackupDue，使用者一直不備份的話滿 7 天後會變成天天跳
    // 通知。SPEC 說的是「每 7 天提醒一次」，所以通知要另外以上一次發通知的時間節流。

    @Test
    @DisplayName("到期且從未發過通知，要發")
    fun `due and never reminded`() {
        val state = BackupReminderState(firstLaunchAt = Instant.parse("2026-08-01T00:00:00Z"), lastBackupAt = null)
        assertTrue(shouldSendBackupReminder(state, now))
    }

    @Test
    @DisplayName("到期但 7 天內已經發過通知，不重複發")
    fun `due but reminded recently`() {
        val state =
            BackupReminderState(
                firstLaunchAt = Instant.parse("2026-07-01T00:00:00Z"),
                lastBackupAt = null,
                lastRemindedAt = Instant.parse("2026-08-10T00:00:00Z"),
            )
        assertTrue(isBackupDue(state, now), "banner 仍然要顯示，只有通知被節流")
        assertFalse(shouldSendBackupReminder(state, now))
    }

    @Test
    @DisplayName("上次通知滿 7 天且仍未備份，再發一次")
    fun `reminded 7 days ago and still not backed up`() {
        val state =
            BackupReminderState(
                firstLaunchAt = Instant.parse("2026-07-01T00:00:00Z"),
                lastBackupAt = null,
                lastRemindedAt = Instant.parse("2026-08-04T00:00:00Z"),
            )
        assertTrue(shouldSendBackupReminder(state, now))
    }

    @Test
    @DisplayName("尚未到期，即使從未發過通知也不發")
    fun `not due never sends`() {
        val state = BackupReminderState(firstLaunchAt = Instant.parse("2026-08-10T00:00:00Z"), lastBackupAt = null)
        assertFalse(shouldSendBackupReminder(state, now))
    }

    // ---- 備份檔名：SPEC.md §3.6 `expense-backup-YYYYMMDD-HHmm.json` ----

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
