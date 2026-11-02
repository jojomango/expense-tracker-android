package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.BackupFileStore
import com.jojomango.expensetracker.domain.BackupPayload
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupReminderState
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.BackupSettings
import com.jojomango.expensetracker.domain.BackupTransaction
import com.jojomango.expensetracker.domain.BackupWriteResult
import com.jojomango.expensetracker.domain.CURRENT_BACKUP_SCHEMA_VERSION
import com.jojomango.expensetracker.domain.ImportMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant

class FakeBackupReminders(
    initial: BackupReminderState = BackupReminderState(null, null),
) : BackupReminderRepository {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<BackupReminderState> = state

    override suspend fun get() = state.value

    override suspend fun recordFirstLaunchIfAbsent(now: Instant) = Unit

    override suspend fun recordBackup(now: Instant) {
        state.value = state.value.copy(lastBackupAt = now)
    }

    override suspend fun recordReminder(now: Instant) {
        state.value = state.value.copy(lastRemindedAt = now)
    }

    override suspend fun setAutoBackupEnabled(enabled: Boolean) {
        state.value = state.value.copy(autoBackupEnabled = enabled)
    }

    override suspend fun setBackupFileLocation(location: String?) {
        state.value = state.value.copy(backupFileLocation = location)
    }
}

class FakeBackupFiles(
    var writeResult: BackupWriteResult = BackupWriteResult.SUCCESS,
    var fileExists: Boolean = true,
    var canKeep: Boolean = true,
) : BackupFileStore {
    val writes = mutableListOf<String>()
    val released = mutableListOf<String>()

    override suspend fun write(
        location: String,
        content: String,
    ): BackupWriteResult {
        writes += location
        return writeResult
    }

    override suspend fun exists(location: String) = fileExists

    override suspend fun displayName(location: String): String? = if (fileExists) "backup.json" else null

    override suspend fun keep(location: String) = canKeep

    override suspend fun release(location: String) {
        released += location
    }
}

class FakeBackups(
    private val transactions: List<BackupTransaction> = emptyList(),
) : BackupRepository {
    override suspend fun export() =
        BackupPayload(
            schemaVersion = CURRENT_BACKUP_SCHEMA_VERSION,
            exportedAt = 0,
            wallets = emptyList(),
            transactions = transactions,
            categories = emptyList(),
            settings = BackupSettings(weekStartDay = 1, theme = "system", defaultWalletId = null),
        )

    override suspend fun import(
        payload: BackupPayload,
        mode: ImportMode,
    ) = Unit
}
