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
import javax.inject.Inject

@HiltViewModel
class WalletManagementViewModel
    @Inject
    constructor(
        walletRepository: WalletRepository,
    ) : ViewModel() {
        val wallets: StateFlow<List<Wallet>> =
            walletRepository
                .observeWallets()
                .map { wallets -> wallets.filterNot { it.archived } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }
