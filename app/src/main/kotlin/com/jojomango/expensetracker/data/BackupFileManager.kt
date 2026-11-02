package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupFileStore
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.BackupWriteResult
import com.jojomango.expensetracker.domain.encodeBackup
import javax.inject.Inject

/**
 * @property location 還能用的備份檔；`null` 代表沒有。
 * @property lost 記住的檔案已不存在，記憶剛被清除，要告訴使用者。
 */
data class BackupFileLookup(
    val location: String?,
    val lost: Boolean = false,
)

/** SPEC.md §3.6 v2.9：App 記住的備份檔。手動匯出與自動備份共用，規則才不會分岔。 */
class BackupFileManager
    @Inject
    constructor(
        private val backups: BackupRepository,
        private val reminders: BackupReminderRepository,
        private val files: BackupFileStore,
    ) {
        suspend fun write(location: String): BackupWriteResult =
            runCatching { files.write(location, encodeBackup(backups.export())) }
                .getOrDefault(BackupWriteResult.FAILED)

        suspend fun displayName(location: String): String? = files.displayName(location)

        /** 回傳 `false` 代表服務不給長期授權，這個檔案記不住，也就不能開自動備份。 */
        suspend fun remember(
            location: String,
            enableAuto: Boolean,
        ): Boolean {
            if (!files.keep(location)) return false
            val previous = reminders.get().backupFileLocation
            if (previous != null && previous != location) files.release(previous)
            reminders.setBackupFileLocation(location)
            if (enableAuto) reminders.setAutoBackupEnabled(true)
            return true
        }

        suspend fun lookup(): BackupFileLookup {
            val location = reminders.get().backupFileLocation
            val usable = location != null && files.exists(location)
            if (location != null && !usable) forget(location)
            return BackupFileLookup(location.takeIf { usable }, lost = location != null && !usable)
        }

        suspend fun forget(location: String) {
            files.release(location)
            reminders.setBackupFileLocation(null)
        }
    }
