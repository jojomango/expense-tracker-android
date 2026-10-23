@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** 新增／編輯錢包——UI-SPEC.md §7「管理錢包…」的入口沒有給完整畫面規格，
 * 這裡沿用首次啟動引導表單（`HomeScreen.FirstWalletOnboarding`）同樣的欄位跟風格，
 * 保持一致；幣別建立後不可改（domain/Wallet.kt 的規則），編輯時欄位停用。 */
@Composable
fun WalletEditScreen(
    onDone: (WalletEditResult) -> Unit,
    onCancel: () -> Unit,
    viewModel: WalletEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.result) {
        state.result?.let(onDone)
    }

    state.pendingDeleteTransactionCount?.let { count ->
        DeleteWalletDialog(
            walletName = state.name,
            transactionCount = count,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDelete,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.wallet_edit_title else R.string.wallet_new_title)) },
                navigationIcon = { TextButton(onClick = onCancel) { Text(stringResource(R.string.common_cancel)) } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.wallet_name_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.currency,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text(stringResource(R.string.wallet_currency_label)) },
                enabled = !state.isEditing,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.wallet_budget_mode))
            Row {
                listOf(
                    BudgetMode.WEEKLY to stringResource(R.string.budget_mode_weekly),
                    BudgetMode.TOTAL to stringResource(R.string.budget_mode_total),
                    BudgetMode.NONE to stringResource(R.string.budget_mode_none),
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = state.budgetMode == mode,
                        onClick = { viewModel.onBudgetModeChange(mode) },
                        label = { Text(label) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            if (state.budgetMode != BudgetMode.NONE) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.budgetAmountDigits,
                    onValueChange = viewModel::onBudgetAmountChange,
                    label = { Text(stringResource(R.string.wallet_budget_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isEditing) R.string.common_save else R.string.wallet_create))
            }

            if (state.isEditing && !state.archived) {
                ArchiveAndDeleteSection(
                    isLastActive = state.isLastActive,
                    onArchive = viewModel::archive,
                    onDelete = viewModel::requestDelete,
                )
            }
        }
    }
}

/** UI-SPEC.md §11：只有編輯既有錢包時才有；最後一個未封存的錢包兩個鍵都停用。 */
@Composable
private fun ArchiveAndDeleteSection(
    isLastActive: Boolean,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onArchive, enabled = !isLastActive) { Text(stringResource(R.string.wallet_archive)) }
    TextButton(
        onClick = onDelete,
        enabled = !isLastActive,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) { Text(stringResource(R.string.wallet_delete)) }
    if (isLastActive) {
        Text(
            stringResource(R.string.wallet_keep_one),
            style = LocalAppTypography.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun DeleteWalletDialog(
    walletName: String,
    transactionCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.wallet_delete_title, walletName)) },
        text = {
            Text(
                if (transactionCount == 0) {
                    stringResource(R.string.wallet_delete_no_transactions)
                } else {
                    pluralStringResource(R.plurals.wallet_delete_with_transactions, transactionCount, transactionCount)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.common_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}
