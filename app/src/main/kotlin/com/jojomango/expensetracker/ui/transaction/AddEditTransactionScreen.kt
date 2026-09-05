@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.TransactionType
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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 標題列
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onCancel) { Text("取消") }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = state.type == TransactionType.EXPENSE,
                        onClick = { viewModel.onSelectType(TransactionType.EXPENSE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("支出") }
                    SegmentedButton(
                        selected = state.type == TransactionType.INCOME,
                        onClick = { viewModel.onSelectType(TransactionType.INCOME) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("收入") }
                }
                Spacer(Modifier.size(48.dp))
            }

            // 金額區
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                state.wallet?.let { wallet ->
                    Text("${wallet.name} · ${wallet.currency}", style = typography.caption, color = extraColors.fg3)
                }
                val amountColor =
                    when {
                        state.amount == 0L -> extraColors.fg3
                        state.type == TransactionType.INCOME -> extraColors.income
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                Text(
                    text = viewModel.formattedAmount(),
                    style = typography.amountInput,
                    color = amountColor,
                )
            }

            // 分類網格
            CategoryGrid(
                categories = state.categoriesForType,
                selectedCategoryId = state.selectedCategoryId,
                onSelect = viewModel::onSelectCategory,
                modifier = Modifier.weight(1f),
            )

            DateAndNoteRow(
                date = state.date,
                note = state.note,
                onSelectDate = viewModel::onSelectDate,
                onNoteChange = viewModel::onNoteChange,
            )

            NumericKeypad(onDigit = viewModel::onDigit, onBackspace = viewModel::onBackspace)

            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().padding(20.dp),
            ) {
                Text(if (state.isEditing) "儲存" else "記一筆")
            }
        }
    }
}

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
            val color = runCatching { Color(android.graphics.Color.parseColor(category.color)) }.getOrDefault(Color.Gray)
            val selected = category.id == selectedCategoryId
            Column(
                modifier = Modifier.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(50.dp)
                            .background(color.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .then(
                                if (selected) {
                                    Modifier.border(2.5.dp, color, RoundedCornerShape(16.dp))
                                } else {
                                    Modifier
                                },
                            ).clickable(role = Role.Button) { onSelect(category.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(category.icon, fontSize = 22.sp)
                }
                Text(category.name, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DateAndNoteRow(
    date: LocalDate,
    note: String,
    onSelectDate: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteField by remember { mutableStateOf(note.isNotEmpty()) }
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val yesterday = today.minus(1, DateTimeUnit.DAY)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        DatePill(label = "今天", selected = date == today) { onSelectDate(today) }
        DatePill(label = "昨天", selected = date == yesterday) { onSelectDate(yesterday) }
        DatePill(
            label = if (date != today && date != yesterday) "${date.monthNumber}/${date.dayOfMonth}" else "選日期",
            selected = date != today && date != yesterday,
        ) { showDatePicker = true }
        Spacer(Modifier.weight(1f))
        DatePill(label = "+備註", selected = showNoteField) { showNoteField = !showNoteField }
    }
    if (showNoteField) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("備註") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
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
                }) { Text("確定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
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
        modifier = Modifier.padding(end = 6.dp).heightIn(min = 36.dp),
    )
}

@Composable
private fun NumericKeypad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
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
                        .aspectRatio(1.6f)
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
