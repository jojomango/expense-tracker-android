@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 週起始日設定（Phase 5）、分類管理（Phase 6）、匯出/匯入 UI（Phase 7）都掛在
 * 這一頁底下——Phase 4 先給一個能導航進來、能導航回去的骨架。
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Text("週起始日、分類管理、備份/還原將在後續 phase 加入")
        }
    }
}
