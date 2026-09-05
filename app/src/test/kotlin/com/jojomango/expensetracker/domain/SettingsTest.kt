package com.jojomango.expensetracker.domain

import kotlinx.datetime.DayOfWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** SPEC.md §3.5 全域設定的預設值。 */
class SettingsTest {
    @Test
    @DisplayName("預設值：weekStartDay=一、theme=SYSTEM、defaultWalletId=null")
    fun `defaults match SPEC md`() {
        val settings = Settings()
        assertEquals(DayOfWeek.MONDAY, settings.weekStartDay)
        assertEquals(Theme.SYSTEM, settings.theme)
        assertNull(settings.defaultWalletId)
    }

    @Test
    @DisplayName("Theme 三種取值")
    fun `theme has three values`() {
        assertEquals(setOf(Theme.LIGHT, Theme.DARK, Theme.SYSTEM), Theme.entries.toSet())
    }

    @Test
    @DisplayName("可自訂 weekStartDay 與 defaultWalletId")
    fun `custom settings`() {
        val settings = Settings(weekStartDay = DayOfWeek.SUNDAY, theme = Theme.DARK, defaultWalletId = "w1")
        assertEquals(DayOfWeek.SUNDAY, settings.weekStartDay)
        assertEquals(Theme.DARK, settings.theme)
        assertEquals("w1", settings.defaultWalletId)
    }
}
