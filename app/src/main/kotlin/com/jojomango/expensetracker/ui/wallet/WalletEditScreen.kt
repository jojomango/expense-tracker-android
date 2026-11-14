@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.ui.common.AmountKeyboardOptions
import com.jojomango.expensetracker.ui.common.CurrencyPickerField
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
            CurrencyPickerField(
                selected = state.currency,
                onSelect = viewModel::onCurrencyChange,
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                labelRes = R.string.wallet_default_currency_label,
            )
            WalletCurrencyNote(isEditing = state.isEditing)
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
                BudgetAmountField(
                    digits = state.budgetAmountDigits,
                    currencyChanged = state.currencyChangedWithBudget,
                    onChange = viewModel::onBudgetAmountChange,
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

/** UI-SPEC.md §11 v2.14：封存／取消封存／刪除的圖示大小。 */
internal val ACTION_ICON_SIZE = 18.dp

/** 文字前加一個裝飾用圖示；無障礙只唸文字，所以圖示不給描述（UI-SPEC.md §11 v2.14）。 */
@Composable
internal fun IconLabel(
    icon: ImageVector,
    text: String,
) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ACTION_ICON_SIZE))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

/** UI-SPEC.md §11：只有編輯既有錢包時才有；最後一個未封存的錢包兩個鍵都停用。 */
@Composable
internal fun ArchiveAndDeleteSection(
    isLastActive: Boolean,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    Spacer(Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onArchive, enabled = !isLastActive) {
        IconLabel(Icons.Outlined.Archive, stringResource(R.string.wallet_archive))
    }
    TextButton(
        onClick = onDelete,
        enabled = !isLastActive,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) { IconLabel(Icons.Outlined.Delete, stringResource(R.string.wallet_delete)) }
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
internal fun DeleteWalletDialog(
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

/** 編輯時提醒：改預設幣別只影響之後的預設值，不會改寫已記錄的交易（UI-SPEC.md §15.1、T8.5.10）。 */
@Composable
internal fun WalletCurrencyNote(isEditing: Boolean) {
    if (!isEditing) return
    Text(
        stringResource(R.string.wallet_currency_change_note),
        style = LocalAppTypography.current.caption,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
    )
}

/** 預算金額欄；換了幣別時欄位已清空，下方提示重新輸入（UI-SPEC.md §15.1、T8.5.9）。 */
@Composable
internal fun BudgetAmountField(
    digits: String,
    currencyChanged: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = digits,
        onValueChange = onChange,
        label = { Text(stringResource(R.string.wallet_budget_amount)) },
        supportingText =
            if (currencyChanged) {
                { Text(stringResource(R.string.wallet_currency_changed_budget)) }
            } else {
                null
            },
        keyboardOptions = AmountKeyboardOptions,
        modifier = Modifier.fillMaxWidth(),
    )
}
