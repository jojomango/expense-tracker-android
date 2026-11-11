@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.jojomango.expensetracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jojomango.expensetracker.domain.Budget
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.CategoryRepository
import com.jojomango.expensetracker.domain.DefaultCategoryKey
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Month
import com.jojomango.expensetracker.domain.MultiCurrency
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
import com.jojomango.expensetracker.ui.analytics.AnalyticsEvent
import com.jojomango.expensetracker.ui.analytics.AnalyticsTracker
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import javax.inject.Inject

enum class StatsPeriod { WEEK, MONTH }

/** 圓環圖的一段弧——[percent] 已經是 0~100 的百分比（`Budget.summarizeByCategory` 算好的）。 */
data class CategorySlice(
    val categoryId: String?,
    /** `null` = 未分類。名稱在畫面上依目前語言才決定（SPEC.md §3.9），ViewModel 不存文字。 */
    val category: Category?,
    val icon: String,
    val color: String,
    val amount: Money,
    val percent: Double,
)

data class StatsUiState(
    val isLoading: Boolean = true,
    val walletId: String? = null,
    /** 目前選取的幣別；圓環、圖例、趨勢都只看這個幣別（UI-SPEC.md §15.4）。 */
    val currency: String = "TWD",
    /** 錢包交易用到的幣別，兩種以上才顯示 chip 列。 */
    val currencies: List<String> = emptyList(),
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
    val selectedCurrency: SelectedCurrency?,
)

/** 記住是哪個錢包選的：切換錢包時回到預設選取（UI-SPEC.md §15.4）。 */
private data class SelectedCurrency(
    val walletId: String,
    val code: String,
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
        private val analyticsTracker: AnalyticsTracker,
    ) : ViewModel() {
        private val period = MutableStateFlow(StatsPeriod.WEEK)
        private val selectedCurrency = MutableStateFlow<SelectedCurrency?>(null)
        private var lastWalletId: String? = null

        private fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val uiState: StateFlow<StatsUiState> =
            combine(
                walletRepository.observeWallets(),
                categoryRepository.observeCategories(),
                settingsRepository.observe(),
                period,
                selectedCurrency,
            ) { wallets, categories, settings, period, selected ->
                val wallet =
                    wallets.firstOrNull { it.id == settings.defaultWalletId }
                        ?: wallets.firstOrNull { !it.archived }
                StatsContext(wallet, categories, settings, period, selected)
            }.flatMapLatest { ctx ->
                val wallet = ctx.wallet
                if (wallet == null) {
                    flowOf(StatsUiState(isLoading = false))
                } else {
                    // 切回原本的錢包也要從預設選取開始，不沿用上次點過的 chip（UI-SPEC.md §15.4）
                    if (wallet.id != lastWalletId) {
                        lastWalletId = wallet.id
                        selectedCurrency.value = null
                    }
                    transactionRepository.observeByWallet(wallet.id).map { transactions ->
                        buildStatsUiState(
                            wallet = wallet,
                            transactions = transactions,
                            categories = ctx.categories,
                            query =
                                StatsQuery(
                                    period = ctx.period,
                                    currency = ctx.selectedCurrency?.takeIf { it.walletId == wallet.id }?.code,
                                    weekStartDay = ctx.settings.weekStartDay,
                                    referenceDate = today(),
                                ),
                        )
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = StatsUiState(),
            )

        fun setPeriod(newPeriod: StatsPeriod) {
            if (period.value == newPeriod) return
            period.value = newPeriod
            analyticsTracker.track(AnalyticsEvent.StatsPeriodSwitched(newPeriod))
        }

        fun selectCurrency(code: String) {
            val walletId = uiState.value.walletId ?: return
            selectedCurrency.value = SelectedCurrency(walletId, code)
        }
    }

/** 要算哪一段：期間、幣別 chip（`null` = 沒點過）、週起始日與「今天」。 */
internal data class StatsQuery(
    val period: StatsPeriod,
    val currency: String?,
    val weekStartDay: DayOfWeek,
    val referenceDate: LocalDate,
)

/**
 * 統計頁的畫面狀態（UI-SPEC.md §6、§15.4）。[StatsQuery.currency] 是使用者點過的 chip；
 * 沒點過、或那個幣別在這個錢包沒有交易時，用 [MultiCurrency.statsCurrency] 的預設選取。
 */
internal fun buildStatsUiState(
    wallet: Wallet,
    transactions: List<Transaction>,
    categories: List<Category>,
    query: StatsQuery,
): StatsUiState {
    val weekStartDay = query.weekStartDay
    val referenceDate = query.referenceDate
    val currencies = MultiCurrency.currenciesUsed(transactions, wallet.currency)
    val currency = query.currency?.takeIf { it in currencies } ?: MultiCurrency.statsCurrency(transactions, wallet.currency)
    val range =
        when (query.period) {
            StatsPeriod.WEEK -> Week.rangeOf(referenceDate, weekStartDay)
            StatsPeriod.MONTH -> Month.rangeOf(referenceDate)
        }
    val summary =
        Budget.summarizeByCategory(
            transactions.filter { it.date in range },
            categories,
            TransactionType.EXPENSE,
            currency,
            NamesResolvedOnScreen,
        )
    val slices =
        summary.map { item ->
            val category = categories.firstOrNull { it.id == item.categoryId }
            CategorySlice(
                categoryId = item.categoryId,
                category = category,
                icon = category?.icon ?: "📦",
                color = colorOf(item.categoryId, categories),
                amount = item.total,
                percent = item.percent,
            )
        }
    // 趨勢圖沿用「錢包幣別」的計算，把錢包當成以選取幣別為預設的錢包來算
    val trend = Budget.summarizeWeeklyTrend(wallet.copy(currency = currency), transactions, weekStartDay, referenceDate, weeksCount = 8)
    return StatsUiState(
        isLoading = false,
        walletId = wallet.id,
        currency = currency,
        currencies = currencies,
        period = query.period,
        periodTotal = Money.sum(summary.map { it.total }, currency),
        categorySlices = slices,
        trendPoints = trend,
    )
}

/**
 * 彙總結果的 `name` 在這裡用不到：[CategorySlice] 存的是分類本身，名稱到畫面上才依目前語言決定。
 * 這裡給一個不含任何文字的實作，避免 ViewModel 留住舊語言的字串。
 */
private object NamesResolvedOnScreen : CategoryNames {
    override fun nameOf(key: DefaultCategoryKey): String = key.id

    override val uncategorized: String = ""
}
