package com.jojomango.expensetracker.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * UI-SPEC.md §6 統計頁的完整實作（圓環/趨勢圖）留給 Phase 6——這裡先放一個
 * 骨架畫面，讓底部導覽的「統計」路由有東西可以導向。
 */
@Composable
fun StatsScreen() {
    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("統計（Phase 6 實作）")
        }
    }
}
