package com.jojomango.expensetracker.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupReminderState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [BackupReminderState] 存在 SharedPreferences 而不是 Room：這是裝置狀態不是使用者資料，
 * 放 Room 就得跟著 schema migration 走，還會被 `RoomBackupRepository` 一起匯出匯入——
 * 從備份還原到新裝置時，新裝置應該從自己的第一次啟動重新計時，不該繼承舊裝置的時間。
 *
 * `@Singleton` 很重要：設定頁（顯示提醒 banner）跟 `BackupReminderWorker`（發通知）
 * 共用同一個實例，任何一邊寫入後，另一邊透過 [observe] 立刻看得到。
 */
@Singleton
class PreferencesBackupReminderRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : BackupReminderRepository {
        private val prefs: SharedPreferences by lazy {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        // null = 還沒從磁碟讀過。第一次讀在 IO thread 上，不卡主執行緒。
        private val state = MutableStateFlow<BackupReminderState?>(null)
        private val writeLock = Mutex()

        override fun observe(): Flow<BackupReminderState> = state.onStart { ensureLoaded() }.filterNotNull()

        override suspend fun get(): BackupReminderState = ensureLoaded()

        override suspend fun recordFirstLaunchIfAbsent(now: Instant) {
            update { current -> if (current.firstLaunchAt != null) current else current.copy(firstLaunchAt = now) }
        }

        override suspend fun recordBackup(now: Instant) {
            update { it.copy(lastBackupAt = now) }
        }

        override suspend fun recordReminder(now: Instant) {
            update { it.copy(lastRemindedAt = now) }
        }

        private suspend fun ensureLoaded(): BackupReminderState =
            state.value ?: withContext(Dispatchers.IO) {
                val loaded =
                    BackupReminderState(
                        firstLaunchAt = prefs.readInstant(KEY_FIRST_LAUNCH_AT),
                        lastBackupAt = prefs.readInstant(KEY_LAST_BACKUP_AT),
                        lastRemindedAt = prefs.readInstant(KEY_LAST_REMINDED_AT),
                    )
                state.compareAndSet(null, loaded)
                state.value ?: loaded
            }

        private suspend fun update(transform: (BackupReminderState) -> BackupReminderState) {
            writeLock.withLock {
                val next = transform(ensureLoaded())
                withContext(Dispatchers.IO) {
                    prefs.edit(commit = true) {
                        writeInstant(KEY_FIRST_LAUNCH_AT, next.firstLaunchAt)
                        writeInstant(KEY_LAST_BACKUP_AT, next.lastBackupAt)
                        writeInstant(KEY_LAST_REMINDED_AT, next.lastRemindedAt)
                    }
                }
                state.value = next
            }
        }

        private fun SharedPreferences.readInstant(key: String): Instant? =
            if (contains(key)) Instant.fromEpochMilliseconds(getLong(key, 0L)) else null

        private fun SharedPreferences.Editor.writeInstant(
            key: String,
            value: Instant?,
        ) {
            if (value == null) remove(key) else putLong(key, value.toEpochMilliseconds())
        }

        private companion object {
            const val PREFS_NAME = "backup_reminder"
            const val KEY_FIRST_LAUNCH_AT = "first_launch_at"
            const val KEY_LAST_BACKUP_AT = "last_backup_at"
            const val KEY_LAST_REMINDED_AT = "last_reminded_at"
        }
    }
