@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jojomango.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Budget
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Month
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.SettingsRepository
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionRepository
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WalletRepository
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.domain.WeeklyTrendPoint
import com.jojomango.expensetracker.domain.colorOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

enum class StatsPeriod { WEEK, MONTH }

/** 圓環圖的一段弧——[percent] 已經是 0~100 的百分比（`Budget.summarizeByCategory` 算好的）。 */
data class CategorySlice(
    val categoryId: String?,
    val name: String,
    val icon: String,
    val color: String,
    val amount: Money,
    val percent: Double,
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val currency: String = "TWD",
    val period: StatsPeriod = StatsPeriod.WEEK,
    val periodTotal: Money? = null,
    val categorySlices: List<CategorySlice> = emptyList(),
    val trendPoints: List<WeeklyTrendPoint> = emptyList(),
)

private data class StatsContext(
    val wallet: Wallet?,
    val categories: List<Category>,
    val settings: Settings,
    val period: StatsPeriod,
)

/**
 * 目前錢包不是從一個共用的 ViewModel 實例拿（跟 `HomeViewModel` 不同），而是
 * 直接看 `Settings.defaultWalletId`——`HomeViewModel.switchWallet()` 每次都會
 * 把選到的錢包寫回這個持久化欄位，所以這裡不需要另外接住 `HomeViewModel`
 * 那個容易踩到「兩個 ViewModel 實例」的坑（見 TASKS.md Phase 5 交接筆記），
 * 直接訂閱 Settings 的 Flow 就能保持跟首頁同步。
 */
@HiltViewModel
class StatsViewModel
    @Inject
    constructor(
        walletRepository: WalletRepository,
        transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        settingsRepository: SettingsRepository,
    ) : ViewModel() {
        private val period = MutableStateFlow(StatsPeriod.WEEK)

        private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val uiState: StateFlow<StatsUiState> =
            combine(
                walletRepository.observeWallets(),
                categoryRepository.observeCategories(),
                settingsRepository.observe(),
                period,
            ) { wallets, categories, settings, period ->
                val wallet =
                    wallets.firstOrNull { it.id == settings.defaultWalletId }
                        ?: wallets.firstOrNull { !it.archived }
                StatsContext(wallet, categories, settings, period)
            }.flatMapLatest { ctx ->
                val wallet = ctx.wallet
                if (wallet == null) {
                    flowOf(StatsUiState(isLoading = false))
                } else {
                    transactionRepository.observeByWallet(wallet.id).map { transactions -> buildUiState(wallet, ctx, transactions) }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsUiState(),
            )

        fun setPeriod(newPeriod: StatsPeriod) {
            period.value = newPeriod
        }

        private fun buildUiState(
            wallet: Wallet,
            ctx: StatsContext,
            transactions: List<Transaction>,
        ): StatsUiState {
            val referenceDate = today()
            val range =
                when (ctx.period) {
                    StatsPeriod.WEEK -> Week.rangeOf(referenceDate, ctx.settings.weekStartDay)
                    StatsPeriod.MONTH -> Month.rangeOf(referenceDate)
                }
            val periodTransactions = transactions.filter { it.date in range }
            val summary = Budget.summarizeByCategory(periodTransactions, ctx.categories, TransactionType.EXPENSE, wallet.currency)
            val slices =
                summary.map { item ->
                    CategorySlice(
                        categoryId = item.categoryId,
                        name = item.name,
                        icon = ctx.categories.firstOrNull { it.id == item.categoryId }?.icon ?: "📦",
                        color = colorOf(item.categoryId, ctx.categories),
                        amount = item.total,
                        percent = item.percent,
                    )
                }
            val periodTotal = Money.sum(summary.map { it.total }, wallet.currency)
            val trend = Budget.summarizeWeeklyTrend(wallet, transactions, ctx.settings.weekStartDay, referenceDate, weeksCount = 8)
            return StatsUiState(
                isLoading = false,
                currency = wallet.currency,
                period = ctx.period,
                periodTotal = periodTotal,
                categorySlices = slices,
                trendPoints = trend,
            )
        }
    }
