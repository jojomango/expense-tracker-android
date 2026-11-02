package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupReminderState
import com.jojomango.expensetracker.domain.BackupTransaction
import com.jojomango.expensetracker.domain.BackupWriteResult
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T4.6.13 與 SPEC.md §3.6 v2.9 的每日檢查。 */
class BackupCheckTest {
    private val taipei = TimeZone.of("Asia/Taipei")
    private val now = Instant.parse("2026-10-31T04:00:00Z")
    private val transaction = BackupTransaction("t", "w", "expense", 1, null, "2026-10-30", null, 0, 0)
    private val dueState =
        BackupReminderState(
            firstLaunchAt = Instant.parse("2026-01-01T04:00:00Z"),
            lastBackupAt = null,
            autoBackupEnabled = true,
            backupFileLocation = "file",
        )
    private val notifications = mutableListOf<Boolean>()

    private suspend fun check(
        reminders: FakeBackupReminders,
        files: FakeBackupFiles,
        transactions: List<BackupTransaction> = listOf(transaction),
    ) = BackupCheck(reminders, FakeBackups(transactions), files).run(now, taipei) {
        notifications += it
        true
    }

    @Test
    @DisplayName("自動備份成功：記錄為已備份，不發通知")
    fun success() =
        runTest {
            val reminders = FakeBackupReminders(dueState)
            val files = FakeBackupFiles()
            check(reminders, files)
            assertEquals(listOf("file"), files.writes)
            assertEquals(now, reminders.state.value.lastBackupAt)
            assertEquals(emptyList<Boolean>(), notifications)
        }

    @Test
    @DisplayName("T4.6.13 — 寫入失敗：不算已備份，發失敗通知")
    fun `T4 6 13`() =
        runTest {
            val reminders = FakeBackupReminders(dueState)
            check(reminders, FakeBackupFiles(writeResult = BackupWriteResult.FAILED))
            assertNull(reminders.state.value.lastBackupAt)
            assertEquals(listOf(true), notifications)
            assertEquals(now, reminders.state.value.lastRemindedAt)
        }

    @Test
    @DisplayName("T4.6.13 — 失敗通知同一個月底只發一次，但隔天仍會重試")
    fun `T4 6 13 throttled`() =
        runTest {
            val reminders = FakeBackupReminders(dueState.copy(lastRemindedAt = now))
            val files = FakeBackupFiles(writeResult = BackupWriteResult.FAILED)
            check(reminders, files)
            assertEquals(listOf("file"), files.writes)
            assertEquals(emptyList<Boolean>(), notifications)
        }

    @Test
    @DisplayName("自動開啟但沒有記住的檔案：算失敗")
    fun missingLocation() =
        runTest {
            val files = FakeBackupFiles()
            check(FakeBackupReminders(dueState.copy(backupFileLocation = null)), files)
            assertEquals(emptyList<String>(), files.writes)
            assertEquals(listOf(true), notifications)
        }

    @Test
    @DisplayName("沒有交易：不寫入，發一般提醒")
    fun noTransactions() =
        runTest {
            val files = FakeBackupFiles()
            check(FakeBackupReminders(dueState), files, emptyList())
            assertEquals(emptyList<String>(), files.writes)
            assertEquals(listOf(false), notifications)
        }

    @Test
    @DisplayName("自動關閉：不寫入，發一般提醒")
    fun autoOff() =
        runTest {
            val files = FakeBackupFiles()
            check(FakeBackupReminders(dueState.copy(autoBackupEnabled = false)), files)
            assertEquals(emptyList<String>(), files.writes)
            assertEquals(listOf(false), notifications)
        }
}
