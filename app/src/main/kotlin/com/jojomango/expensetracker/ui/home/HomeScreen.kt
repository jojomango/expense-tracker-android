@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.majorDigitsToMinorUnits
import com.jojomango.expensetracker.ui.common.AmountKeyboardOptions
import com.jojomango.expensetracker.ui.common.CurrencyPickerField
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.notification.AutoRequestNotificationPermissionOnce
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import com.jojomango.expensetracker.ui.transaction.SavedTransactionSnackbar
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
    savedMessage: String? = null,
    onSavedMessageShown: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val categoryNames = rememberCategoryNames()
    SavedTransactionSnackbar(savedMessage, snackbarHostState, onSavedMessageShown)

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

private fun formatAmount(
    state: HomeUiState,
    transaction: Transaction,
): String {
    if (state.currentWallet == null) return transaction.amount.toString()
    return Money.of(transaction.amount, transaction.currency).format()
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

        WalletTransactionList(
            state = state,
            today = today,
            onEditTransaction = onEditTransaction,
            onDelete = onDelete,
            emptyContent = { EmptyTransactionsState(onAddTransaction) },
        )
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
        CurrencyPickerField(
            selected = currency,
            onSelect = { currency = it },
            enabled = true,
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
