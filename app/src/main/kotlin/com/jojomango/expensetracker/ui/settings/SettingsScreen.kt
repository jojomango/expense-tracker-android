@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.LanguagePreference
import com.jojomango.expensetracker.domain.Theme
import com.jojomango.expensetracker.ui.donation.DonationOffers
import com.jojomango.expensetracker.ui.donation.DonationTier
import com.jojomango.expensetracker.ui.donation.stringRes
import com.jojomango.expensetracker.ui.i18n.AppLocale
import com.jojomango.expensetracker.ui.notification.rememberEnableNotifications
import com.jojomango.expensetracker.ui.notification.rememberNotificationsEnabled
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 設定頁：錢包/分類管理入口、週起始日、外觀、隱私、備份與還原、支持開發者
 * （SPEC.md §3.5–§3.8；區塊順序見 UI-SPEC.md §10）。
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
    supportViewModel: PrivacyAndSupportViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }
    LaunchedEffect(Unit) {
        supportViewModel.donationMessages.collect { snackbarHostState.showSnackbar(context.getString(it.stringRes)) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } },
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
                headlineContent = { Text(stringResource(R.string.settings_manage_wallets)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onManageWallets),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_manage_categories)) },
                modifier = Modifier.clickable(role = Role.Button, onClick = onManageCategories),
            )

            SectionTitle(stringResource(R.string.settings_week_start))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekStartDayOptions.forEach { (day, label) ->
                    FilterChip(
                        selected = settings.weekStartDay == day,
                        onClick = { viewModel.setWeekStartDay(day) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_appearance))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                themeOptions.forEach { (theme, label) ->
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            LanguageSection()

            PrivacySection(supportViewModel)

            BackupSection(viewModel)

            DonationSection(supportViewModel)
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

/**
 * UI-SPEC.md §12。「繁體中文」「English」永遠用各自的語言寫（不翻譯）：看不懂目前介面的人也找得到自己的語言。
 * 點選後由 [AppLocale] 存起來並重建 Activity，整頁以新語言重新繪製。
 */
@Composable
private fun LanguageSection() {
    val context = LocalContext.current
    val activity = context.findActivity()
    val current = remember(context) { AppLocale.preference(context) }
    SectionTitle(stringResource(R.string.settings_language))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            LanguagePreference.SYSTEM to stringResource(R.string.settings_language_system),
            LanguagePreference.ZH_TW to stringResource(R.string.language_name_zh_tw),
            LanguagePreference.EN to stringResource(R.string.language_name_en),
        ).forEach { (preference, label) ->
            FilterChip(
                selected = current == preference,
                onClick = { activity?.let { AppLocale.setPreference(it, preference) } },
                label = { Text(label) },
                // 繁中介面裡「外觀」也有一個「跟隨系統」，E2E-14 用 tag 分辨
                modifier = Modifier.testTag("language-${preference.storageValue}"),
            )
        }
    }
}

/** UI-SPEC.md §10.1。整列是一個開關，TalkBack 唸成「協助改善 App，開關，已關閉」。 */
@Composable
private fun PrivacySection(viewModel: PrivacyAndSupportViewModel) {
    val enabled by viewModel.analyticsEnabled.collectAsState()
    val typography = LocalAppTypography.current
    SectionTitle(stringResource(R.string.settings_privacy))
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_analytics_title), style = typography.rowTitle) },
        supportingContent = {
            Text(
                stringResource(R.string.settings_analytics_subtitle),
                style = typography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { Switch(checked = enabled, onCheckedChange = null) },
        modifier =
            Modifier
                .testTag("analytics-consent")
                .toggleable(value = enabled, role = Role.Switch, onValueChange = viewModel::setAnalyticsEnabled),
    )
}

