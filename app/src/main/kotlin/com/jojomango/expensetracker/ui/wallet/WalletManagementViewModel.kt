package com.jojomango.expensetracker.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 編輯頁結束時，透過錢包管理頁那一層 `NavBackStackEntry.savedStateHandle` 的這個 key 交回
 * Snackbar 訊息（見 `MainActivity`）。注意不能從這個 ViewModel 的 `SavedStateHandle` 讀——
 * Navigation Compose 裡那是另一個物件，只拿得到路由參數，讀不到別的畫面寫進來的值。
 */
const val WALLET_MESSAGE_KEY = "walletMessage"

@HiltViewModel
class WalletManagementViewModel
    @Inject
    constructor(
        private val walletRepository: WalletRepository,
    ) : ViewModel() {
        val activeWallets: StateFlow<List<Wallet>> =
            walletRepository
                .observeWallets()
                .map { wallets -> wallets.filterNot { it.archived } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val archivedWallets: StateFlow<List<Wallet>> =
            walletRepository
                .observeWallets()
                .map { wallets -> wallets.filter { it.archived } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun unarchive(walletId: String) {
            viewModelScope.launch { walletRepository.setArchived(walletId, false) }
        }
    }
