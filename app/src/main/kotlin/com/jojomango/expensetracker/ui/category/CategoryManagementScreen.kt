@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jojomango.expensetracker.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryType

/** UI-SPEC.md §6 沒有給分類管理頁完整規格（只提到分類色票），這裡沿用
 * `WalletManagementScreen` 一樣的「列表 + 新增/編輯表單」骨架。刪除用
 * `SwipeToDismissBox`（跟首頁交易列表同一套手勢），系統預設分類刪除會
 * 失敗，用 Snackbar 顯示錯誤（TESTCASES.md E2E-8）。 */
@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessages.collect { message -> snackbarHostState.showSnackbar(message) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("分類管理") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = onAddCategory) { Text("新增") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            CategorySection(
                title = "支出",
                categories = categories.filter { it.type == CategoryType.EXPENSE },
                onEditCategory = onEditCategory,
                onDeleteCategory = viewModel::deleteCategory,
            )
            CategorySection(
                title = "收入",
                categories = categories.filter { it.type == CategoryType.INCOME },
                onEditCategory = onEditCategory,
                onDeleteCategory = viewModel::deleteCategory,
            )
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    categories: List<Category>,
    onEditCategory: (String) -> Unit,
    onDeleteCategory: (Category) -> Unit,
) {
    if (categories.isEmpty()) return
    Text(title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
    LazyColumn {
        items(categories, key = { it.id }) { category ->
            CategoryRow(category = category, onEdit = { onEditCategory(category.id) }, onDelete = { onDeleteCategory(category) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    onDelete()
                }
                // 系統預設分類的刪除會被 ViewModel 拒絕（只是跳錯誤訊息），這裡
                // 一律回傳 false 讓列自己歸位，不要假裝刪除已經生效——真正刪除
                // 成功後這一列會因為 categories Flow 更新而直接從清單消失。
                false
            },
        )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("刪除", color = MaterialTheme.colorScheme.error)
            }
        },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(role = Role.Button, onClick = onEdit)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val color = runCatching { Color(android.graphics.Color.parseColor(category.color)) }.getOrDefault(Color.Gray)
            Box(
                modifier = Modifier.size(38.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(category.icon)
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(category.name)
                if (category.isDefault) {
                    Text("系統預設", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
