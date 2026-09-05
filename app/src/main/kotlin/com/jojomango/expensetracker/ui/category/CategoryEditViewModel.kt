package com.jojomango.expensetracker.ui.category

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.CategoryType
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
    val name: String = "",
    val type: CategoryType = CategoryType.EXPENSE,
    val icon: String = "🏷️",
    val color: String = CategoryColors.palette.first(),
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = categoryId != null
    val canSubmit: Boolean get() = name.isNotBlank() && icon.isNotBlank()
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
                            name = existing.name,
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

        fun submit() {
            val state = internalState.value
            if (!state.canSubmit) return
            viewModelScope.launch {
                val category =
                    Category(
                        id = state.categoryId ?: UUID.randomUUID().toString(),
                        name = state.name,
                        type = state.type,
                        icon = state.icon,
                        color = state.color,
                        isDefault = state.isDefault,
                    )
                categoryRepository.upsert(category)
                internalState.value = internalState.value.copy(saved = true)
            }
        }
    }
