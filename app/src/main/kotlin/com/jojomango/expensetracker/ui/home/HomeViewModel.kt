@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jojomango.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Budget
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.BudgetStatus
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.DefaultCategories
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.TotalBudgetStatus
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.domain.WeekGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val wallets: List<Wallet> = emptyList(),
    val currentWallet: Wallet? = null,
    val categories: List<Category> = emptyList(),
    val weekStartDay: DayOfWeek = Settings().weekStartDay,
    val weeklyBalance: BudgetStatus? = null,
    val totalBalance: TotalBudgetStatus? = null,
    val weeklyExpenseTotal: Money? = null,
    val transactionGroups: List<WeekGroup<Transaction>> = emptyList(),
) {
    val needsOnboarding: Boolean get() = !isLoading && wallets.isEmpty()
}

private data class HomeContext(
    val wallets: List<Wallet>,
    val categories: List<Category>,
    val settings: Settings,
    val resolvedWalletId: String?,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val walletRepository: WalletRepository,
        private val transactionRepository: TransactionRepository,
        private val categoryRepository: CategoryRepository,
        private val settingsRepository: SettingsRepository,
    ) : ViewModel() {
        /** 使用者手動選過的錢包；`null` 代表還沒選過，交給 [Settings.defaultWalletId] 或第一個錢包決定。 */
        private val selectedWalletId = MutableStateFlow<String?>(null)

        private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val uiState: StateFlow<HomeUiState> =
            combine(
                walletRepository.observeWallets(),
                categoryRepository.observeCategories(),
                settingsRepository.observe(),
                selectedWalletId,
            ) { wallets, categories, settings, selectedId ->
                val resolvedId = selectedId ?: settings.defaultWalletId ?: wallets.firstOrNull { !it.archived }?.id
                HomeContext(wallets, categories, settings, resolvedId)
            }.flatMapLatest { ctx ->
                val wallet = ctx.wallets.firstOrNull { it.id == ctx.resolvedWalletId }
                val transactionsFlow = if (wallet != null) transactionRepository.observeByWallet(wallet.id) else flowOf(emptyList())
                transactionsFlow.map { transactions -> buildUiState(wallet, ctx, transactions) }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState(),
            )

        private fun buildUiState(
            wallet: Wallet?,
            ctx: HomeContext,
            transactions: List<Transaction>,
        ): HomeUiState {
            if (wallet == null) {
                return HomeUiState(
                    isLoading = false,
                    wallets = ctx.wallets,
                    categories = ctx.categories,
                    weekStartDay = ctx.settings.weekStartDay,
                )
            }
            val referenceDate = today()
            val weeklyBalance = Budget.calculateWeeklyBalance(wallet, transactions, ctx.settings.weekStartDay, referenceDate)
            val totalBalance = Budget.calculateTotalBalance(wallet, transactions)
            val weeklyExpenseTotal =
                Budget.calculateWeeklyExpenseTotal(wallet, transactions, ctx.settings.weekStartDay, referenceDate)
            val groups = Week.groupByWeek(transactions, ctx.settings.weekStartDay) { it.date }
            return HomeUiState(
                isLoading = false,
                wallets = ctx.wallets,
                currentWallet = wallet,
                categories = ctx.categories,
                weekStartDay = ctx.settings.weekStartDay,
                weeklyBalance = weeklyBalance,
                totalBalance = totalBalance,
                weeklyExpenseTotal = weeklyExpenseTotal,
                transactionGroups = groups,
            )
        }

        fun switchWallet(walletId: String) {
            selectedWalletId.value = walletId
            viewModelScope.launch {
                settingsRepository.update(settingsRepository.get().copy(defaultWalletId = walletId))
            }
        }

        /** E2E-1：首次啟動引導——建立第一個錢包，並種下預設分類。 */
        fun createFirstWallet(
            name: String,
            currency: String,
            budgetMode: BudgetMode,
            budgetAmount: Long?,
        ) {
            viewModelScope.launch {
                categoryRepository.seedDefaultsIfEmpty()
                val wallet =
                    Wallet(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        currency = currency,
                        budgetMode = budgetMode,
                        budgetAmount = budgetAmount,
                    )
                walletRepository.upsert(wallet)
                selectedWalletId.value = wallet.id
            }
        }

        fun deleteTransaction(transaction: Transaction) {
            viewModelScope.launch { transactionRepository.delete(transaction.id) }
        }

        /** Snackbar「還原」動作鍵——UI-SPEC.md §4.3。 */
        fun undoDeleteTransaction(transaction: Transaction) {
            viewModelScope.launch { transactionRepository.upsert(transaction) }
        }

        companion object {
            /** 首次啟動的預設分類種子，暴露給 onboarding 畫面顯示（不需要先建錢包才看得到）。 */
            val defaultCategoryPreview = DefaultCategories.seedDefaults()
        }
    }
