package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Locale

/** TESTCASES.md T10.4.6 — 金額格式兩種語言相同（SPEC.md §3.9：金額格式不翻譯）。 */
class MoneyLocaleTest {
    private fun formatUnder(
        locale: Locale,
        money: Money,
    ): String {
        val previous = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            return money.format()
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    @DisplayName("T10.4.6 — NT$1,234.00 在繁中與英文（以及小數點是逗號的德文）下都一樣")
    fun `T10 4 6`() {
        val twd = Money.of(123400, "TWD")
        for (locale in listOf(Locale.TAIWAN, Locale.US, Locale.GERMANY)) {
            assertEquals("NT$1,234.00", formatUnder(locale, twd), "locale=$locale")
        }
        assertEquals(formatUnder(Locale.TAIWAN, Money.of(145000, "JPY")), formatUnder(Locale.US, Money.of(145000, "JPY")))
    }
}
