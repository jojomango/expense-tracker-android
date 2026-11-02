package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupFileStore
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupReminderState
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.BackupWriteResult
import com.jojomango.expensetracker.domain.encodeBackup
import com.jojomango.expensetracker.domain.isBackupDue
import com.jojomango.expensetracker.domain.shouldAttemptAutoBackup
import com.jojomango.expensetracker.domain.shouldSendBackupReminder
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

/**
 * SPEC.md §3.6 v2.9 的每日檢查：能自動備份就直接完成，否則走提醒通知。
 *
 * [run] 的 `notify` 收到 `true` 代表自動備份失敗；回傳 `false` 代表通知發不出去，這時不記錄
 * 「提醒過了」——等使用者打開通知後，下一次檢查就會馬上提醒。
 */
class BackupCheck(
    private val reminders: BackupReminderRepository,
    private val backups: BackupRepository,
    private val files: BackupFileStore,
) {
    suspend fun run(
        now: Instant,
        timeZone: TimeZone,
        notify: (autoBackupFailed: Boolean) -> Boolean,
    ) {
        val state = reminders.get()
        // 先看開關與到期：export 要讀整個資料庫，不該為了每天的檢查白跑。
        val payload = if (state.autoBackupEnabled && isBackupDue(state, now, timeZone)) backups.export() else null
        val attempted = payload != null && shouldAttemptAutoBackup(state, payload.transactions.size, now, timeZone)
        if (attempted && payload != null && writeAutoBackup(state, encodeBackup(payload))) {
            reminders.recordBackup(now)
        } else if (shouldSendBackupReminder(reminders.get(), now, timeZone) && notify(attempted)) {
            reminders.recordReminder(now)
        }
    }

    private suspend fun writeAutoBackup(
        state: BackupReminderState,
        content: String,
    ): Boolean = state.backupFileLocation?.let { files.write(it, content) } == BackupWriteResult.SUCCESS
}
