@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.DaySections
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.MultiCurrency
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.domain.WeekEntry
import com.jojomango.expensetracker.domain.categoryDisplayName
import com.jojomango.expensetracker.domain.colorOf
import com.jojomango.expensetracker.ui.i18n.dayTitleLabels
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.i18n.weekTitleLabels
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography
import kotlinx.datetime.LocalDate

internal fun categoryNameOf(
    state: HomeUiState,
    transaction: Transaction,
    names: CategoryNames,
): String = categoryDisplayName(transaction.categoryId, state.categories, names)

/**
 * 預算卡＋交易列表。首頁與封存錢包檢視頁（UI-SPEC.md §11.1）共用；檢視頁是唯讀的，
 * 兩個回呼都傳 null——交易列就不能點、不能左滑，也沒有 TalkBack 的刪除動作。
 */
@Composable
internal fun WalletTransactionList(
    state: HomeUiState,
    today: LocalDate,
    onEditTransaction: ((String) -> Unit)?,
    onDelete: ((Transaction) -> Unit)?,
    emptyContent: @Composable () -> Unit,
) {
    val wallet = state.currentWallet ?: return
    val context = LocalContext.current
    val weekLabels = weekTitleLabels(context)
    val dayLabels = dayTitleLabels(context)
    val categoryNames = rememberCategoryNames()
    val weeks =
        remember(state.transactionGroups, today) {
            DaySections.entriesOf(state.transactionGroups, today) { it.date }
        }
    // 預算卡是列表第一項、隨列表捲走；標題區留在上面（UI-SPEC §4.2 v2.7、UX-006）。
    // 底部多留 80dp，捲到底時最後一列不被 FAB 與底部導覽蓋住。
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp)) {
        item(key = "budget-card") {
            // §4.2 預算卡
            BudgetCard(state = state, wallet = wallet)
            Spacer(Modifier.height(12.dp))
        }
        if (state.transactionGroups.isEmpty()) {
            item(key = "empty") { emptyContent() }
        }
        // §4.3 交易列表：週標題 → 每天一段（日小標題＋日卡片），跨年時插年份分隔
        state.transactionGroups.zip(weeks).forEach { (group, entries) ->
            item(key = "week-${group.range.start}") {
                SubtotalHeader(
                    title = Week.groupTitle(group.range.start, state.weekStartDay, today, weekLabels),
                    nets = MultiCurrency.netTotalsByCurrency(group.items, wallet.currency),
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            entries.forEach { entry ->
                when (entry) {
                    is WeekEntry.Year -> item(key = "year-${entry.year}") { YearSeparator(entry.year) }
                    is WeekEntry.Day -> {
                        item(key = "day-${entry.date}") {
                            DayHeader(
                                title = DaySections.dayTitle(entry.date, today, dayLabels),
                                nets = MultiCurrency.netTotalsByCurrency(entry.items, wallet.currency),
                            )
                        }
                        item(key = "day-card-${entry.date}") {
                            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                Column {
                                    entry.items.forEachIndexed { index, transaction ->
                                        // 左滑狀態要跟著交易走，不是跟著位置走：沒有 key 時，刪掉一列後
                                        // 補上來的那筆會繼承「已滑開」的狀態，畫面上只剩垃圾桶背景（空白列）。
                                        key(transaction.id) {
                                            TransactionRow(
                                                transaction = transaction,
                                                categoryName = categoryNameOf(state, transaction, categoryNames),
                                                categoryIcon =
                                                    state.categories.firstOrNull { it.id == transaction.categoryId }?.icon ?: "📦",
                                                categoryColor = colorOf(transaction.categoryId, state.categories),
                                                onClick = onEditTransaction?.let { edit -> { edit(transaction.id) } },
                                                onDelete = onDelete?.let { delete -> { delete(transaction) } },
                                            )
                                        }
                                        if (index != entry.items.lastIndex) {
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 週標題與日小標題共用：左文字、右淨額；淨收入用收入色，其他用次要文字色（UI-SPEC §4.3 v2.6／v2.7）。
 * 多種幣別時各算一段並排，每段各自決定顏色；放不下就換行、靠右，不截斷（UI-SPEC §15.3）。
 */
@Composable
private fun SubtotalHeader(
    title: String,
    nets: List<Money>,
    modifier: Modifier = Modifier,
) {
    val typography = LocalAppTypography.current
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    val income = LocalAppExtraColors.current.income
    val separator = stringResource(R.string.currency_separator)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = typography.label, color = neutral)
        FlowRow(modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp), horizontalArrangement = Arrangement.End) {
            nets.forEachIndexed { index, net ->
                Text(
                    buildAnnotatedString {
                        if (index > 0) withStyle(SpanStyle(color = neutral)) { append(separator) }
                        withStyle(SpanStyle(color = if (net.amount > 0) income else neutral)) { append(net.formatSigned()) }
                    },
                    style = typography.label,
                )
            }
        }
    }
}

/** 日小標題：TalkBack 用標題導覽可以在各天之間跳，左右兩段合成一句唸（UX-006）。 */
@Composable
private fun DayHeader(
    title: String,
    nets: List<Money>,
) {
    val amounts = nets.joinToString(stringResource(R.string.currency_separator)) { it.formatSigned() }
    val description = stringResource(R.string.home_day_header_desc, title, amounts)
    SubtotalHeader(
        title = title,
        nets = nets,
        modifier =
            Modifier
                .heightIn(min = 28.dp)
                .clearAndSetSemantics {
                    heading()
                    contentDescription = description
                },
    )
}

/** 跨年時的年份分隔：不可點、不顯示年度合計（UI-SPEC §4.3 v2.7）。 */
@Composable
private fun YearSeparator(year: Int) {
    val description = stringResource(R.string.home_year_separator_desc, year)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clearAndSetSemantics {
                    heading()
                    contentDescription = description
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            year.toString(),
            fontSize = 12.sp,
            fontWeight = FontWeight.W700,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/** [onClick]／[onDelete] 為 null 時是唯讀列（封存錢包檢視頁，UI-SPEC.md §11.1）：不能點、不能左滑。 */
@Composable
private fun TransactionRow(
    transaction: Transaction,
    categoryName: String,
    categoryIcon: String,
    categoryColor: String,
    onClick: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = {
        TransactionRowContent(transaction, categoryName, categoryIcon, categoryColor, onClick)
    }
    if (onDelete == null) {
        Box(modifier = Modifier.testTag("transaction-row")) { content() }
    } else {
        SwipeToDeleteRow(onDelete = onDelete, content = content)
    }
}

@Composable
private fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val deleteLabel = stringResource(R.string.common_delete)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        // E2E 用穩定的 tag 定位這一列，不用螢幕座標百分比——座標會隨裝置解析度與版面高度
        // 改變（TD-2：API 36 的 1080×2400 emulator 上真的滑錯列）。
        // TalkBack 開著時左滑手勢不會觸發，補一個走同一條刪除路徑的自訂動作（UI-SPEC §4.3 v2.7、UX-007）
        modifier =
            Modifier
                .testTag("transaction-row")
                .semantics {
                    customActions = listOf(CustomAccessibilityAction(deleteLabel) { onDelete().let { true } })
                },
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        content()
    }
}

@Composable
private fun TransactionRowContent(
    transaction: Transaction,
    categoryName: String,
    categoryIcon: String,
    categoryColor: String,
    onClick: (() -> Unit)?,
) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick, role = Role.Button) else Modifier)
                .padding(vertical = 11.dp, horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val parsedColor = runCatching { Color(android.graphics.Color.parseColor(categoryColor)) }.getOrDefault(extraColors.fg3)
        Box(
            modifier =
                Modifier
                    .size(38.dp)
                    .background(
                        color = parsedColor.copy(alpha = LocalAppExtraColors.current.categoryTintAlpha),
                        shape = RoundedCornerShape(12.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Text(categoryIcon)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(categoryName, style = typography.rowTitle)
            // 日期已經在日小標題，副標只放備註；沒有備註就只有一行（UI-SPEC §4.3 v2.7、UX-006）
            transaction.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    note,
                    style = typography.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val isExpense = transaction.type == TransactionType.EXPENSE
        val sign = if (isExpense) "-" else "+"
        val color = if (isExpense) MaterialTheme.colorScheme.onSurface else extraColors.income
        Text(sign + Money.of(transaction.amount, transaction.currency).format(), style = typography.rowAmount, color = color)
    }
}
