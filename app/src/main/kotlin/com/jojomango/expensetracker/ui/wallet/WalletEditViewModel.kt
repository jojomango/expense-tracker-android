package com.jojomango.expensetracker.ui.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.majorDigitsToMinorUnits
import com.jojomango.expensetracker.domain.minorUnitsToMajorDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalletEditUiState(
    val isLoading: Boolean = true,
    /** 編輯既有錢包時才有值；新增錢包時是 null，`currency` 因此可以編輯。 */
    val walletId: String? = null,
    val name: String = "",
    val currency: String = "TWD",
    val budgetMode: BudgetMode = BudgetMode.WEEKLY,
    val budgetAmountDigits: String = "",
    val saved: Boolean = false,
) {
    val isEditing: Boolean get() = walletId != null
    val canSubmit: Boolean get() = name.isNotBlank() && Currencies.builtIn.containsKey(currency)
}

@HiltViewModel
class WalletEditViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val walletRepository: WalletRepository,
    ) : ViewModel() {
        private val walletId: String? = savedStateHandle.get<String>("walletId")

        private val internalState = MutableStateFlow(WalletEditUiState(walletId = walletId))
        val uiState: StateFlow<WalletEditUiState> = internalState.asStateFlow()

        init {
            viewModelScope.launch {
                val existing = walletId?.let { walletRepository.getWallet(it) }
                internalState.value =
                    if (existing != null) {
                        val decimalDigits = Currencies.builtIn[existing.currency]?.decimalDigits ?: 2
                        internalState.value.copy(
                            isLoading = false,
                            name = existing.name,
                            currency = existing.currency,
                            budgetMode = existing.budgetMode,
                            budgetAmountDigits =
                                existing.budgetAmount?.let { minorUnitsToMajorDigits(it, decimalDigits) } ?: "",
                        )
                    } else {
                        internalState.value.copy(isLoading = false)
                    }
            }
        }

        fun onNameChange(name: String) {
            internalState.value = internalState.value.copy(name = name)
        }

        fun onCurrencyChange(currency: String) {
            internalState.value = internalState.value.copy(currency = currency.uppercase())
        }

        fun onBudgetModeChange(mode: BudgetMode) {
            internalState.value = internalState.value.copy(budgetMode = mode)
        }

        fun onBudgetAmountChange(digits: String) {
            internalState.value = internalState.value.copy(budgetAmountDigits = digits.filter { it.isDigit() })
        }

        fun submit() {
            val state = internalState.value
            if (!state.canSubmit) return
            viewModelScope.launch {
                val decimalDigits = Currencies.builtIn[state.currency]?.decimalDigits ?: 2
                val budgetAmount =
                    if (state.budgetMode == BudgetMode.NONE) {
                        null
                    } else {
                        majorDigitsToMinorUnits(state.budgetAmountDigits.ifEmpty { "0" }, decimalDigits)
                    }
                val wallet =
                    Wallet(
                        id =
                            state.walletId ?: java.util.UUID
                                .randomUUID()
                                .toString(),
                        name = state.name,
                        currency = state.currency,
                        budgetMode = state.budgetMode,
                        budgetAmount = budgetAmount,
                    )
                walletRepository.upsert(wallet)
                internalState.value = internalState.value.copy(saved = true)
            }
        }
    }