@Composable
private fun BackupSection(viewModel: SettingsViewModel) {
    val backup by viewModel.backup.collectAsState()
    val isBackupDue by viewModel.isBackupDue.collectAsState()
    val lastBackupAt by viewModel.lastBackupAt.collectAsState()
    val typography = LocalAppTypography.current
    val confirmPhrase = stringResource(R.string.settings_replace_confirm_phrase)

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

    SectionTitle(stringResource(R.string.settings_backup_section))

    if (isBackupDue) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            Text(
                stringResource(R.string.settings_backup_due),
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
    ) { Text(stringResource(R.string.settings_export)) }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Text(
        stringResource(R.string.settings_import_section),
        style = typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )

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
                Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    if (backup.importMode == ImportMode.REPLACE) {
        val focusManager = LocalFocusManager.current
        OutlinedTextField(
            value = backup.confirmText,
            onValueChange = viewModel::setConfirmText,
            label = { Text(stringResource(R.string.settings_import_confirm_label, confirmPhrase)) },
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
            if (viewModel.canStartImport(
                    confirmPhrase,
                )
            ) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
        },
        enabled = !backup.isWorking,
        modifier = Modifier.fillMaxWidth(),
    ) { Text(stringResource(R.string.settings_import_choose_file)) }

    if (backup.isWorking) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
    }

    NotificationRow()
    Spacer(Modifier.height(24.dp))
}

/**
 * UI-SPEC.md §10.2。展開狀態用 `remember` 而不是 `rememberSaveable`：規格要求每次進入設定頁
 * 都是收合的，離開這個畫面（composable 離開 composition）就該忘掉。
 */
@Composable
private fun DonationSection(viewModel: PrivacyAndSupportViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val expandedLabel = stringResource(R.string.settings_support_expanded)
    val collapsedLabel = stringResource(R.string.settings_support_collapsed)
    val offers by viewModel.donationOffers.collectAsState()
    val typography = LocalAppTypography.current
    val activity = LocalContext.current.findActivity()

    HorizontalDivider()
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_support_title), style = typography.rowTitle) },
        trailingContent = {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier =
            Modifier
                .heightIn(min = 48.dp)
                .semantics { stateDescription = if (expanded) expandedLabel else collapsedLabel }
                .clickable(role = Role.Button) {
                    expanded = !expanded
                    if (expanded) viewModel.onDonationSectionOpened()
                },
    )
    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.settings_support_body),
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            when (val current = offers) {
                DonationOffers.Unavailable ->
                    Text(
                        stringResource(R.string.donation_unavailable),
                        style = typography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                else ->
                    donationTierLabels.forEach { (tier, label) ->
                        val price = (current as? DonationOffers.Available)?.prices?.get(tier)
                        FilledTonalButton(
                            onClick = { activity?.let { viewModel.donate(it, tier) } },
                            enabled = price != null && activity != null,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            Text(stringResource(label), modifier = Modifier.weight(1f))
                            Text(price ?: "—")
                        }
                    }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

private val donationTierLabels =
    listOf(
        DonationTier.SMALL to R.string.donation_small,
        DonationTier.MEDIUM to R.string.donation_medium,
        DonationTier.LARGE to R.string.donation_large,
    )

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
            stringResource(R.string.settings_notifications_off),
            style = LocalAppTypography.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = enableNotifications) { Text(stringResource(R.string.settings_notifications_enable)) }
    }
}

@Composable
private fun lastBackupText(lastBackupAt: Instant?): String {
    if (lastBackupAt == null) return stringResource(R.string.settings_never_backed_up)
    val t = lastBackupAt.toLocalDateTime(TimeZone.currentSystemDefault())
    val minute = t.minute.toString().padStart(2, '0')
    return stringResource(R.string.settings_last_backup, "${t.year}/${t.monthNumber}/${t.dayOfMonth} ${t.hour}:$minute")
}

private val weekStartDayOptions =
    listOf(
        DayOfWeek.SUNDAY to R.string.weekday_sun,
        DayOfWeek.MONDAY to R.string.weekday_mon,
        DayOfWeek.TUESDAY to R.string.weekday_tue,
        DayOfWeek.WEDNESDAY to R.string.weekday_wed,
        DayOfWeek.THURSDAY to R.string.weekday_thu,
        DayOfWeek.FRIDAY to R.string.weekday_fri,
        DayOfWeek.SATURDAY to R.string.weekday_sat,
    )

private val themeOptions =
    listOf(
        Theme.SYSTEM to R.string.theme_system,
        Theme.LIGHT to R.string.theme_light,
        Theme.DARK to R.string.theme_dark,
    )

/** 標籤文字跟網頁版 `Settings.tsx` 相同。 */
private val importModeOptions =
    listOf(
        ImportMode.MERGE to R.string.import_mode_merge,
        ImportMode.REPLACE to R.string.import_mode_replace,
    )
