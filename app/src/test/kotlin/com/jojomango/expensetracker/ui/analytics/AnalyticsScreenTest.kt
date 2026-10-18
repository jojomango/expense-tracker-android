package com.jojomango.expensetracker.ui.analytics

import com.jojomango.expensetracker.ui.navigation.Routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AnalyticsScreenTest {
    @Test
    @DisplayName("T9.1.4 — screen_view 只送固定的畫面名稱：路由裡的交易 id 不會被送出去")
    fun `routes map to fixed screen names`() {
        assertEquals(AnalyticsScreen.HOME, analyticsScreenFor(Routes.HOME))
        assertEquals(AnalyticsScreen.STATS, analyticsScreenFor(Routes.STATS))
        assertEquals(AnalyticsScreen.SETTINGS, analyticsScreenFor(Routes.SETTINGS))
        assertEquals(AnalyticsScreen.ADD_TRANSACTION, analyticsScreenFor(Routes.ADD_TRANSACTION))
        assertEquals(AnalyticsScreen.EDIT_TRANSACTION, analyticsScreenFor(Routes.EDIT_TRANSACTION_PATTERN))
        assertEquals(AnalyticsScreen.WALLET_MANAGEMENT, analyticsScreenFor(Routes.WALLET_MANAGEMENT))
        assertEquals(AnalyticsScreen.CATEGORY_MANAGEMENT, analyticsScreenFor(Routes.CATEGORY_MANAGEMENT))
    }

    @Test
    @DisplayName("表上沒有的畫面（錢包／分類編輯）不送 screen_view")
    fun `unlisted routes are not tracked`() {
        assertNull(analyticsScreenFor(Routes.WALLET_EDIT_PATTERN))
        assertNull(analyticsScreenFor(Routes.CATEGORY_NEW))
        assertNull(analyticsScreenFor(null))
    }
}
