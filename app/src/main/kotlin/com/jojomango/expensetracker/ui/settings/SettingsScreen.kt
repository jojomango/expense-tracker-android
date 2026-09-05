@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.datetime.DayOfWeek

/**
 * 週起始日設定（Phase 5）掛在這一頁；分類管理（Phase 6）、匯出/匯入 UI（Phase 7）
 * 留給後續 phase。錢包管理不掛在這裡——UI-SPEC.md §7 講的入口是錢包切換 sheet
 * 的「管理錢包…」，不是設定頁。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageWallets: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            ListItem(
                headlineContent = { Text("管理錢包") },
                modifier = Modifier.clickable(onClick = onManageWallets),
            )

            Text("週起始日", modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                weekStartDayOptions.forEach { (day, label) ->
                    FilterChip(
                        selected = settings.weekStartDay == day,
                        onClick = { viewModel.setWeekStartDay(day) },
                        label = { Text(label) },
                    )
                }
            }

            Text("分類管理、備份/還原將在後續 phase 加入", modifier = Modifier.padding(top = 20.dp))
        }
    }
}

private val weekStartDayOptions =
    listOf(
        DayOfWeek.SUNDAY to "週日",
        DayOfWeek.MONDAY to "週一",
        DayOfWeek.TUESDAY to "週二",
        DayOfWeek.WEDNESDAY to "週三",
        DayOfWeek.THURSDAY to "週四",
        DayOfWeek.FRIDAY to "週五",
        DayOfWeek.SATURDAY to "週六",
    )
