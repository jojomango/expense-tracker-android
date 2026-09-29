@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.jojomango.expensetracker.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.ui.common.LoadingState
import com.jojomango.expensetracker.ui.theme.CategoryColors

/** 新增／編輯分類——沿用 `WalletEditScreen` 同樣的骨架。分類型別（支出/收入）
 * 建立後不可改，理由見 `CategoryEditViewModel`。色票只能從
 * `CategoryColors.palette` 挑，不做自由選色（TASKS.md Phase 6）。 */
@Composable
fun CategoryEditScreen(
    onDone: () -> Unit,
    onCancel: () -> Unit,
    viewModel: CategoryEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "編輯分類" else "新增分類") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("分類名稱") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.icon,
                onValueChange = viewModel::onIconChange,
                label = { Text("Icon（emoji）") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("類型")
            Row {
                listOf(CategoryType.EXPENSE to "支出", CategoryType.INCOME to "收入").forEach { (type, label) ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        label = { Text(label) },
                        enabled = !state.isEditing,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("顏色")
            // FlowRow 而不是 Row：10 個 48dp 的色塊加間距約 560dp，一般手機螢幕寬只有 ~393dp，
            // 用 Row 的話最後 4 個顏色（灰、翠綠、金黃、靛藍）會被擠出螢幕外、根本點不到
            // （Phase 7 無障礙檢查時發現）。
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp).selectableGroup(),
            ) {
                CategoryColors.palette.forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    val selected = state.color == hex
                    // T8.3.7：所有可點元素寬高皆需 >= 48dp。色塊只有顏色沒有文字，TalkBack
                    // 需要一個顏色名稱才唸得出來，不然只會唸「按鈕」。
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .selectable(
                                    selected = selected,
                                    onClick = { viewModel.onColorChange(hex) },
                                    role = Role.RadioButton,
                                ).semantics { contentDescription = colorNames[hex] ?: hex },
                    ) {
                        if (selected) Text("✓", color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isEditing) "儲存" else "建立分類")
            }
        }
    }
}

/** 色票的中文名稱，給 TalkBack 唸——色塊本身沒有文字。 */
private val colorNames =
    mapOf(
        CategoryColors.FOOD to "橘紅",
        CategoryColors.TRANSPORT to "綠",
        CategoryColors.HOUSING to "土黃",
        CategoryColors.SHOPPING to "藍",
        CategoryColors.ENTERTAINMENT to "紫",
        CategoryColors.MEDICAL to "玫紅",
        CategoryColors.MISC to "灰",
        CategoryColors.SALARY to "翠綠",
        CategoryColors.BONUS to "金黃",
        CategoryColors.INVESTMENT to "靛藍",
    )
