package com.jojomango.expensetracker.ui.wallet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.ui.home.HomeContext
import com.jojomango.expensetracker.ui.home.HomeUiState
import com.jojomango.expensetracker.ui.home.buildHomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

data class ArchivedWalletUiState(
    /** 預算卡與交易列表沿用首頁的狀態，規則才不會兩邊各寫一份（UI-SPEC.md §11.1）。 */
    val home: HomeUiState = HomeUiState(),
    /** 刪除確認對話框要顯示的交易筆數；`null` 代表對話框沒開。 */
    val pendingDeleteTransactionCount: Int? = null,
    val result: WalletEditResult? = null,
)

private data class ArchivedWalletActions(
    val pendingDeleteTransactionCount: Int? = null,
    val result: WalletEditResult? = null,
)

/** UI-SPEC.md §11.1 封存錢包檢視頁：唯讀，只能取消封存或刪除。 */
@HiltViewModel
class ArchivedWalletViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val walletRepository: WalletRepository,
        private val transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val walletId: String = checkNotNull(savedStateHandle["walletId"]) { "route must carry walletId" }
        private val actions = MutableStateFlow(ArchivedWalletActions())

        val uiState: StateFlow<ArchivedWalletUiState> =
            combine(
                walletRepository.observeWallets(),
                transactionRepository.observeByWallet(walletId),
                categoryRepository.observeCategories(),
                settingsRepository.observe(),
                actions,
            ) { wallets, transactions, categories, settings, action ->
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val wallet = wallets.firstOrNull { it.id == walletId }
                ArchivedWalletUiState(
                    home = buildHomeUiState(wallet, HomeContext(wallets, categories, settings), transactions, today),
                    pendingDeleteTransactionCount = action.pendingDeleteTransactionCount,
                    result = action.result,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ArchivedWalletUiState())

        private val walletName: String get() =
            uiState.value.home.currentWallet
                ?.name
                .orEmpty()

        // 封存的錢包不會是最後一個未封存的錢包，取消封存與刪除都不會被 LastWalletException 擋下（UI-SPEC.md §11.1）
        fun unarchive() {
            val name = walletName
            viewModelScope.launch {
                walletRepository.setArchived(walletId, false)
                actions.value = actions.value.copy(result = WalletEditResult.Unarchived(name))
            }
        }

        /** 先查交易筆數再開確認對話框——筆數要即時查，不能用進頁面時的舊值。 */
        fun requestDelete() {
            viewModelScope.launch {
                val count = transactionRepository.countByWallet(walletId)
                actions.value = actions.value.copy(pendingDeleteTransactionCount = count)
            }
        }

        fun dismissDelete() {
            actions.value = actions.value.copy(pendingDeleteTransactionCount = null)
        }

        fun confirmDelete() {
            val name = walletName
            viewModelScope.launch {
                walletRepository.delete(walletId)
                actions.value = ArchivedWalletActions(result = WalletEditResult.Deleted(name))
            }
        }
    }
