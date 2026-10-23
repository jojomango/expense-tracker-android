package com.jojomango.expensetracker.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.displayName
import com.jojomango.expensetracker.domain.renamed
import com.jojomango.expensetracker.ui.theme.CategoryColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryEditUiState(
    val isLoading: Boolean = true,
    /** 編輯既有分類時才有值；新增時是 null，`type` 因此可以編輯——分類建立後
     * 型別（支出/收入）不可改，理由跟 Wallet.currency 一樣：已有的統計/預算
     * 計算都假設一個分類的 type 不會變動。 */
    val categoryId: String? = null,
    val isDefault: Boolean = false,
    /** 編輯前的分類；預設分類的名稱要靠它在畫面上依目前語言顯示（SPEC.md §3.9）。 */
    val existing: Category? = null,
    /** 使用者在名稱欄位打的字；`null` = 還沒動過，欄位顯示 [existing] 的顯示名稱。 */
    val name: String? = null,
    val type: CategoryType = CategoryType.EXPENSE,
    val icon: String = "🏷️",
    val color: String = CategoryColors.palette.first(),
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = categoryId != null
    val canSubmit: Boolean get() = (name ?: existing?.name).orEmpty().isNotBlank() && icon.isNotBlank()
}

@HiltViewModel
class CategoryEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        private val categoryId: String? = savedStateHandle.get<String>("categoryId")

        private val internalState = MutableStateFlow(CategoryEditUiState(categoryId = categoryId))
        val uiState: StateFlow<CategoryEditUiState> = internalState.asStateFlow()

        init {
            viewModelScope.launch {
                val existing = categoryId?.let { id -> categoryRepository.getAllOnce().firstOrNull { it.id == id } }
                internalState.value =
                    if (existing != null) {
                        internalState.value.copy(
                            isLoading = false,
                            isDefault = existing.isDefault,
                            existing = existing,
                            type = existing.type,
                            icon = existing.icon,
                            color = existing.color,
                        )
                    } else {
                        internalState.value.copy(isLoading = false)
                    }
            }
        }

        fun onNameChange(name: String) {
            internalState.value = internalState.value.copy(name = name)
        }

        fun onTypeChange(type: CategoryType) {
            internalState.value = internalState.value.copy(type = type)
        }

        fun onIconChange(icon: String) {
            internalState.value = internalState.value.copy(icon = icon)
        }

        fun onColorChange(color: String) {
            internalState.value = internalState.value.copy(color = color)
        }

        /**
         * [names] 是目前語言的分類名稱：預設分類只改圖示或顏色時名稱欄位不變，不算改名，
         * 繼續跟著語言顯示（[renamed]、T10.2.3）。
         */
        fun submit(names: CategoryNames) {
            val state = internalState.value
            if (!state.canSubmit) return
            val existing = state.existing
            val newName = state.name ?: existing?.displayName(names) ?: return
            viewModelScope.launch {
                val category =
                    (
                        existing?.renamed(newName, names)
                            ?: Category(UUID.randomUUID().toString(), newName, state.type, state.icon, state.color)
                    ).copy(type = state.type, icon = state.icon, color = state.color)
                categoryRepository.upsert(category)
                internalState.value = internalState.value.copy(saved = true)
            }
        }
    }
