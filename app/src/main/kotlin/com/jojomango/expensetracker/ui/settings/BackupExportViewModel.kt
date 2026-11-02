package com.jojomango.expensetracker.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.data.BackupFileManager
import com.jojomango.expensetracker.domain.BACKUP_FILE_NAME
import com.jojomango.expensetracker.domain.BackupReminderRepository
import com.jojomango.expensetracker.domain.BackupWriteResult
import com.jojomango.expensetracker.domain.backupFileName
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import com.jojomango.expensetracker.ui.i18n.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class BackupUpdatePrompt(
    val fileName: String?,
    val lastBackupAt: Instant?,
)

data class BackupExportUiState(
    val isWorking: Boolean = false,
    val updatePrompt: BackupUpdatePrompt? = null,
)

/** 檔案選擇器要由畫面開啟；各用一個 launcher，程序被回收後結果才不會失去用途。 */
enum class BackupPicker { CHOOSE_FILE, ENABLE_AUTO, SAVE_COPY }

data class BackupPickerRequest(
    val picker: BackupPicker,
    val suggestedName: String,
)

enum class BackupUpdateChoice { UPDATE, SAVE_AS_NEW, CANCEL }

/** SPEC.md §3.6 v2.9：手動匯出與自動備份的設定。自動備份本身在 `BackupCheck`。 */
@HiltViewModel
class BackupExportViewModel
    @Inject
    constructor(
        private val backupFiles: BackupFileManager,
        private val backupReminderRepository: BackupReminderRepository,
        private val analyticsTracker: AnalyticsTracker,
    ) : ViewModel() {
        private val uiState = MutableStateFlow(BackupExportUiState())
        val state: StateFlow<BackupExportUiState> = uiState.asStateFlow()

        val autoBackupEnabled: StateFlow<Boolean> =
            backupReminderRepository
                .observe()
                .map { it.autoBackupEnabled }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

        val backupFileName: StateFlow<String?> =
            backupReminderRepository
                .observe()
                .map { state -> state.backupFileLocation?.let { backupFiles.displayName(it) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        private val pickerFlow = MutableSharedFlow<BackupPickerRequest>(extraBufferCapacity = 1)
        val backupPickerRequests: SharedFlow<BackupPickerRequest> = pickerFlow

        private val messageFlow = MutableSharedFlow<UiMessage>(extraBufferCapacity = 1)
        val messages: SharedFlow<UiMessage> = messageFlow

        fun onExportClicked() {
            viewModelScope.launch {
                val location = usableFile()
                if (location == null) {
                    pickerFlow.emit(BackupPickerRequest(BackupPicker.CHOOSE_FILE, BACKUP_FILE_NAME))
                } else {
                    val prompt = BackupUpdatePrompt(backupFiles.displayName(location), backupReminderRepository.get().lastBackupAt)
                    uiState.update { it.copy(updatePrompt = prompt) }
                }
            }
        }

        fun onBackupUpdateChoice(choice: BackupUpdateChoice) {
            uiState.update { it.copy(updatePrompt = null) }
            when (choice) {
                BackupUpdateChoice.UPDATE -> updateRememberedFile()
                BackupUpdateChoice.SAVE_AS_NEW -> {
                    val name = backupFileName(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                    pickerFlow.tryEmit(BackupPickerRequest(BackupPicker.SAVE_COPY, name))
                }
                BackupUpdateChoice.CANCEL -> Unit
            }
        }

        fun changeBackupFile() {
            pickerFlow.tryEmit(BackupPickerRequest(BackupPicker.CHOOSE_FILE, BACKUP_FILE_NAME))
        }

        fun setAutoBackupEnabled(enabled: Boolean) {
            viewModelScope.launch {
                if (!enabled || usableFile() != null) {
                    backupReminderRepository.setAutoBackupEnabled(enabled)
                } else {
                    pickerFlow.emit(BackupPickerRequest(BackupPicker.ENABLE_AUTO, BACKUP_FILE_NAME))
                }
            }
        }

        fun onBackupFileChosen(
            uri: Uri,
            enableAuto: Boolean,
        ) {
            runExclusive {
                val location = uri.toString()
                if (backupFiles.write(location) == BackupWriteResult.SUCCESS) {
                    val remembered = backupFiles.remember(location, enableAuto)
                    recordExported(location)
                    // 這次寫入成功，但服務不給長期授權，之後背景就寫不進去，自動備份不能開。
                    if (enableAuto && !remembered) messageFlow.emit(UiMessage(R.string.settings_export_failed))
                } else {
                    messageFlow.emit(UiMessage(R.string.settings_export_failed))
                }
            }
        }

        fun onBackupCopyChosen(uri: Uri) {
            runExclusive {
                val location = uri.toString()
                if (backupFiles.write(location) == BackupWriteResult.SUCCESS) {
                    recordExported(location)
                } else {
                    messageFlow.emit(UiMessage(R.string.settings_export_failed))
                }
            }
        }

        private fun updateRememberedFile() {
            runExclusive {
                val location = backupReminderRepository.get().backupFileLocation ?: return@runExclusive
                when (backupFiles.write(location)) {
                    BackupWriteResult.SUCCESS -> recordExported(location)
                    BackupWriteResult.MISSING -> {
                        backupFiles.forget(location)
                        messageFlow.emit(UiMessage(R.string.settings_backup_file_missing))
                        pickerFlow.emit(BackupPickerRequest(BackupPicker.CHOOSE_FILE, BACKUP_FILE_NAME))
                    }
                    BackupWriteResult.FAILED -> messageFlow.emit(UiMessage(R.string.settings_export_failed))
                }
            }
        }

        // 只有真的寫成功才算備份過——寫到一半失敗不能重設提醒的計時。
        private suspend fun recordExported(location: String) {
            backupReminderRepository.recordBackup(Clock.System.now())
            analyticsTracker.track(AnalyticsEvent.BackupExported)
            val name = backupFiles.displayName(location) ?: BACKUP_FILE_NAME
            messageFlow.emit(UiMessage(R.string.settings_export_done, listOf(name)))
        }

        private suspend fun usableFile(): String? {
            val lookup = backupFiles.lookup()
            if (lookup.lost) messageFlow.emit(UiMessage(R.string.settings_backup_file_missing))
            return lookup.location
        }

        /** 同一時間只跑一個寫入——連點兩次不該同時覆蓋同一個檔案。 */
        private fun runExclusive(block: suspend () -> Unit) {
            if (uiState.value.isWorking) return
            uiState.update { it.copy(isWorking = true) }
            viewModelScope.launch {
                try {
                    block()
                } finally {
                    uiState.update { it.copy(isWorking = false) }
                }
            }
        }
    }
