@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.ui.common.currencyName
import com.jojomango.expensetracker.ui.common.currencyOptionLabel
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

const val CURRENCY_SHEET_ROW_TAG = "currency-sheet-row"

/** 金額區上方的「{錢包名} · {幣別}」，{幣別} 可點開幣別 sheet（UI-SPEC.md §15.2）。 */
@Composable
internal fun CurrencyButton(
    walletName: String,
    currency: String,
    onClick: () -> Unit,
) {
    val typography = LocalAppTypography.current
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val description = stringResource(R.string.transaction_currency_desc, currencyName(LocalContext.current, currency))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$walletName · ", style = typography.caption, color = color)
        Row(
            modifier =
                Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onClick)
                    .clearAndSetSemantics {
                        contentDescription = description
                        role = Role.Button
                        onClick { onClick().let { true } }
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(currency, style = typography.caption, color = color)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = color)
        }
    }
}

/** 底部 sheet 而不是下拉選單，也不叫出系統鍵盤（UI-SPEC.md §15.2、§5 第 3 點）。 */
@Composable
internal fun CurrencySheet(
    codes: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        CurrencySheetList(codes = codes, selected = selected, onSelect = onSelect)
    }
}

@Composable
internal fun CurrencySheetList(
    codes: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    Column(modifier = Modifier.navigationBarsPadding()) {
        Text(
            stringResource(R.string.transaction_choose_currency),
            style = LocalAppTypography.current.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        LazyColumn {
            items(codes, key = { it }) { code ->
                val isSelected = code == selected
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = isSelected, role = Role.Button) { onSelect(code) }
                            .testTag(CURRENCY_SHEET_ROW_TAG)
                            .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(currencyOptionLabel(context, code), modifier = Modifier.weight(1f))
                    if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
