@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.home.WalletTransactionList
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

/** UI-SPEC.md §11.1：從錢包管理頁「已封存」區塊點進來的唯讀檢視頁。 */
@Composable
fun ArchivedWalletScreen(
    onBack: () -> Unit,
    onDone: (WalletEditResult) -> Unit,
    viewModel: ArchivedWalletViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.result) {
        state.result?.let(onDone)
    }

    ArchivedWalletContent(
        state = state,
        onBack = onBack,
        onUnarchive = viewModel::unarchive,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
    )
}

@Composable
internal fun ArchivedWalletContent(
    state: ArchivedWalletUiState,
    onBack: () -> Unit,
    onUnarchive: () -> Unit,
    onRequestDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
) {
    val wallet = state.home.currentWallet

    state.pendingDeleteTransactionCount?.let { count ->
        DeleteWalletDialog(
            walletName = wallet?.name.orEmpty(),
            transactionCount = count,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(wallet?.name.orEmpty())
                        Text(
                            stringResource(R.string.wallet_archived_caption),
                            style = LocalAppTypography.current.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.common_back)) } },
                actions = { ArchivedWalletActions(onUnarchive = onUnarchive, onRequestDelete = onRequestDelete) },
            )
        },
    ) { padding ->
        if (wallet == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            WalletTransactionList(
                state = state.home,
                today = today,
                onEditTransaction = null,
                onDelete = null,
                emptyContent = {
                    Box(modifier = Modifier.fillMaxSize().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.archived_wallet_empty),
                            style = LocalAppTypography.current.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun ArchivedWalletActions(
    onUnarchive: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    IconButton(onClick = onUnarchive) {
        Icon(Icons.Outlined.Unarchive, contentDescription = stringResource(R.string.wallet_unarchive))
    }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.common_more_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.wallet_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(ACTION_ICON_SIZE),
                    )
                },
                onClick = {
                    menuOpen = false
                    onRequestDelete()
                },
            )
        }
    }
}
