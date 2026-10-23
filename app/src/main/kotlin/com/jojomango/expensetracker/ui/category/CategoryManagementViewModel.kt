package com.jojomango.expensetracker.ui.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.DefaultCategoryException
import com.jojomango.expensetracker.ui.i18n.UiMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryManagementViewModel
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
    ) : ViewModel() {
        val categories: StateFlow<List<Category>> =
            categoryRepository.observeCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _errorMessages = MutableSharedFlow<UiMessage>()
        val errorMessages: SharedFlow<UiMessage> = _errorMessages

        /** SPEC.md §3.3：系統預設分類可改名但不可刪除——刪除時拋出 [DefaultCategoryException]，
         * 這裡接住轉成給使用者看的錯誤訊息（TESTCASES.md E2E-8）。[DefaultCategoryException.message]
         * 本身是英文（domain 層的內部訊息，不是面向使用者的文字），故意不拿來顯示，
         * 一律換成 string resources 的訊息。 */
        @Suppress("SwallowedException")
        fun deleteCategory(category: Category) {
            viewModelScope.launch {
                try {
                    categoryRepository.delete(category.id)
                } catch (e: DefaultCategoryException) {
                    _errorMessages.emit(UiMessage(R.string.category_default_cannot_delete))
                }
            }
        }
    }
