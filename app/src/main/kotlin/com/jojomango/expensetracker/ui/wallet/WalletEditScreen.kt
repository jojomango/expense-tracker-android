@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.BudgetMode

/** 新增／編輯錢包——UI-SPEC.md §7「管理錢包…」的入口沒有給完整畫面規格，
 * 這裡沿用首次啟動引導表單（`HomeScreen.FirstWalletOnboarding`）同樣的欄位跟風格，
 * 保持一致；幣別建立後不可改（domain/Wallet.kt 的規則），編輯時欄位停用。 */
@Composable
fun WalletEditScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: WalletEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "編輯錢包" else "新增錢包") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("錢包名稱") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.currency,
                onValueChange = viewModel::onCurrencyChange,
                label = { Text("幣別（ISO 4217，如 TWD）") },
                enabled = !state.isEditing,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("預算模式")
            Row {
                listOf(BudgetMode.WEEKLY to "每週", BudgetMode.TOTAL to "總預算", BudgetMode.NONE to "不設定").forEach { (mode, label) ->
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
                    label = { Text("預算金額") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditing) "儲存" else "建立錢包")
            }
        }
    }
}
