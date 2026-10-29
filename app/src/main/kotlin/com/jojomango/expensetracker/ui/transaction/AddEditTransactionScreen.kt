@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.transaction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

/** UI-SPEC.md §5 記帳頁——自製數字鍵台、分類網格，不用系統輸入法／下拉選單。 */
@Composable
fun AddEditTransactionScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddEditTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current

    LaunchedEffect(state.submitted) {
        if (state.submitted) onDone()
    }

    Scaffold { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        // imePadding：系統鍵盤出現時整頁縮到鍵盤上方（UX-002，不依賴 adjustResize 的隱含行為）
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            // UI-SPEC §5 的壓縮順序：基準 360×760dp 的內容區放不下「兩列分類＋56dp 鍵台」時，
            // 鍵高降到 48dp、主鍵上下內距縮小，把高度讓給分類網格（實測 360×760dp 上差約 46dp）。
            val compact = maxHeight < COMPACT_CONTENT_HEIGHT
            // 系統鍵盤與自製數字鍵台不能同時佔用下半部：編輯備註（鍵盤開著）時收起數字鍵台（UI-SPEC §5、UX-002）。
            // 用鍵盤是否顯示而不是焦點來判斷：按返回鍵收鍵盤時焦點還在備註欄，數字鍵台也要回來。
            val editingNote = WindowInsets.isImeVisible
            val focusManager = LocalFocusManager.current
            // 備註聚焦時分類區用單列或摘要列，由鍵盤上方剩下的高度決定（UI-SPEC §5 第 3 點、UX-005）
            var lastNoteLayout by remember { mutableStateOf(NoteCategoryLayout.ROW) }
            val noteLayout = noteCategoryLayout(maxHeight, lastNoteLayout)
            SideEffect { lastNoteLayout = noteLayout }
            val tight = editingNote && noteLayout == NoteCategoryLayout.SUMMARY
            Column(modifier = Modifier.fillMaxSize()) {
                // 標題列
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (tight) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) }
                    SingleChoiceSegmentedButtonRow {
                        SegmentedButton(
                            selected = state.type == TransactionType.EXPENSE,
                            onClick = { viewModel.onSelectType(TransactionType.EXPENSE) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        ) { Text(stringResource(R.string.type_expense)) }
                        SegmentedButton(
                            selected = state.type == TransactionType.INCOME,
                            onClick = { viewModel.onSelectType(TransactionType.INCOME) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        ) { Text(stringResource(R.string.type_income)) }
                    }
                    Spacer(Modifier.size(48.dp))
                }

                // 金額區（上下內距 8dp：讓分類網格一次露出兩列，UI-SPEC §5、UX-001）。
                // 編輯備註時整個金額區可點：清除焦點、收起鍵盤，數字鍵台回來（UX-002）；
                // 鍵盤上方空間不足時改成單行，把高度讓給分類摘要列與備註欄（UX-005）
                AmountArea(
                    walletLabel = state.wallet?.let { "${it.name} · ${it.currency}" },
                    amountText = viewModel.formattedAmount(),
                    amountColor =
                        when {
                            state.amount == 0L -> extraColors.fg3
                            state.type == TransactionType.INCOME -> extraColors.income
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    editingNote = editingNote,
                    singleLine = tight,
                    onBackToKeypad = { focusManager.clearFocus() },
                )

                // 分類網格；編輯備註時改單列或摘要列（UX-005），多出來的高度留白，主鍵仍緊接在備註欄下方
                if (editingNote) {
                    NoteFocusedCategories(
                        layout = noteLayout,
                        categories = state.categoriesForType,
                        selectedCategoryId = state.selectedCategoryId,
                        onSelect = viewModel::onSelectCategory,
                        onShowGrid = { focusManager.clearFocus() },
                    )
                    Spacer(Modifier.weight(1f))
                } else {
                    CategoryGrid(
                        categories = state.categoriesForType,
                        selectedCategoryId = state.selectedCategoryId,
                        onSelect = viewModel::onSelectCategory,
                        modifier = Modifier.weight(1f),
                    )
                }

                DateAndNoteRow(
                    date = state.date,
                    note = state.note,
                    onSelectDate = viewModel::onSelectDate,
                    onNoteChange = viewModel::onNoteChange,
                    compact = tight,
                )

                AnimatedVisibility(visible = !editingNote) {
                    NumericKeypad(
                        onDigit = viewModel::onDigit,
                        onBackspace = viewModel::onBackspace,
                        keyHeight = if (compact) 48.dp else 56.dp,
                    )
                }

                Button(
                    onClick = viewModel::submit,
                    enabled = state.canSubmit,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = if (compact) 8.dp else 20.dp),
                ) {
                    Text(stringResource(if (state.isEditing) R.string.common_save else R.string.transaction_submit))
                }
            }
        }
    }
}

