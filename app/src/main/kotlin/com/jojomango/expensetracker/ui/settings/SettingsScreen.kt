@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.Theme
import com.jojomango.expensetracker.ui.notification.rememberEnableNotifications
import com.jojomango.expensetracker.ui.notification.rememberNotificationsEnabled
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 設定頁：錢包/分類管理入口、週起始日、外觀、備份與還原（SPEC.md §3.5、§3.6）。
 *
 * 還沒有任何錢包時也進得來（首次啟動引導頁的「從備份還原」）——換新裝置的使用者第一步
 * 就是匯入備份，不該被迫先建一個用不到的錢包。這點跟網頁版 `Home.tsx` 的設計一致。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageWallets: () -> Unit,
    onManageCategories: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // 點空白處收起鍵盤。確認字欄位的鍵盤會蓋住下面的「選擇備份檔並匯入」，
                    // 光靠鍵盤上的「完成」不夠直覺。
                    .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            ListItem(
                headlineContent = { Text("管理錢包") },
                modifier = Modifier.clickable(role = Role.Button, onClick = onManageWallets),
            )
            ListItem(
                headlineContent = { Text("分類管理") },
                modifier = Modifier.clickable(role = Role.Button, onClick = onManageCategories),
            )

            SectionTitle("週起始日")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekStartDayOptions.forEach { (day, label) ->
                    FilterChip(
                        selected = settings.weekStartDay == day,
                        onClick = { viewModel.setWeekStartDay(day) },
                        label = { Text(label) },
                    )
                }
            }

            SectionTitle("外觀")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeOptions.forEach { (theme, label) ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        label = { Text(label) },
                    )
                }
            }

            BackupSection(viewModel)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = LocalAppTypography.current.cardTitle,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
}

@Composable
private fun BackupSection(viewModel: SettingsViewModel) {
    val backup by viewModel.backup.collectAsState()
    val isBackupDue by viewModel.isBackupDue.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    val typography = LocalAppTypography.current

    // SPEC.md §3.6：用 Storage Access Framework 讓使用者自己選存放位置（例如 Google Drive
    // 同步的資料夾）。使用者在挑選器按取消時 uri 是 null，什麼都不做——那是使用者的決定，
    // 不是錯誤，也不該被當成「已經備份過」。
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) viewModel.exportTo(uri)
        }
    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) viewModel.importFrom(uri)
        }

    SectionTitle("備份與還原")

    if (isBackupDue) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Text(
                "你已經一段時間沒有備份資料了，建議匯出備份以免資料遺失。",
                style = typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
        }
    }

    Text(
        lastBackupText(lastBackupAt),
        style = typography.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { exportLauncher.launch(viewModel.suggestedBackupFileName()) },
        enabled = !backup.isWorking,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("匯出備份") }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Text("匯入備份", style = typography.bodyMedium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

    Column(modifier = Modifier.selectableGroup()) {
        importModeOptions.forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(
                            selected = backup.importMode == mode,
                            onClick = { viewModel.setImportMode(mode) },
                            role = Role.RadioButton,
                        ),
            ) {
                // 整列可點、RadioButton 本身不另外接 onClick，TalkBack 才會把這一列唸成一個選項。
                RadioButton(selected = backup.importMode == mode, onClick = null)
                Text(label, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    if (backup.importMode == ImportMode.REPLACE) {
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = backup.confirmText,
            onValueChange = viewModel::setConfirmText,
            label = { Text("輸入「$REPLACE_CONFIRM_PHRASE」以確認清空現有資料") },
            singleLine = true,
            // 輸入完按鍵盤的「完成」就收起鍵盤，不然鍵盤會蓋住下面的「選擇備份檔並匯入」。
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }

    Spacer(Modifier.height(12.dp))
    FilledTonalButton(
        onClick = {
            // 備份檔一定是 JSON，但不同的檔案管理員/雲端硬碟 app 給的 MIME type 不一定是
            // application/json（有的是 text/plain 或 application/octet-stream），只允許
            // application/json 的話使用者可能根本選不到自己匯出的檔案，所以放寬。
            if (viewModel.canStartImport()) importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
        },
        enabled = !backup.isWorking,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("選擇備份檔並匯入") }

    if (backup.isWorking) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }

    NotificationRow()
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NotificationRow() {
    val enabled = rememberNotificationsEnabled()
    val enableNotifications = rememberEnableNotifications()
    if (enabled) return
    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(
            "備份提醒通知目前是關閉的，開啟後每 7 天會提醒你備份一次。",
            style = LocalAppTypography.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = enableNotifications) { Text("開啟通知") }
    }
}

private fun lastBackupText(lastBackupAt: Instant?): String {
    if (lastBackupAt == null) return "尚未備份過"
    val t = lastBackupAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = t.minute.toString().padStart(2, '0')
    return "上次備份：${t.year}/${t.monthNumber}/${t.dayOfMonth} ${t.hour}:$minute"
}

private val weekStartDayOptions =
    listOf(
        DayOfWeek.SUNDAY to "週日",
        DayOfWeek.MONDAY to "週一",
        DayOfWeek.TUESDAY to "週二",
        DayOfWeek.WEDNESDAY to "週三",
        DayOfWeek.THURSDAY to "週四",
        DayOfWeek.FRIDAY to "週五",
        DayOfWeek.SATURDAY to "週六",
    )

private val themeOptions =
    listOf(
        Theme.SYSTEM to "跟隨系統",
        Theme.LIGHT to "淺色",
        Theme.DARK to "深色",
    )

/** 標籤文字跟網頁版 `Settings.tsx` 相同。 */
private val importModeOptions =
    listOf(
        ImportMode.MERGE to "合併（保留現有資料）",
        ImportMode.REPLACE to "取代（清空現有資料）",
    )
