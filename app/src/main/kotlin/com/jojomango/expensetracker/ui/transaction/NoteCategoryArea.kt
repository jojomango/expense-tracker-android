package com.jojomango.expensetracker.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.displayName
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** 備註欄聚焦時分類區的兩種版面（UI-SPEC §5 第 3 點、UX-005）。 */
internal enum class NoteCategoryLayout { ROW, SUMMARY }

/**
 * 單列版面需要的內容高度（鍵盤上方、扣掉狀態列）。在 360×760dp emulator 上量：標題列 64、金額區 82、
 * 單列 80、日期列 64、備註欄 64、主鍵 56，合計約 410dp；取 400dp 再配上緩衝。
 */
internal val NOTE_ROW_MIN_HEIGHT = 400.dp

/** 鍵盤開關動畫中可用高度會一路變化，沒有緩衝會在門檻附近來回切換版面。 */
internal val NOTE_LAYOUT_HYSTERESIS = 12.dp

internal fun noteCategoryLayout(
    available: Dp,
    previous: NoteCategoryLayout,
): NoteCategoryLayout =
    when {
        available < NOTE_ROW_MIN_HEIGHT - NOTE_LAYOUT_HYSTERESIS -> NoteCategoryLayout.SUMMARY
        available > NOTE_ROW_MIN_HEIGHT + NOTE_LAYOUT_HYSTERESIS -> NoteCategoryLayout.ROW
        else -> previous
    }

@Composable
internal fun NoteFocusedCategories(
    layout: NoteCategoryLayout,
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
    onShowGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (layout) {
        NoteCategoryLayout.ROW -> CategoryRow(categories, selectedCategoryId, onSelect, modifier)
        NoteCategoryLayout.SUMMARY ->
            CategorySummary(categories.firstOrNull { it.id == selectedCategoryId }, onShowGrid, modifier)
    }
}

@Composable
private fun CategoryRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val listState = rememberLazyListState()
    // 只在選中的分類不在可見範圍時才捲，不要每次聚焦都跳回最前面
    LaunchedEffect(Unit) {
        val index = categories.indexOfFirst { it.id == selectedCategoryId }
        val visible = listState.layoutInfo.visibleItemsInfo
        val fullyVisible =
            visible.any {
                it.index == index &&
                    it.offset >= 0 &&
                    it.offset + it.size <= listState.layoutInfo.viewportEndOffset
            }
        if (index >= 0 && !fullyVisible) listState.animateScrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, c -> c.id }) { _, category ->
            CategoryCell(
                category = category,
                selected = category.id == selectedCategoryId,
                onClick = { onSelect(category.id) },
                modifier = Modifier.width(68.dp),
            )
        }
    }
}

@Composable
private fun CategorySummary(
    selected: Category?,
    onShowGrid: () -> Unit,
    modifier: Modifier,
) {
    val typography = LocalAppTypography.current
    val name = selected?.displayName(rememberCategoryNames())
    val description =
        if (name != null) {
            stringResource(R.string.transaction_category_change_desc, name)
        } else {
            stringResource(R.string.transaction_category_choose)
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .height(48.dp)
                .clickable(onClick = onShowGrid)
                .clearAndSetSemantics {
                    contentDescription = description
                    role = Role.Button
                }.padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null && name != null) {
            val color = categoryColor(selected)
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(color.copy(alpha = LocalAppExtraColors.current.categoryTintAlpha), RoundedCornerShape(10.dp))
                        .border(2.dp, color, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(selected.icon, fontSize = 16.sp)
            }
            Spacer(Modifier.width(10.dp))
            Text(name, fontWeight = FontWeight.W700)
        } else {
            Text(stringResource(R.string.transaction_category_choose))
        }
        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.transaction_category_change),
            style = typography.caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun categoryColor(category: Category): Color =
    runCatching { Color(android.graphics.Color.parseColor(category.color)) }.getOrDefault(Color.Gray)
