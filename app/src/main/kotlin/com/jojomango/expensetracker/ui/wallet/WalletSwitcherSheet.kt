@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** UI-SPEC.md §7 — 錢包切換。 */
@Composable
fun WalletSwitcherSheet(
    wallets: List<Wallet>,
    currentWalletId: String?,
    onSelectWallet: (String) -> Unit,
    onManageWallets: () -> Unit,
    onDismiss: () -> Unit,
) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                "切換錢包",
                style = typography.caption,
                color = extraColors.fg3,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            wallets.filterNot { it.archived }.forEach { wallet ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onSelectWallet(wallet.id) }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(wallet.name)
                        Text(wallet.currency, style = typography.caption, color = extraColors.fg3)
                    }
                    if (wallet.id == currentWalletId) {
                        Icon(Icons.Filled.Check, contentDescription = "目前錢包", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider()
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onManageWallets)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("管理錢包…", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
