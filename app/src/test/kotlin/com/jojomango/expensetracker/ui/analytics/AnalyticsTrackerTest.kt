package com.jojomango.expensetracker.ui.analytics

import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T9.1 — 分析開關與事件（SPEC.md P3、§3.7）。 */
class AnalyticsTrackerTest {
    private class FakeSink : AnalyticsSink {
        val logged = mutableListOf<Pair<String, Map<String, Any>>>()
        val collectionEnabledCalls = mutableListOf<Boolean>()

        override fun log(
            name: String,
            params: Map<String, Any>,
        ) {
            logged += name to params
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            collectionEnabledCalls += enabled
        }
    }

    private class FakeConsent(
        enabled: Boolean = AnalyticsConsent.DEFAULT,
    ) : AnalyticsConsent {
        override var enabled: Boolean = enabled
    }

    private val secretTransaction =
        Transaction(
            id = "t1",
            walletId = "w-日常",
            type = TransactionType.EXPENSE,
            amount = 12345,
            categoryId = "c-咖啡",
            date = LocalDate(2026, 8, 12),
            note = "祕密",
            createdAt = Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

    @Test
    @DisplayName("T9.1.1 — 首次安裝的預設值：分析開關為關閉")
    fun `T9 1 1`() {
        assertFalse(AnalyticsConsent.DEFAULT)
    }

    @Test
    @DisplayName("T9.1.2 — 開關關閉時送出任何事件都不轉送給 SDK")
    fun `T9 1 2`() {
        val sink = FakeSink()
        val tracker = AnalyticsTracker(FakeConsent(enabled = false), sink)
        tracker.track(AnalyticsEvent.BackupExported)
        tracker.track(AnalyticsEvent.TransactionSaved.from(secretTransaction, isEdit = false))
        assertTrue(sink.logged.isEmpty())
    }

    @Test
    @DisplayName("T9.1.3 — 開關開啟後送 transaction_saved，參數只有 type、is_edit")
    fun `T9 1 3`() {
        val sink = FakeSink()
        val tracker = AnalyticsTracker(FakeConsent(enabled = true), sink)
        tracker.track(AnalyticsEvent.TransactionSaved.from(secretTransaction, isEdit = true))
        val (name, params) = sink.logged.single()
        assertEquals("transaction_saved", name)
        assertEquals(mapOf("type" to "expense", "is_edit" to true), params)
    }

    @Test
    @DisplayName("T9.1.4 — 參數值不得出現金額、備註、分類、錢包、幣別")
    fun `T9 1 4`() {
        val sink = FakeSink()
        val tracker = AnalyticsTracker(FakeConsent(enabled = true), sink)
        tracker.track(AnalyticsEvent.TransactionSaved.from(secretTransaction, isEdit = false))
        tracker.track(AnalyticsEvent.WalletCreated(BudgetMode.WEEKLY))
        val everything = sink.logged.joinToString { (n, p) -> n + p.keys.joinToString() + p.values.joinToString() }
        for (secret in listOf("12345", "祕密", "咖啡", "日常", "JPY")) {
            assertFalse(everything.contains(secret), "事件裡出現了「$secret」：$everything")
        }
    }

    @Test
    @DisplayName("T9.1.5 — 每個事件名稱都在 SPEC.md §3.7 表列清單內（其他名稱由 sealed 型別在編譯期擋下）")
    fun `T9 1 5`() {
        val specEvents =
            setOf(
                "screen_view",
                "transaction_saved",
                "wallet_created",
                "stats_period_switched",
                "backup_exported",
                "backup_imported",
                "theme_changed",
                "donation_section_opened",
                "donation_completed",
            )
        assertEquals(specEvents, AnalyticsEvent.allEventNames)
    }

    @Test
    @DisplayName("T9.1.6 — 開啟後再關閉：之後的事件都是 no-op，並呼叫 SDK 停用收集")
    fun `T9 1 6`() {
        val sink = FakeSink()
        val consent = FakeConsent(enabled = false)
        val tracker = AnalyticsTracker(consent, sink)
        tracker.setEnabled(true)
        tracker.track(AnalyticsEvent.BackupExported)
        tracker.setEnabled(false)
        tracker.track(AnalyticsEvent.BackupExported)
        assertEquals(1, sink.logged.size)
        assertEquals(listOf(true, false), sink.collectionEnabledCalls)
        assertFalse(consent.enabled)
    }

    @Test
    @DisplayName("啟動時就把 SDK 的收集狀態對齊目前的開關（不只在切換時才通知）")
    fun `syncs collection state on start`() {
        val sink = FakeSink()
        AnalyticsTracker(FakeConsent(enabled = false), sink).syncCollectionState()
        assertEquals(listOf(false), sink.collectionEnabledCalls)
    }
}
