package com.jojomango.expensetracker.ui.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Currencies
import com.jojomango.expensetracker.domain.LastWalletException
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.majorDigitsToMinorUnits
import com.jojomango.expensetracker.domain.minorUnitsToMajorDigits
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 編輯頁結束時要回到錢包管理頁，並帶一則 Snackbar 訊息（UI-SPEC.md §11）。 */
sealed interface WalletEditResult {
    data object Saved : WalletEditResult

    data class Archived(
        val name: String,
    ) : WalletEditResult

    data class Deleted(
        val name: String,
    ) : WalletEditResult
}

data class WalletEditUiState(
    val isLoading: Boolean = true,
    /** 編輯既有錢包時才有值；新增錢包時是 null，`currency` 因此可以編輯。 */
    val walletId: String? = null,
    val name: String = "",
    val currency: String = "TWD",
    val budgetMode: BudgetMode = BudgetMode.WEEKLY,
    val budgetAmountDigits: String = "",
    /** 保留既有錢包的封存狀態——存檔時如果不帶這個值，封存的錢包會被默默取消封存。 */
    val archived: Boolean = false,
    /** 這是最後一個未封存的錢包：不能封存也不能刪除（SPEC.md §3.1、UI-SPEC.md §11）。 */
    val isLastActive: Boolean = false,
    /** 刪除確認對話框要顯示的交易筆數；`null` 代表對話框沒開。 */
    val pendingDeleteTransactionCount: Int? = null,
    val result: WalletEditResult? = null,
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
        private val transactionRepository: TransactionRepository,
        private val analyticsTracker: AnalyticsTracker,
    ) : ViewModel() {
        private val walletId: String? = savedStateHandle.get<String>("walletId")

        private val internalState = MutableStateFlow(WalletEditUiState(walletId = walletId))
        val uiState: StateFlow<WalletEditUiState> = internalState.asStateFlow()

        init {
            viewModelScope.launch {
                val existing = walletId?.let { walletRepository.getWallet(it) }
                val activeCount = walletRepository.getAllOnce().count { !it.archived }
                internalState.value =
                    if (existing != null) {
                        val decimalDigits = Currencies.builtIn[existing.currency]?.decimalDigits ?: 2
                        internalState.value.copy(
                            isLoading = false,
                            archived = existing.archived,
                            isLastActive = !existing.archived && activeCount <= 1,
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
                        archived = state.archived,
                    )
                walletRepository.upsert(wallet)
                if (state.walletId == null) analyticsTracker.track(AnalyticsEvent.WalletCreated(wallet.budgetMode))
                internalState.value = internalState.value.copy(result = WalletEditResult.Saved)
            }
        }

        // LastWalletException 在這裡是預期中的「被擋下」訊號，處理方式見 onBlockedAsLastActive
        @Suppress("SwallowedException")
        fun archive() {
            val id = walletId ?: return
            if (internalState.value.isLastActive) return
            viewModelScope.launch {
                try {
                    walletRepository.setArchived(id, true)
                    internalState.value = internalState.value.copy(result = WalletEditResult.Archived(internalState.value.name))
                } catch (e: LastWalletException) {
                    onBlockedAsLastActive()
                }
            }
        }

        /** 先查交易筆數再開確認對話框——筆數要即時查，不能用進頁面時的舊值。 */
        fun requestDelete() {
            val id = walletId ?: return
            if (internalState.value.isLastActive) return
            viewModelScope.launch {
                val count = transactionRepository.countByWallet(id)
                internalState.value = internalState.value.copy(pendingDeleteTransactionCount = count)
            }
        }

        fun dismissDelete() {
            internalState.value = internalState.value.copy(pendingDeleteTransactionCount = null)
        }

        // LastWalletException 在這裡是預期中的「被擋下」訊號，處理方式見 onBlockedAsLastActive
        @Suppress("SwallowedException")
        fun confirmDelete() {
            val id = walletId ?: return
            viewModelScope.launch {
                try {
                    walletRepository.delete(id)
                    internalState.value =
                        internalState.value.copy(
                            pendingDeleteTransactionCount = null,
                            result = WalletEditResult.Deleted(internalState.value.name),
                        )
                } catch (e: LastWalletException) {
                    onBlockedAsLastActive()
                }
            }
        }

        // 畫面上的狀態可能過時（例如進頁面之後別的錢包被封存了），repository 才是最後一道關卡。
        // 被擋下時不 crash，改成把按鈕停用，跟一開始就是最後一個錢包的畫面一樣。
        private fun onBlockedAsLastActive() {
            internalState.value = internalState.value.copy(isLastActive = true, pendingDeleteTransactionCount = null)
        }
    }
