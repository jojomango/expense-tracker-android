package com.jojomango.expensetracker.ui.analytics

import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.ImportMode
import com.jojomango.expensetracker.domain.Theme
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.ui.donation.DonationTier
import com.jojomango.expensetracker.ui.navigation.Routes
import com.jojomango.expensetracker.ui.stats.StatsPeriod

/**
 * SPEC.md §3.7 的事件白名單。新增事件**一定要**加一個值到這裡，也就是一定要先改 SPEC 的表
 * ——T9.1.5 會比對這份清單跟 SPEC。
 */
enum class AnalyticsEventName(
    val value: String,
) {
    SCREEN_VIEW("screen_view"),
    TRANSACTION_SAVED("transaction_saved"),
    WALLET_CREATED("wallet_created"),
    STATS_PERIOD_SWITCHED("stats_period_switched"),
    BACKUP_EXPORTED("backup_exported"),
    BACKUP_IMPORTED("backup_imported"),
    THEME_CHANGED("theme_changed"),
    DONATION_SECTION_OPENED("donation_section_opened"),
    DONATION_COMPLETED("donation_completed"),
}

/** 畫面名稱只能是這些固定值（SPEC.md §3.7 `screen_view` 的參數）。 */
enum class AnalyticsScreen(
    val value: String,
) {
    HOME("home"),
    STATS("stats"),
    ADD_TRANSACTION("add_transaction"),
    EDIT_TRANSACTION("edit_transaction"),
    SETTINGS("settings"),
    WALLET_MANAGEMENT("wallet_management"),
    CATEGORY_MANAGEMENT("category_management"),
}

/**
 * 傳進來的是 NavGraph 的路由「樣板」（`transaction/edit/{transactionId}`），不是實際路徑，
 * 而且只回傳固定的 enum——路由參數（交易 id 等）沒有任何機會進到事件裡。
 */
fun analyticsScreenFor(routePattern: String?): AnalyticsScreen? =
    when (routePattern) {
        Routes.HOME -> AnalyticsScreen.HOME
        Routes.STATS -> AnalyticsScreen.STATS
        Routes.SETTINGS -> AnalyticsScreen.SETTINGS
        Routes.ADD_TRANSACTION -> AnalyticsScreen.ADD_TRANSACTION
        Routes.EDIT_TRANSACTION_PATTERN -> AnalyticsScreen.EDIT_TRANSACTION
        Routes.WALLET_MANAGEMENT -> AnalyticsScreen.WALLET_MANAGEMENT
        Routes.CATEGORY_MANAGEMENT -> AnalyticsScreen.CATEGORY_MANAGEMENT
        else -> null
    }

/**
 * 能送出的事件只有這些子型別——想送清單以外的東西，型別根本建不出來（T9.1.5）。
 * 參數值一律是 enum 轉出來的固定字串或布林，**沒有任何地方能把金額、分類名稱、備註、
 * 錢包名稱、幣別放進來**（SPEC.md P3 第 1 點；T9.1.4）。
 */
sealed interface AnalyticsEvent {
    val name: AnalyticsEventName
    val params: Map<String, Any> get() = emptyMap()

    data class ScreenView(
        val screen: AnalyticsScreen,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.SCREEN_VIEW
        override val params get() = mapOf("screen_name" to screen.value)
    }

    data class TransactionSaved(
        val type: TransactionType,
        val isEdit: Boolean,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.TRANSACTION_SAVED
        override val params get() = mapOf("type" to type.name.lowercase(), "is_edit" to isEdit)

        companion object {
            /** 呼叫端手上通常是整筆交易；這裡只挑出允許的欄位，其餘（金額、分類、備註…）丟掉。 */
            fun from(
                transaction: Transaction,
                isEdit: Boolean,
            ) = TransactionSaved(transaction.type, isEdit)
        }
    }

    data class WalletCreated(
        val budgetMode: BudgetMode,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.WALLET_CREATED
        override val params get() = mapOf("budget_mode" to budgetMode.name.lowercase())
    }

    data class StatsPeriodSwitched(
        val period: StatsPeriod,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.STATS_PERIOD_SWITCHED
        override val params get() = mapOf("period" to period.name.lowercase())
    }

    data object BackupExported : AnalyticsEvent {
        override val name = AnalyticsEventName.BACKUP_EXPORTED
    }

    data class BackupImported(
        val mode: ImportMode,
        val success: Boolean,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.BACKUP_IMPORTED
        override val params get() = mapOf("mode" to mode.name.lowercase(), "result" to if (success) "success" else "failed")
    }

    data class ThemeChanged(
        val theme: Theme,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.THEME_CHANGED
        override val params get() = mapOf("theme" to theme.name.lowercase())
    }

    data object DonationSectionOpened : AnalyticsEvent {
        override val name = AnalyticsEventName.DONATION_SECTION_OPENED
    }

    data class DonationCompleted(
        val tier: DonationTier,
    ) : AnalyticsEvent {
        override val name = AnalyticsEventName.DONATION_COMPLETED
        override val params get() = mapOf("tier" to tier.analyticsValue)
    }

    companion object {
        val allEventNames: Set<String> get() = AnalyticsEventName.entries.mapTo(mutableSetOf()) { it.value }
    }
}

/** 分析與當機回報 SDK 的薄轉接層（Firebase 實作見 [FirebaseAnalyticsSink]）。 */
interface AnalyticsSink {
    fun log(
        name: String,
        params: Map<String, Any>,
    )

    /** 同時控制分析與當機回報；關閉時 SDK 不得再送出任何東西。 */
    fun setCollectionEnabled(enabled: Boolean)
}

/** 「協助改善 App」開關的狀態，存在本機。 */
interface AnalyticsConsent {
    var enabled: Boolean

    companion object {
        /** SPEC.md §3.7、D8：預設關閉。 */
        const val DEFAULT = false
    }
}

/**
 * 全 App 唯一送事件的入口（SPEC.md §6 Phase 9）。開關關閉時 [track] 一律是 no-op（T9.1.2），
 * 所以呼叫端不需要自己判斷開關。
 */
class AnalyticsTracker(
    private val consent: AnalyticsConsent,
    private val sink: AnalyticsSink,
) {
    val isEnabled: Boolean get() = consent.enabled

    fun track(event: AnalyticsEvent) {
        if (consent.enabled) sink.log(event.name.value, event.params)
    }

    fun setEnabled(enabled: Boolean) {
        consent.enabled = enabled
        sink.setCollectionEnabled(enabled)
    }

    /** App 啟動時呼叫一次，讓 SDK 的收集狀態跟使用者的選擇一致。 */
    fun syncCollectionState() {
        sink.setCollectionEnabled(consent.enabled)
    }
}
