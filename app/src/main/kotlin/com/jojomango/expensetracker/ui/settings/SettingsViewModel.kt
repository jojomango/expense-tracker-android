package com.jojomango.expensetracker.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BackupIntegrityException
import com.jojomango.expensetracker.domain.BackupParseException
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupRepository
import com.jojomango.expensetracker.domain.BackupSchemaTooNewException
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.Theme
import com.jojomango.expensetracker.domain.backupFileName
import com.jojomango.expensetracker.domain.decodeBackup
import com.jojomango.expensetracker.domain.encodeBackup
import com.jojomango.expensetracker.domain.isBackupDue
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import com.jojomango.expensetracker.ui.i18n.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.io.IOException
import javax.inject.Inject

data class BackupUiState(
    /** 預設合併，跟網頁版一樣——取代會清空資料，不該是不小心就選到的預設值。 */
    val importMode: ImportMode = ImportMode.MERGE,
    val confirmText: String = "",
    val isWorking: Boolean = false,
)

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val backupRepository: BackupRepository,
        private val backupReminderRepository: BackupReminderRepository,
        private val analyticsTracker: AnalyticsTracker,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        val settings: StateFlow<Settings> =
            settingsRepository.observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

        val isBackupDue: StateFlow<Boolean> =
            backupReminderRepository
                .observe()
                .map { isBackupDue(it, Clock.System.now()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val lastBackupAt: StateFlow<Instant?> =
            backupReminderRepository
                .observe()
                .map { it.lastBackupAt }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        private val backupState = MutableStateFlow(BackupUiState())
        val backup: StateFlow<BackupUiState> = backupState.asStateFlow()

        private val messageFlow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
        val messages: SharedFlow<UiMessage> = messageFlow

        /** SPEC.md §3.5：變更後所有錢包的週分組即時重算，不改變任何交易資料——這件事
         * 天生成立，因為 Week/Budget 的計算永遠即時、不快取任何跟 weekStartDay 有關的值。 */
        fun setWeekStartDay(day: DayOfWeek) {
            viewModelScope.launch {
                settingsRepository.update(settingsRepository.get().copy(weekStartDay = day))
            }
        }

        fun setTheme(theme: Theme) {
            viewModelScope.launch {
                val current = settingsRepository.get()
                if (current.theme == theme) return@launch
                settingsRepository.update(current.copy(theme = theme))
                analyticsTracker.track(AnalyticsEvent.ThemeChanged(theme))
            }
        }

        fun setImportMode(mode: ImportMode) {
            backupState.update { it.copy(importMode = mode) }
        }

        fun setConfirmText(text: String) {
            backupState.update { it.copy(confirmText = text) }
        }

        fun suggestedBackupFileName(): String = backupFileName(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))

        /**
         * 打開檔案挑選器之前先檢查。取代模式要先輸入確認字——在選檔**之前**擋下來，
         * 不然使用者選完檔案才被告知要回頭輸入，還得重選一次。
         *
         * [confirmPhrase] 依目前語言（繁中「確認取代」跟網頁版 `Settings.tsx` 同一個字；英文 `REPLACE`），
         * 由畫面從 string resources 傳進來。
         */
        fun canStartImport(confirmPhrase: String): Boolean {
            val state = backupState.value
            if (state.importMode == ImportMode.REPLACE && state.confirmText.trim() != confirmPhrase) {
                messageFlow.tryEmit(UiMessage(R.string.settings_import_confirm_required, listOf(confirmPhrase)))
                return false
            }
            return true
        }

        fun exportTo(uri: Uri) {
            runExclusive {
                val written =
                    runCatching {
                        val json = encodeBackup(backupRepository.export())
                        withContext(Dispatchers.IO) {
                            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open file")
                            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        }
                    }
                if (written.isSuccess) {
                    // 只有真的寫成功才算備份過——寫到一半失敗不能重設提醒的計時。
                    backupReminderRepository.recordBackup(Clock.System.now())
                    analyticsTracker.track(AnalyticsEvent.BackupExported)
                    messageFlow.emit(UiMessage(R.string.settings_export_done, listOf(displayNameOf(uri) ?: suggestedBackupFileName())))
                } else {
                    messageFlow.emit(UiMessage(R.string.settings_export_failed))
                }
            }
        }

        // 例外本身的 message 是 domain 層給開發者看的英文，不是給使用者看的；這裡刻意
        // 丟掉它，換成 string resources 裡對應的訊息（CLAUDE.md：介面文字一律放在 string resources）。
        @Suppress("SwallowedException")
        fun importFrom(uri: Uri) {
            val mode = backupState.value.importMode
            runExclusive {
                val message =
                    try {
                        val json =
                            withContext(Dispatchers.IO) {
                                val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open file")
                                stream.use { it.readBytes().toString(Charsets.UTF_8) }
                            }
                        // TESTCASES.md T4.2.7：一萬筆也要 3 秒內完成、UI 不凍結，解析跟寫入都不在主執行緒。
                        withContext(Dispatchers.Default) { backupRepository.import(decodeBackup(json), mode) }
                        backupState.update { it.copy(confirmText = "") }
                        UiMessage(R.string.settings_import_done)
                    } catch (e: IOException) {
                        UiMessage(R.string.settings_import_failed_read)
                    } catch (e: SecurityException) {
                        // 使用者在檔案管理員撤銷了這個檔案的存取權限
                        UiMessage(R.string.settings_import_failed_read)
                    } catch (e: BackupSchemaTooNewException) {
                        UiMessage(R.string.settings_import_schema_too_new)
                    } catch (e: BackupIntegrityException) {
                        UiMessage(R.string.settings_import_failed_integrity)
                    } catch (e: BackupParseException) {
                        UiMessage(R.string.settings_import_failed_parse)
                    }
                analyticsTracker.track(AnalyticsEvent.BackupImported(mode, success = message.id == R.string.settings_import_done))
                messageFlow.emit(message)
            }
        }

        /** 匯出/匯入同一時間只跑一個——連點兩次按鈕不該同時開兩個寫入。 */
        private fun runExclusive(block: suspend () -> Unit) {
            if (backupState.value.isWorking) return
            backupState.update { it.copy(isWorking = true) }
            viewModelScope.launch {
                try {
                    block()
                } finally {
                    backupState.update { it.copy(isWorking = false) }
                }
            }
        }

        private fun displayNameOf(uri: Uri): String? =
            runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
            }.getOrNull()
    }
