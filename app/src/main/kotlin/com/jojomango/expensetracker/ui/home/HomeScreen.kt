@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Budget
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.DaySections
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.domain.WeekEntry
import com.jojomango.expensetracker.domain.categoryDisplayName
import com.jojomango.expensetracker.domain.colorOf
import com.jojomango.expensetracker.domain.majorDigitsToMinorUnits
import com.jojomango.expensetracker.ui.common.AmountKeyboardOptions
import com.jojomango.expensetracker.ui.common.CurrencyCodeKeyboardOptions
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.i18n.dayTitleLabels
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.i18n.weekTitleLabels
import com.jojomango.expensetracker.ui.notification.AutoRequestNotificationPermissionOnce
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchWalletRequested: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categoryNames = rememberCategoryNames()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
        } else if (state.needsOnboarding) {
            FirstWalletOnboarding(
                modifier = Modifier.padding(padding),
                onCreateWallet = { name, currency, budgetMode, amount ->
                    viewModel.createFirstWallet(name, currency, budgetMode, amount)
                },
                onRestoreFromBackup = onOpenSettings,
            )
        } else if (state.currentWallet != null) {
            AutoRequestNotificationPermissionOnce()
            HomeContent(
                state = state,
                padding = padding,
                onAddTransaction = onAddTransaction,
                onEditTransaction = onEditTransaction,
                onOpenSettings = onOpenSettings,
                onSwitchWalletRequested = onSwitchWalletRequested,
                onDelete = { transaction ->
                    viewModel.deleteTransaction(transaction)
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message =
                                    context.getString(
                                        R.string.home_deleted_transaction,
                                        categoryNameOf(state, transaction, categoryNames),
                                        formatAmount(state, transaction),
                                    ),
                                actionLabel = context.getString(R.string.common_undo),
                                duration = SnackbarDuration.Short,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.undoDeleteTransaction(transaction)
                        }
                    }
                },
            )
        }
    }
}

private fun categoryNameOf(
    state: HomeUiState,
    transaction: Transaction,
    names: CategoryNames,
): String = categoryDisplayName(transaction.categoryId, state.categories, names)

private fun formatAmount(
    state: HomeUiState,
    transaction: Transaction,
): String {
    val currency = state.currentWallet?.currency ?: return transaction.amount.toString()
    return Money.of(transaction.amount, currency).format()
}

