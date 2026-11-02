package com.jojomango.expensetracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/**
 * 匯出流程裡由畫面負責的部分：檔案選擇器與更新確認對話框。
 *
 * SPEC.md §3.6：用 Storage Access Framework 讓使用者自己選存放位置（例如 Google Drive
 * 同步的資料夾）。使用者在挑選器按取消時 uri 是 null，什麼都不做——那是使用者的決定，
 * 不是錯誤，也不該被當成「已經備份過」。
 */
@Composable
internal fun BackupExportEffects(viewModel: BackupExportViewModel) {
    val export by viewModel.state.collectAsState()
    val contract = ActivityResultContracts.CreateDocument("application/json")
    val chooseFile =
        rememberLauncherForActivityResult(contract) { uri -> uri?.let { viewModel.onBackupFileChosen(it, enableAuto = false) } }
    val enableAuto = rememberLauncherForActivityResult(contract) { uri -> uri?.let { viewModel.onBackupFileChosen(it, enableAuto = true) } }
    val saveCopy = rememberLauncherForActivityResult(contract) { uri -> uri?.let(viewModel::onBackupCopyChosen) }
    LaunchedEffect(Unit) {
        viewModel.backupPickerRequests.collect { request ->
            when (request.picker) {
                BackupPicker.CHOOSE_FILE -> chooseFile
                BackupPicker.ENABLE_AUTO -> enableAuto
                BackupPicker.SAVE_COPY -> saveCopy
            }.launch(request.suggestedName)
        }
    }
    export.updatePrompt?.let { prompt ->
        BackupUpdateDialog(prompt = prompt, onChoice = viewModel::onBackupUpdateChoice)
    }
}

/** UI-SPEC.md §13.1。 */
@Composable
internal fun AutoBackupRow(
    enabled: Boolean,
    fileName: String?,
    onEnabledChange: (Boolean) -> Unit,
    onChangeFile: () -> Unit,
) {
    val typography = LocalAppTypography.current
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_auto_backup_title), style = typography.rowTitle) },
        supportingContent = {
            Text(
                fileName?.let { stringResource(R.string.settings_auto_backup_file, it) }
                    ?: stringResource(R.string.settings_auto_backup_description),
                style = typography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        // Switch 本身不接 onCheckedChange，整列才會被 TalkBack 唸成一個開關。
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange),
    )
    if (enabled) {
        TextButton(onClick = onChangeFile) { Text(stringResource(R.string.settings_auto_backup_change_file)) }
    }
}

/** UI-SPEC.md §13.2：三個按鈕放不進同一列，由強到弱直向排列。 */
@Composable
internal fun BackupUpdateDialog(
    prompt: BackupUpdatePrompt,
    onChoice: (BackupUpdateChoice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onChoice(BackupUpdateChoice.CANCEL) },
        title = { Text(stringResource(R.string.settings_backup_update_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(lastBackupText(prompt.lastBackupAt))
                prompt.fileName?.let { Text(stringResource(R.string.settings_backup_update_file, it)) }
                Text(stringResource(R.string.settings_backup_update_note))
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = { onChoice(BackupUpdateChoice.UPDATE) }) { Text(stringResource(R.string.settings_backup_update_confirm)) }
                TextButton(
                    onClick = { onChoice(BackupUpdateChoice.SAVE_AS_NEW) },
                ) { Text(stringResource(R.string.settings_backup_update_save_new)) }
                TextButton(onClick = { onChoice(BackupUpdateChoice.CANCEL) }) { Text(stringResource(R.string.common_cancel)) }
            }
        },
    )
}
