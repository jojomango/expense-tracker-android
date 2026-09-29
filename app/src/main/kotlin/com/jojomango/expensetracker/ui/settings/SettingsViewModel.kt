package com.jojomango.expensetracker.ui.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/** 跟網頁版 `Settings.tsx` 的 `REPLACE_CONFIRM_PHRASE` 同一個字，兩邊行為一致。 */
const val REPLACE_CONFIRM_PHRASE = "確認取代"

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

        private val messageFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val messages: SharedFlow<String> = messageFlow

        /** SPEC.md §3.5：變更後所有錢包的週分組即時重算，不改變任何交易資料——這件事
         * 天生成立，因為 Week/Budget 的計算永遠即時、不快取任何跟 weekStartDay 有關的值。 */
        fun setWeekStartDay(day: DayOfWeek) {
            viewModelScope.launch {
                settingsRepository.update(settingsRepository.get().copy(weekStartDay = day))
            }
        }

        fun setTheme(theme: Theme) {
            viewModelScope.launch {
                settingsRepository.update(settingsRepository.get().copy(theme = theme))
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
         */
        fun canStartImport(): Boolean {
            val state = backupState.value
            if (state.importMode == ImportMode.REPLACE && state.confirmText.trim() != REPLACE_CONFIRM_PHRASE) {
                messageFlow.tryEmit("請輸入「$REPLACE_CONFIRM_PHRASE」以確認清空現有資料")
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
                            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("無法開啟檔案")
                            stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        }
                    }
                if (written.isSuccess) {
                    // 只有真的寫成功才算備份過——寫到一半失敗不能重設提醒的計時。
                    backupReminderRepository.recordBackup(Clock.System.now())
                    messageFlow.emit("已匯出 ${displayNameOf(uri) ?: suggestedBackupFileName()}")
                } else {
                    messageFlow.emit("匯出失敗：無法寫入這個位置，請換一個位置再試")
                }
            }
        }

        // 例外本身的 message 是 domain 層給開發者看的英文，不是給使用者看的；這裡刻意
        // 丟掉它，換成對應的繁體中文訊息（CLAUDE.md「面向使用者的文字一律繁體中文」）。
        @Suppress("SwallowedException")
        fun importFrom(uri: Uri) {
            val mode = backupState.value.importMode
            runExclusive {
                val message =
                    try {
                        val json =
                            withContext(Dispatchers.IO) {
                                val stream = context.contentResolver.openInputStream(uri) ?: throw IOException("無法開啟檔案")
                                stream.use { it.readBytes().toString(Charsets.UTF_8) }
                            }
                        // TESTCASES.md T4.2.7：一萬筆也要 3 秒內完成、UI 不凍結，解析跟寫入都不在主執行緒。
                        withContext(Dispatchers.Default) { backupRepository.import(decodeBackup(json), mode) }
                        backupState.update { it.copy(confirmText = "") }
                        "匯入完成"
                    } catch (e: IOException) {
                        "匯入失敗：無法讀取這個檔案"
                    } catch (e: SecurityException) {
                        // 使用者在檔案管理員撤銷了這個檔案的存取權限
                        "匯入失敗：無法讀取這個檔案"
                    } catch (e: BackupSchemaTooNewException) {
                        "這個備份檔來自較新版本的 App，請先更新 App 再匯入"
                    } catch (e: BackupIntegrityException) {
                        "匯入失敗：備份檔內容不完整或已損毀"
                    } catch (e: BackupParseException) {
                        "匯入失敗：這不是有效的備份檔"
                    }
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