@Composable
private fun AmountArea(
    walletLabel: String?,
    amountText: String,
    amountColor: Color,
    editingNote: Boolean,
    singleLine: Boolean,
    onBackToKeypad: () -> Unit,
) {
    val typography = LocalAppTypography.current
    val backToKeypad = stringResource(R.string.transaction_back_to_keypad)
    val modifier =
        if (editingNote) {
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onBackToKeypad)
                .semantics { contentDescription = backToKeypad }
        } else {
            Modifier.fillMaxWidth()
        }
    val walletText: @Composable () -> Unit = {
        walletLabel?.let { Text(it, style = typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (singleLine) {
        Row(modifier = modifier.height(36.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            walletText()
            Spacer(Modifier.weight(1f))
            Text(amountText, style = typography.donutTotal, color = amountColor)
        }
    } else {
        Column(modifier = modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            walletText()
            Text(
                text = amountText,
                style = if (editingNote) typography.amountInput.copy(fontSize = 40.sp) else typography.amountInput,
                color = amountColor,
            )
        }
    }
}

/** 低於這個內容高度（扣掉系統列）就用緊湊尺寸；360×760dp 的手機內容區約 710dp。 */
private val COMPACT_CONTENT_HEIGHT = 740.dp

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        items(categories, key = { it.id }) { category ->
            CategoryCell(
                category = category,
                selected = category.id == selectedCategoryId,
                onClick = { onSelect(category.id) },
                modifier = Modifier.padding(6.dp),
            )
        }
    }
}

@Composable
internal fun DateAndNoteRow(
    date: LocalDate,
    note: String,
    onSelectDate: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    compact: Boolean = false,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(note.isNotEmpty()) }
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = if (compact) 4.dp else 8.dp)) {
        DatePill(label = stringResource(R.string.date_today), selected = date == today) { onSelectDate(today) }
        DatePill(label = stringResource(R.string.date_yesterday), selected = date == yesterday) { onSelectDate(yesterday) }
        // 只放日曆圖示：英文四個 pill 在 360dp 放不下一列；選了其他日期才在圖示旁顯示 M/D（UI-SPEC §5 第 4 點、UX-004）
        val otherDate = date != today && date != yesterday
        CalendarPill(
            label = if (otherDate) "${date.monthNumber}/${date.dayOfMonth}" else null,
            onClick = { showDatePicker = true },
        )
        Spacer(Modifier.weight(1f))
        DatePill(label = stringResource(R.string.transaction_add_note), selected = showNoteField) { showNoteField = !showNoteField }
    }
    if (showNoteField) {
        val focusManager = LocalFocusManager.current
        NoteField(
            value = note,
            onValueChange = onNoteChange,
            onDone = { focusManager.clearFocus() },
            height = if (compact) 48.dp else 56.dp,
        )
    }
    if (showDatePicker) {
        val initialMillis = date.atStartOfDayMillis()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onSelectDate(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.common_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.common_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun LocalDate.atStartOfDayMillis(): Long = this.atStartOfDayInstant().toEpochMilliseconds()

private fun LocalDate.atStartOfDayInstant(): Instant =
    kotlinx.datetime.LocalDateTime(this, kotlinx.datetime.LocalTime(0, 0)).toInstant(TimeZone.UTC)

@Composable
private fun DatePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = Modifier.padding(end = 6.dp).heightIn(min = 48.dp),
    )
}

/** 日曆 pill：未選其他日期時是 48×48dp 的方形圖示；選了之後顯示 M/D 並把它當成狀態唸出來（UX-004）。 */
@Composable
private fun CalendarPill(
    label: String?,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.date_pick)
    FilterChip(
        selected = label != null,
        onClick = onClick,
        label = {
            if (label != null) {
                Text(label)
            } else {
                Icon(Icons.Filled.CalendarMonth, contentDescription = description, modifier = Modifier.size(20.dp))
            }
        },
        leadingIcon =
            if (label != null) {
                { Icon(Icons.Filled.CalendarMonth, contentDescription = description, modifier = Modifier.size(18.dp)) }
            } else {
                null
            },
        modifier =
            Modifier
                .padding(end = 6.dp)
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .semantics { if (label != null) stateDescription = label },
    )
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    keyHeight: Dp,
) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "00", "0", "⌫")
    val extraColors = LocalAppExtraColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier =
            Modifier
                .fillMaxWidth()
                .background(extraColors.keypad)
                .padding(8.dp),
    ) {
        items(keys) { key ->
            Box(
                modifier =
                    Modifier
                        .padding(4.dp)
                        // 固定鍵高（UI-SPEC §2.4）：aspectRatio 會讓寬螢幕上的鍵台長得太高，把分類網格擠成一列
                        .fillMaxWidth()
                        .height(keyHeight)
                        .background(if (key == "⌫") Color.Transparent else extraColors.key, RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button) {
                            if (key == "⌫") onBackspace() else onDigit(key)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text(key, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
