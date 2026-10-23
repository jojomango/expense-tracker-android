@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** UI-SPEC.md §7「管理錢包…」導向的頁面：未封存的錢包可以點進去編輯；有封存的錢包時，
 * 下方多一個「已封存」區塊可以取消封存（UI-SPEC.md §11）。 */
@Composable
fun WalletManagementScreen(
    onBack: () -> Unit,
    onAddWallet: () -> Unit,
    onEditWallet: (String) -> Unit,
    message: String?,
    onMessageShown: () -> Unit,
    viewModel: WalletManagementViewModel = hiltViewModel(),
) {
    val activeWallets by viewModel.activeWallets.collectAsState()
    val archivedWallets by viewModel.archivedWallets.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        // 一定要等 Snackbar 結束才清掉訊息：先清的話 message（這個 LaunchedEffect 的 key）
        // 會變成 null，Compose 會立刻取消這個 effect，Snackbar 一出現就被收掉。
        try {
            snackbarHostState.showSnackbar(text)
        } finally {
            onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wallet_management_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } },
                actions = { TextButton(onClick = onAddWallet) { Text(stringResource(R.string.common_add)) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            activeWallets.forEachIndexed { index, wallet ->
                ListItem(
                    headlineContent = { Text(wallet.name) },
                    supportingContent = { Text("${wallet.currency} · ${budgetModeLabel(wallet.budgetMode)}") },
                    modifier = Modifier.clickable { onEditWallet(wallet.id) },
                )
                if (index != activeWallets.lastIndex) {
                    HorizontalDivider()
                }
            }
            if (archivedWallets.isNotEmpty()) {
                Text(
                    stringResource(R.string.wallet_archived_section),
                    style = LocalAppTypography.current.cardTitle,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
                )
                archivedWallets.forEach { wallet ->
                    ListItem(
                        headlineContent = { Text(wallet.name, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        trailingContent = {
                            TextButton(onClick = { viewModel.unarchive(wallet.id) }) { Text(stringResource(R.string.wallet_unarchive)) }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun budgetModeLabel(mode: BudgetMode): String =
    stringResource(
        when (mode) {
            BudgetMode.WEEKLY -> R.string.budget_mode_weekly_label
            BudgetMode.TOTAL -> R.string.budget_mode_total
            BudgetMode.NONE -> R.string.budget_mode_none
        },
    )
