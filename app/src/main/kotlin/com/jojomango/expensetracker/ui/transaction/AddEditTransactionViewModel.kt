package com.jojomango.expensetracker.ui.transaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.MultiCurrency
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.appendDigit
import com.jojomango.expensetracker.domain.deleteDigit
import com.jojomango.expensetracker.domain.majorDigitsToMinorUnits
import com.jojomango.expensetracker.domain.minorUnitsToMajorDigits
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
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
    /** 這筆交易的幣別（UI-SPEC.md §15.2）；載入完成前先用錢包的預設幣別。 */
    val currency: String = wallet?.currency ?: "TWD",
    /** 幣別 sheet 的列表順序（[MultiCurrency.pickerOrder]）。 */
    val currencyChoices: List<String> = emptyList(),
    val type: TransactionType = TransactionType.EXPENSE,
    val amountDigits: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val date: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val note: String = "",
    /** 儲存成功的交易；首頁的「已記錄」Snackbar 要用它的分類與幣別。 */
    val saved: Transaction? = null,
) {
    /**
     * 自製鍵台輸入的是「幣別主要單位」的整數位數（跟 [amountDigits] 一樣的字串），
     * 不是 [Money] 的最小單位——這裡要乘上 `10^decimalDigits` 換算，
     * 對應 UI-SPEC.md §5「即時千分位格式化」與 TESTCASES.md E2E-2 的
     * 「輸入金額「120」...顯示「NT$120.00」」（不是 NT$1.20）。
     */
    val amount: Long
        get() {
            val decimalDigits = Currencies.builtIn[currency]?.decimalDigits ?: 2
            return majorDigitsToMinorUnits(amountDigits, decimalDigits)
        }
    val categoriesForType: List<Category> get() = categories.filter { it.type == categoryTypeOf(type) }
    val canSubmit: Boolean get() = amount > 0

    companion object {
        fun categoryTypeOf(type: TransactionType): CategoryType =
            if (type == TransactionType.EXPENSE) CategoryType.EXPENSE else CategoryType.INCOME
    }
}

/** 新增交易用錢包的預設幣別、不沿用上一筆；編輯時用這筆交易的幣別（UI-SPEC.md §15.2，T8.5.1、T8.5.5）。 */
internal fun initialCurrency(
    wallet: Wallet,
    existing: Transaction?,
): String = existing?.currency ?: wallet.currency

@HiltViewModel
class AddEditTransactionViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val walletRepository: WalletRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: com.jojomango.expensetracker.domain.SettingsRepository,
        private val analyticsTracker: AnalyticsTracker,
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
                            val decimalDigits = Currencies.builtIn[existing.currency]?.decimalDigits ?: 2
                            internalState.value =
                                internalState.value.copy(
                                    type = existing.type,
                                    amountDigits = minorUnitsToMajorDigits(existing.amount, decimalDigits),
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
                val walletTransactions = wallet?.let { w -> transactionRepository.getAllOnce().filter { it.walletId == w.id } }.orEmpty()
                internalState.value =
                    internalState.value.copy(
                        isLoading = false,
                        wallet = wallet,
                        currency = wallet?.let { initialCurrency(it, loadedOriginal) } ?: internalState.value.currency,
                        currencyChoices = wallet?.let { MultiCurrency.pickerOrder(it.currency, walletTransactions) }.orEmpty(),
                    )
            }
        }

        fun onDigit(input: String) {
            internalState.value = internalState.value.copy(amountDigits = appendDigit(internalState.value.amountDigits, input))
        }

        fun onBackspace() {
            internalState.value = internalState.value.copy(amountDigits = deleteDigit(internalState.value.amountDigits))
        }

        fun onSelectCurrency(code: String) {
            internalState.value = internalState.value.copy(currency = code)
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
                        currency = state.currency,
                        categoryId = state.selectedCategoryId,
                        date = state.date,
                        note = state.note.ifBlank { null },
                        createdAt = loadedOriginal?.createdAt ?: now,
                        updatedAt = now,
                    )
                transactionRepository.upsert(transaction)
                analyticsTracker.track(AnalyticsEvent.TransactionSaved.from(transaction, isEdit = loadedOriginal != null))
                internalState.value = internalState.value.copy(saved = transaction)
            }
        }

        fun formattedAmount(): String {
            val state = internalState.value
            if (state.wallet == null) return state.amountDigits
            return Money.of(state.amount, state.currency).format()
        }
    }
