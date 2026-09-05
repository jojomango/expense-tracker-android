package com.jojomango.expensetracker.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.appendDigit
import com.jojomango.expensetracker.domain.deleteDigit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.UUID
import javax.inject.Inject

data class AddEditTransactionUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val wallet: Wallet? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountDigits: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val note: String = "",
    val submitted: Boolean = false,
) {
    val amount: Long get() = amountDigits.toLongOrNull() ?: 0L
    val categoriesForType: List<Category> get() = categories.filter { it.type == categoryTypeOf(type) }
    val canSubmit: Boolean get() = amount > 0

    companion object {
        fun categoryTypeOf(type: TransactionType): CategoryType =
            if (type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME
    }
}

@HiltViewModel
class AddEditTransactionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val walletRepository: WalletRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: com.jojomango.expensetracker.domain.SettingsRepository,
    ) : ViewModel() {
        private val transactionId: String? = savedStateHandle.get<String>("transactionId")

        private val internalState = MutableStateFlow(AddEditTransactionUiState(isEditing = transactionId != null))
        private var loadedOriginal: Transaction? = null

        val uiState: StateFlow<AddEditTransactionUiState> =
            combine(internalState, categoryRepository.observeCategories()) { state, categories ->
                state.copy(categories = categories)
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), internalState.value)

        init {
            viewModelScope.launch {
                val wallet =
                    if (transactionId != null) {
                        val existing = transactionRepository.getAllOnce().firstOrNull { it.id == transactionId }
                        loadedOriginal = existing
                        val w = existing?.walletId?.let { walletRepository.getWallet(it) }
                        if (existing != null) {
                            internalState.value =
                                internalState.value.copy(
                                    type = existing.type,
                                    amountDigits = existing.amount.toString(),
                                    selectedCategoryId = existing.categoryId,
                                    date = existing.date,
                                    note = existing.note.orEmpty(),
                                )
                        }
                        w
                    } else {
                        val defaultWalletId = settingsRepository.get().defaultWalletId
                        defaultWalletId?.let { walletRepository.getWallet(it) }
                            ?: walletRepository.getAllOnce().firstOrNull { !it.archived }
                    }
                internalState.value = internalState.value.copy(isLoading = false, wallet = wallet)
            }
        }

        fun onDigit(input: String) {
            internalState.value = internalState.value.copy(amountDigits = appendDigit(internalState.value.amountDigits, input))
        }

        fun onBackspace() {
            internalState.value = internalState.value.copy(amountDigits = deleteDigit(internalState.value.amountDigits))
        }

        fun onSelectType(type: TransactionType) {
            internalState.value = internalState.value.copy(type = type, selectedCategoryId = null)
        }

        fun onSelectCategory(categoryId: String) {
            internalState.value = internalState.value.copy(selectedCategoryId = categoryId)
        }

        fun onSelectDate(date: LocalDate) {
            internalState.value = internalState.value.copy(date = date)
        }

        fun onNoteChange(note: String) {
            internalState.value = internalState.value.copy(note = note)
        }

        fun submit() {
            val state = internalState.value
            val wallet = state.wallet ?: return
            if (!state.canSubmit) return
            viewModelScope.launch {
                val now = Clock.System.now()
                val transaction =
                    Transaction(
                        id = loadedOriginal?.id ?: UUID.randomUUID().toString(),
                        walletId = wallet.id,
                        type = state.type,
                        amount = state.amount,
                        categoryId = state.selectedCategoryId,
                        date = state.date,
                        note = state.note.ifBlank { null },
                        createdAt = loadedOriginal?.createdAt ?: now,
                        updatedAt = now,
                    )
                transactionRepository.upsert(transaction)
                internalState.value = internalState.value.copy(submitted = true)
            }
        }

        fun formattedAmount(): String {
            val wallet = internalState.value.wallet ?: return internalState.value.amountDigits
            return Money.of(internalState.value.amount, wallet.currency).format()
        }
    }
