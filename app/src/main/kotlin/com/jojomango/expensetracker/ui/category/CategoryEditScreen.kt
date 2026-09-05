@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.CategoryType
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
            Row {
                CategoryColors.palette.forEach { hex ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
                    Column {
                        // T8.3.7：所有可點元素寬高皆需 >= 48dp。
                        androidx.compose.foundation.layout.Box(
                            modifier =
                                Modifier
                                    .padding(end = 8.dp)
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.onColorChange(hex) },
                        ) {
                            if (state.color == hex) {
                                Text(
                                    "✓",
                                    color = Color.White,
                                    modifier = Modifier.fillMaxSize(),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
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