@Composable
internal fun HomeContent(
    state: HomeUiState,
    padding: PaddingValues,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchWalletRequested: () -> Unit,
    onDelete: (Transaction) -> Unit,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) {
    val wallet = state.currentWallet ?: return
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
    ) {
        // §4.1 標題區
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .clickable(onClick = onSwitchWalletRequested, role = Role.Button)
                        .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(wallet.name, style = typography.navTitle)
                Icon(Icons.Filled.ArrowDropDown, contentDescription = stringResource(R.string.home_switch_wallet))
            }
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.home_settings))
            }
        }

        val context = LocalContext.current
        val weekLabels = weekTitleLabels(context)
        val dayLabels = dayTitleLabels(context)
        val categoryNames = rememberCategoryNames()
        val weeks =
            remember(state.transactionGroups, today) {
                DaySections.entriesOf(state.transactionGroups, today) { it.date }
            }
        // 預算卡是列表第一項、隨列表捲走；標題區留在上面（UI-SPEC §4.2 v2.7、UX-006）。
        // 底部多留 80dp，捲到底時最後一列不被 FAB 與底部導覽蓋住。
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
            item(key = "budget-card") {
                // §4.2 預算卡
                BudgetCard(state = state, wallet = wallet)
                Spacer(Modifier.height(12.dp))
            }
            if (state.transactionGroups.isEmpty()) {
                item(key = "empty") { EmptyTransactionsState(onAddTransaction) }
            }
            // §4.3 交易列表：週標題 → 每天一段（日小標題＋日卡片），跨年時插年份分隔
            state.transactionGroups.zip(weeks).forEach { (group, entries) ->
                item(key = "week-${group.range.start}") {
                    SubtotalHeader(
                        title = Week.groupTitle(group.range.start, state.weekStartDay, today, weekLabels),
                        net = Budget.netTotal(group.items, wallet.currency),
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
                entries.forEach { entry ->
                    when (entry) {
                        is WeekEntry.Year -> item(key = "year-${entry.year}") { YearSeparator(entry.year) }
                        is WeekEntry.Day -> {
                            item(key = "day-${entry.date}") {
                                DayHeader(
                                    title = DaySections.dayTitle(entry.date, today, dayLabels),
                                    net = Budget.netTotal(entry.items, wallet.currency),
                                )
                            }
                            item(key = "day-card-${entry.date}") {
                                Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    Column {
                                        entry.items.forEachIndexed { index, transaction ->
                                            // 左滑狀態要跟著交易走，不是跟著位置走：沒有 key 時，刪掉一列後
                                            // 補上來的那筆會繼承「已滑開」的狀態，畫面上只剩垃圾桶背景（空白列）。
                                            key(transaction.id) {
                                                TransactionRow(
                                                    transaction = transaction,
                                                    wallet = wallet,
                                                    categoryName = categoryNameOf(state, transaction, categoryNames),
                                                    categoryIcon =
                                                        state.categories.firstOrNull { it.id == transaction.categoryId }?.icon ?: "📦",
                                                    categoryColor = colorOf(transaction.categoryId, state.categories),
                                                    onClick = { onEditTransaction(transaction.id) },
                                                    onDelete = { onDelete(transaction) },
                                                )
                                            }
                                            if (index != entry.items.lastIndex) {
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 週標題與日小標題共用：左文字、右淨額；淨收入用收入色，其他用次要文字色（UI-SPEC §4.3 v2.6／v2.7）。 */
@Composable
private fun SubtotalHeader(
    title: String,
    net: Money,
    modifier: Modifier = Modifier,
) {
    val typography = LocalAppTypography.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = typography.label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            net.formatSigned(),
            style = typography.label,
            color = if (net.amount > 0) LocalAppExtraColors.current.income else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 日小標題：TalkBack 用標題導覽可以在各天之間跳，左右兩段合成一句唸（UX-006）。 */
@Composable
private fun DayHeader(
    title: String,
    net: Money,
) {
    val description = stringResource(R.string.home_day_header_desc, title, net.formatSigned())
    SubtotalHeader(
        title = title,
        net = net,
        modifier =
            Modifier
                .height(28.dp)
                .clearAndSetSemantics {
                    heading()
                    contentDescription = description
                },
    )
}

/** 跨年時的年份分隔：不可點、不顯示年度合計（UI-SPEC §4.3 v2.7）。 */
@Composable
private fun YearSeparator(year: Int) {
    val description = stringResource(R.string.home_year_separator_desc, year)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clearAndSetSemantics {
                    heading()
                    contentDescription = description
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            year.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    wallet: Wallet,
    categoryName: String,
    categoryIcon: String,
    categoryColor: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val deleteLabel = stringResource(R.string.common_delete)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        // E2E 用穩定的 tag 定位這一列，不用螢幕座標百分比——座標會隨裝置解析度與版面高度
        // 改變（TD-2：API 36 的 1080×2400 emulator 上真的滑錯列）。
        // TalkBack 開著時左滑手勢不會觸發，補一個走同一條刪除路徑的自訂動作（UI-SPEC §4.3 v2.7、UX-007）
        modifier =
            Modifier
                .testTag("transaction-row")
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete().let { true } })
                },
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onClick, role = Role.Button)
                    .padding(vertical = 11.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val parsedColor = runCatching { Color(android.graphics.Color.parseColor(categoryColor)) }.getOrDefault(extraColors.fg3)
            Box(
                modifier =
                    Modifier
                        .size(38.dp)
                        .background(
                            color = parsedColor.copy(alpha = LocalAppExtraColors.current.categoryTintAlpha),
                            shape = RoundedCornerShape(12.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(categoryIcon)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, style = typography.rowTitle)
                // 日期已經在日小標題，副標只放備註；沒有備註就只有一行（UI-SPEC §4.3 v2.7、UX-006）
                transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        note,
                        style = typography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            val isExpense = transaction.type == TransactionType.EXPENSE
            val sign = if (isExpense) "-" else "+"
            val color = if (isExpense) MaterialTheme.colorScheme.onSurface else extraColors.income
            Text(sign + Money.of(transaction.amount, wallet.currency).format(), style = typography.rowAmount, color = color)
        }
    }
}

@Composable
private fun EmptyTransactionsState(onAddTransaction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📝", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_empty))
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onAddTransaction) {
            Text(stringResource(R.string.home_add_first))
        }
    }
}

@Composable
private fun FirstWalletOnboarding(
    onCreateWallet: (name: String, currency: String, budgetMode: BudgetMode, amount: Long?) -> Unit,
    onRestoreFromBackup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("TWD") }
    var budgetMode by remember { mutableStateOf(BudgetMode.WEEKLY) }
    var amountText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.wallet_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase() },
            label = { Text(stringResource(R.string.wallet_currency_label)) },
            keyboardOptions = CurrencyCodeKeyboardOptions,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.wallet_budget_mode), style = MaterialTheme.typography.labelLarge)
        Row {
            listOf(
                BudgetMode.WEEKLY to stringResource(R.string.budget_mode_weekly),
                BudgetMode.TOTAL to stringResource(R.string.budget_mode_total),
                BudgetMode.NONE to stringResource(R.string.budget_mode_none),
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = budgetMode == mode,
                    onClick = { budgetMode = mode },
                    label = { Text(label) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (budgetMode != BudgetMode.NONE) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                label = { Text(stringResource(R.string.wallet_budget_amount)) },
                keyboardOptions = AmountKeyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        val decimalDigits = Currencies.builtIn[currency]?.decimalDigits ?: 2
        val amountMinorUnits = if (amountText.isEmpty()) null else majorDigitsToMinorUnits(amountText, decimalDigits)
        Button(
            onClick = {
                onCreateWallet(
                    name,
                    currency,
                    budgetMode,
                    if (budgetMode == BudgetMode.NONE) null else (amountMinorUnits ?: 0L),
                )
            },
            enabled = name.isNotBlank() && Currencies.builtIn.containsKey(currency),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.onboarding_create))
        }

        // 換新裝置的使用者第一步是還原備份，不該被迫先建一個用不到的錢包（SPEC.md §3.6）。
        // 網頁版引導頁也保留了「設定」入口，理由相同。
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onRestoreFromBackup, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_restore))
        }
    }
}
