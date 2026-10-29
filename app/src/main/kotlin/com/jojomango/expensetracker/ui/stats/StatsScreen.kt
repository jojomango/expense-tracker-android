@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.WeeklyTrendPoint
import com.jojomango.expensetracker.domain.displayName
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.i18n.rememberCategoryNames
import com.jojomango.expensetracker.ui.theme.LocalAppExtraColors
import com.jojomango.expensetracker.ui.theme.LocalAppTypography

/** UI-SPEC.md §6 統計頁：圓環卡（Canvas 手畫，不用圖表函式庫）+ 趨勢卡
 * （8 根柱狀圖，同樣 Canvas 手畫）。 */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val typography = LocalAppTypography.current

    Scaffold { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
        ) {
            Text(stringResource(R.string.stats_title), style = typography.titleLarge, modifier = Modifier.padding(vertical = 16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.period == StatsPeriod.WEEK,
                    onClick = { viewModel.setPeriod(StatsPeriod.WEEK) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.stats_this_week)) }
                SegmentedButton(
                    selected = state.period == StatsPeriod.MONTH,
                    onClick = { viewModel.setPeriod(StatsPeriod.MONTH) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.stats_this_month)) }
            }

            Spacer(Modifier.height(16.dp))
            DonutCard(state = state)

            Spacer(Modifier.height(16.dp))
            TrendCard(points = state.trendPoints)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DonutCard(state: StatsUiState) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val periodLabel =
        stringResource(
            if (state.period ==
                StatsPeriod.WEEK
            ) {
                R.string.stats_spent_this_week
            } else {
                R.string.stats_spent_this_month
            },
        )

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            // 外框必須跟圓環一樣是正方形。之前外框是 1.4:1、Canvas 用 fillMaxSize().aspectRatio(1f)，
            // aspectRatio 在尺寸已經被 fillMaxSize 鎖死時會忽略約束，畫出一個以寬度為直徑的圓，
            // 結果圓環上緣被卡片裁掉、下緣蓋住第一列圖例文字（Phase 7 深色模式檢查時發現）。
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * 0.16f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    var startAngle = -90f
                    if (state.categorySlices.isEmpty()) {
                        drawArc(
                            color = extraColors.track,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            topLeft = topLeft,
                            size = arcSize,
                        )
                    }
                    state.categorySlices.forEach { slice ->
                        val sweep = (slice.percent / 100.0 * 360.0).toFloat()
                        drawArc(
                            color = parseHexColor(slice.color),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth),
                            topLeft = topLeft,
                            size = arcSize,
                        )
                        startAngle += sweep
                    }
                }
                // UI-SPEC.md §6：圓環中心疊獨立的 Box + Text，不畫在 Canvas 裡（排版麻煩）。
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(periodLabel, style = typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.periodTotal?.format() ?: "—", style = typography.donutTotal)
                }
            }

            Spacer(Modifier.height(16.dp))
            state.categorySlices.forEach { slice ->
                CategoryLegendRow(slice)
            }
            if (state.categorySlices.isEmpty()) {
                Text(stringResource(R.string.stats_empty), style = typography.caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryLegendRow(slice: CategorySlice) {
    val categoryNames = rememberCategoryNames()
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val color = parseHexColor(slice.color)
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
                Spacer(Modifier.size(8.dp))
                Text(
                    "${slice.icon} ${slice.category?.displayName(categoryNames) ?: categoryNames.uncategorized}",
                    style = typography.bodyMedium,
                )
            }
            Text(
                "${slice.amount.format()}（${formatPercent(slice.percent)}%）",
                style = typography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (slice.percent / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp),
            color = color,
            trackColor = extraColors.track,
            // Material3 1.3 起預設在右端畫一個 stop indicator 小點、填色與底色間留空隙；
            // UI-SPEC §2.4 只要圓角的單一長條，兩者都關掉
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun TrendCard(points: List<WeeklyTrendPoint>) {
    val typography = LocalAppTypography.current
    val extraColors = LocalAppExtraColors.current
    val primary = MaterialTheme.colorScheme.primary

    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(stringResource(R.string.stats_trend_title), style = typography.cardTitle)
            Spacer(Modifier.height(16.dp))

            val maxAmount = points.maxOfOrNull { it.total.amount } ?: 0L
            val average = if (points.isNotEmpty()) points.sumOf { it.total.amount } / points.size else 0L

            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (points.isEmpty() || maxAmount <= 0) return@Canvas
                val columnWidth = size.width / points.size
                val barWidth = columnWidth * 0.5f
                points.forEachIndexed { index, point ->
                    val barHeight = (point.total.amount.toFloat() / maxAmount.toFloat()) * size.height
                    val x = index * columnWidth + (columnWidth - barWidth) / 2
                    drawRect(
                        color = if (index == points.lastIndex) primary else extraColors.track,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidth, barHeight),
                    )
                }
                val averageY = size.height - (average.toFloat() / maxAmount.toFloat()) * size.height
                drawLine(
                    color = extraColors.fg3,
                    start = Offset(0f, averageY),
                    end = Offset(size.width, averageY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                points.forEachIndexed { index, point ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            "${point.range.start.monthNumber}/${point.range.start.dayOfMonth}",
                            style = typography.tabLabel,
                            color = if (index == points.lastIndex) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun parseHexColor(hex: String): Color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)

/** 百分比顯示到小數點後 2 位，但整數時不顯示多餘的 ".00"（例如 E2E-3 的 60%、E2E-10 的 66.67%）。 */
private fun formatPercent(percent: Double): String {
    val rounded = kotlin.math.round(percent * 100) / 100.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
