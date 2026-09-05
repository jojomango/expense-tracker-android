package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.BudgetStatus
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.TotalBudgetStatus
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.ui.theme.AppTypography
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** UI-SPEC.md §4.2 首頁預算卡——獨立成一個檔案，避免 HomeScreen.kt 塞進太多
 * function（detekt TooManyFunctions）。 */
@Composable
fun BudgetCard(
    state: HomeUiState,
    wallet: Wallet,
) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val isOverBudget = state.weeklyBalance?.isOverBudget ?: state.totalBalance?.isOverBudget ?: false

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

            BudgetAmountRow(state, isOverBudget, typography)

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

            // 「已用 X/Y · 還有 N 天」「日均可用」是週預算特有的概念（見
            // HomeViewModel.buildUiState 的說明），只在 WEEKLY 模式顯示。
            if (wallet.budgetMode == BudgetMode.WEEKLY && state.weeklyBalance != null && state.daysLeftInWeek != null) {
                WeeklyBudgetDetailRows(state, wallet, typography)
            }

            // SPEC.md §3.4「total 模式額外顯示：已用百分比」——跟週預算的
            // 「已用 X/Y · 還有 N 天」是不同的資訊（總預算沒有週期可言）。
            if (state.totalBalance != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "已用 ${Math.round(state.totalBalance.usedPercent)}%",
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetAmountRow(
    state: HomeUiState,
    isOverBudget: Boolean,
    typography: AppTypography,
) {
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
            // TESTCASES.md E2E-3 要求「警示色 + 圖示」，但 UI-SPEC.md §4.2 明講
            // 「不用 emoji 警示圖示」——這裡用 Material Icons 的向量圖示（不是
            // emoji 字元）滿足兩邊：icon 本身就是 UI-SPEC 說的「不用 emoji」，
            // 又滿足 TESTCASES 說的「+ 圖示」。
            Icon(
                Icons.Filled.Warning,
                contentDescription = "已超支",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("已超支", style = typography.label, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WeeklyBudgetDetailRows(
    state: HomeUiState,
    wallet: Wallet,
    typography: AppTypography,
) {
    val budgetText = Money.of(requireNotNull(wallet.budgetAmount), wallet.currency).format()
    val usedText = (state.weeklyExpenseTotal ?: Money.of(0, wallet.currency)).format()
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "已用 $usedText / $budgetText",
            style = typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "還有 ${state.daysLeftInWeek} 天",
            style = typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (state.dailyAllowance != null) {
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("日均可用", style = typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                state.dailyAllowance.format(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
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
