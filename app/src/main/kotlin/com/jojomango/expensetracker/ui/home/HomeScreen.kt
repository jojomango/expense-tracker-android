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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.BudgetStatus
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.TotalBudgetStatus
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.domain.colorOf
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.needsOnboarding) {
            FirstWalletOnboarding(
                modifier = Modifier.padding(padding),
                onCreateWallet = { name, currency, budgetMode, amount ->
                    viewModel.createFirstWallet(name, currency, budgetMode, amount)
                },
            )
        } else if (state.currentWallet != null) {
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
                                message = "已刪除 ${categoryNameOf(state, transaction)} ${formatAmount(state, transaction)}",
                                actionLabel = "還原",
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
): String = state.categories.firstOrNull { it.id == transaction.categoryId }?.name ?: "未分類"

private fun formatAmount(
    state: HomeUiState,
    transaction: Transaction,
): String {
    val currency = state.currentWallet?.currency ?: return transaction.amount.toString()
    return Money.of(transaction.amount, currency).format()
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    padding: PaddingValues,
    onAddTransaction: () -> Unit,
    onEditTransaction: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchWalletRequested: () -> Unit,
    onDelete: (Transaction) -> Unit,
) {
    val wallet = state.currentWallet ?: return
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

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
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "切換錢包")
            }
            TextButton(onClick = onOpenSettings) {
                Text("設定")
            }
        }

        // §4.2 預算卡
        BudgetCard(state = state, wallet = wallet)

        Spacer(Modifier.height(16.dp))

        // §4.3 交易列表
        if (state.transactionGroups.isEmpty()) {
            EmptyTransactionsState(onAddTransaction)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                state.transactionGroups.forEach { group ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        ) {
                            Text(
                                Week.groupTitle(group.range.start, state.weekStartDay, today),
                                style = typography.label,
                                color = extraColors.fg3,
                            )
                        }
                    }
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column {
                                group.items.forEachIndexed { index, transaction ->
                                    TransactionRow(
                                        transaction = transaction,
                                        wallet = wallet,
                                        categoryName = categoryNameOf(state, transaction),
                                        categoryIcon = state.categories.firstOrNull { it.id == transaction.categoryId }?.icon ?: "📦",
                                        categoryColor = colorOf(transaction.categoryId, state.categories),
                                        onClick = { onEditTransaction(transaction.id) },
                                        onDelete = { onDelete(transaction) },
                                    )
                                    if (index != group.items.lastIndex) {
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

@Composable
private fun BudgetCard(
    state: HomeUiState,
    wallet: Wallet,
) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            val label =
                when (wallet.budgetMode) {
                    BudgetMode.WEEKLY -> "本週還可以花"
                    BudgetMode.TOTAL -> "總預算還剩"
                    BudgetMode.NONE -> "本週支出"
                }
            Text(label, style = typography.label, color = extraColors.fg3)
            Spacer(Modifier.height(4.dp))

            val isOverBudget = state.weeklyBalance?.isOverBudget ?: state.totalBalance?.isOverBudget ?: false
            val amountText =
                when {
                    state.weeklyBalance != null -> state.weeklyBalance.balance.format()
                    state.totalBalance != null -> state.totalBalance.balance.format()
                    else -> state.weeklyExpenseTotal?.format() ?: "—"
                }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    amountText,
                    style = typography.balance,
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                if (isOverBudget) {
                    Spacer(Modifier.width(8.dp))
                    Text("已超支", style = typography.label, color = MaterialTheme.colorScheme.error)
                }
            }

            if (wallet.budgetMode != BudgetMode.NONE) {
                Spacer(Modifier.height(12.dp))
                val usedPercent = usedPercentOf(state, wallet)
                LinearProgressIndicator(
                    progress = { usedPercent.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
                    color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = extraColors.track,
                )
            }
        }
    }
}

private fun usedPercentOf(
    state: HomeUiState,
    wallet: Wallet,
): Double {
    val total: TotalBudgetStatus? = state.totalBalance
    val weekly: BudgetStatus? = state.weeklyBalance
    return when {
        total != null -> (total.usedPercent / 100.0).coerceIn(0.0, 1.0)
        weekly != null && wallet.budgetAmount != null && wallet.budgetAmount > 0 -> {
            val spent = wallet.budgetAmount - weekly.balance.amount
            (spent.toDouble() / wallet.budgetAmount.toDouble()).coerceIn(0.0, 1.0)
        }
        else -> 0.0
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
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "刪除", tint = MaterialTheme.colorScheme.error)
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
                        .background(color = parsedColor.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(categoryIcon)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(categoryName, style = typography.rowTitle)
                val subtitle =
                    "${transaction.date.monthNumber}/${transaction.date.dayOfMonth}" +
                        (transaction.note?.let { " · $it" } ?: "")
                Text(subtitle, style = typography.caption, color = extraColors.fg3, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text("這個錢包還沒有交易")
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(onClick = onAddTransaction) {
            Text("記第一筆")
        }
    }
}

@Composable
private fun FirstWalletOnboarding(
    modifier: Modifier = Modifier,
    onCreateWallet: (name: String, currency: String, budgetMode: BudgetMode, amount: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("TWD") }
    var budgetMode by remember { mutableStateOf(BudgetMode.WEEKLY) }
    var amountText by remember { mutableStateOf("") }

    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("建立你的第一個錢包", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("錢包名稱") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = currency,
            onValueChange = { currency = it.uppercase() },
            label = { Text("幣別（ISO 4217，如 TWD）") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text("預算模式", style = MaterialTheme.typography.labelLarge)
        Row {
            listOf(BudgetMode.WEEKLY to "每週", BudgetMode.TOTAL to "總預算", BudgetMode.NONE to "不設定").forEach { (mode, label) ->
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
                label = { Text("預算金額") },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
        val decimalDigits = Currencies.builtIn[currency]?.decimalDigits ?: 2
        val amountMinorUnits = amountText.toLongOrNull()?.let { it * pow10(decimalDigits) }
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
            Text("建立錢包")
        }
    }
}

private fun pow10(exponent: Int): Long {
    var result = 1L
    repeat(exponent) { result *= 10 }
    return result
}
