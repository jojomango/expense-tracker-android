package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T7.1 — 金額輸入位數限制。 */
class TransactionInputTest {
    @Test
    @DisplayName("T7.1.1 — \"\" 按 0 -> \"0\"")
    fun `T7 1 1`() {
        assertEquals("0", appendDigit("", "0"))
    }

    @Test
    @DisplayName("T7.1.2 — \"0\" 按 5 -> \"5\"（去前導 0）")
    fun `T7 1 2`() {
        assertEquals("5", appendDigit("0", "5"))
    }

    @Test
    @DisplayName("T7.1.3 — \"0\" 按 00 -> \"0\"（不產生 \"000\"）")
    fun `T7 1 3`() {
        assertEquals("0", appendDigit("0", "00"))
    }

    @Test
    @DisplayName("T7.1.4 — \"12345678\" 按 9 -> 不變（達 8 位上限）")
    fun `T7 1 4`() {
        assertEquals("12345678", appendDigit("12345678", "9"))
    }

    @Test
    @DisplayName("T7.1.5 — \"1234567\" 按 00 -> 不變（會超過 8 位）")
    fun `T7 1 5`() {
        assertEquals("1234567", appendDigit("1234567", "00"))
    }

    @Test
    @DisplayName("T7.1.6 — \"180\" 按 ⌫ -> \"18\"")
    fun `T7 1 6`() {
        assertEquals("18", deleteDigit("180"))
    }

    @Test
    @DisplayName("T7.1.7 — \"\" 按 ⌫ -> \"\"")
    fun `T7 1 7`() {
        assertEquals("", deleteDigit(""))
    }

    @Test
    @DisplayName("一般累加：\"1\" 按 2 -> \"12\"")
    fun `normal accumulation`() {
        assertEquals("12", appendDigit("1", "2"))
    }
}
