@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.BudgetMode

/** UI-SPEC.md §7「管理錢包…」導向的頁面——沒有給完整規格，這裡先做最小可用版本：
 * 列出所有未封存錢包，點一個進編輯，右上角「新增」開建立表單。 */
@Composable
fun WalletManagementScreen(
    onBack: () -> Unit,
    onAddWallet: () -> Unit,
    onEditWallet: (String) -> Unit,
    viewModel: WalletManagementViewModel = hiltViewModel(),
) {
    val wallets by viewModel.wallets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("管理錢包") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = onAddWallet) { Text("新增") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            wallets.forEachIndexed { index, wallet ->
                ListItem(
                    headlineContent = { Text(wallet.name) },
                    supportingContent = { Text("${wallet.currency} · ${budgetModeLabel(wallet.budgetMode)}") },
                    modifier = Modifier.clickable { onEditWallet(wallet.id) },
                )
                if (index != wallets.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

private fun budgetModeLabel(mode: BudgetMode): String =
    when (mode) {
        BudgetMode.WEEKLY -> "每週預算"
        BudgetMode.TOTAL -> "總預算"
        BudgetMode.NONE -> "不設定"
    }
