package com.jojomango.expensetracker.ui.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T9.1.7 — 開關狀態存在本機，重新啟動後保留。 */
@RunWith(RobolectricTestRunner::class)
class PreferencesAnalyticsConsentTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `T9 1 7 — 首次安裝讀到預設的關閉`() {
        assertFalse(PreferencesAnalyticsConsent(context).enabled)
    }

    @Test
    fun `T9 1 7 — 開啟後換一個新實例（模擬重新啟動）仍是開啟`() {
        PreferencesAnalyticsConsent(context).enabled = true
        assertTrue(PreferencesAnalyticsConsent(context).enabled)

        PreferencesAnalyticsConsent(context).enabled = false
        assertFalse(PreferencesAnalyticsConsent(context).enabled)
    }
}
