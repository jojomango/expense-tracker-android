package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T1 — Money. Domain-only, no mocks (see CLAUDE.md 禁令 2).
 */
class MoneyTest {
    // ---- T1.1 建構與驗證 ----

    @Test
    @DisplayName("T1.1.1 — Money.of(10050, TWD) 內部值 10050，顯示 NT\$100.50")
    fun `T1 1 1`() {
        val m = Money.of(10050, "TWD")
        assertEquals(10050L, m.amount)
        assertEquals("NT$100.50", m.format())
    }

    @Test
    @DisplayName("T1.1.2 — Money.of(1000, JPY) 顯示 ¥1,000（0 位小數）")
    fun `T1 1 2`() {
        val m = Money.of(1000, "JPY")
        assertEquals("¥1,000", m.format())
    }

    @Test
    @DisplayName("T1.1.3 — Money.of(0, TWD) 合法，顯示 NT\$0.00")
    fun `T1 1 3`() {
        val m = Money.of(0, "TWD")
        assertEquals("NT$0.00", m.format())
    }

    @Test
    @DisplayName("T1.1.4 — Money.of(-500, TWD) 合法（餘額可為負），顯示 -NT\$5.00")
    fun `T1 1 4`() {
        val m = Money.of(-500, "TWD")
        assertEquals("-NT$5.00", m.format())
    }

    // T1.1.5：型別系統層級擋掉（amount 為 Long），編譯期即擋下，不需要 runtime 測試。

    @Test
    @DisplayName("T1.1.6 — Money.of(100, XXX) 未知幣別代碼，拋 IllegalArgumentException")
    fun `T1 1 6`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of(100, "XXX")
        }
    }

    @Test
    @DisplayName("T1.1.7 — Money.of(Long.MAX_VALUE / 2, TWD) 合法，不溢位")
    fun `T1 1 7`() {
        val m = Money.of(Long.MAX_VALUE / 2, "TWD")
        assertEquals(Long.MAX_VALUE / 2, m.amount)
    }

    // ---- T1.2 解析使用者輸入 ----

    @Test
    @DisplayName("T1.2.1 — parse(\"100\", TWD) -> 10000")
    fun `T1 2 1`() {
        assertEquals(10000L, Money.parse("100", "TWD").amount)
    }

    @Test
    @DisplayName("T1.2.2 — parse(\"100.5\", TWD) -> 10050")
    fun `T1 2 2`() {
        assertEquals(10050L, Money.parse("100.5", "TWD").amount)
    }

    @Test
    @DisplayName("T1.2.3 — parse(\"100.50\", TWD) -> 10050")
    fun `T1 2 3`() {
        assertEquals(10050L, Money.parse("100.50", "TWD").amount)
    }

    @Test
    @DisplayName("T1.2.4 — parse(\"100.567\", TWD) 超過小數位，拋錯")
    fun `T1 2 4`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.parse("100.567", "TWD")
        }
    }

    @Test
    @DisplayName("T1.2.5 — parse(\"1,234.56\", TWD) 容許千分位 -> 123456")
    fun `T1 2 5`() {
        assertEquals(123456L, Money.parse("1,234.56", "TWD").amount)
    }

    @Test
    @DisplayName("T1.2.6 — parse(\"\" / \"abc\" / \"-\", TWD) 皆拋錯")
    fun `T1 2 6`() {
        for (input in listOf("", "abc", "-")) {
            assertThrows(IllegalArgumentException::class.java) {
                Money.parse(input, "TWD")
            }
        }
    }

    @Test
    @DisplayName("T1.2.7 — parse(\"0\", TWD) -> 0，Money 合法")
    fun `T1 2 7`() {
        assertEquals(0L, Money.parse("0", "TWD").amount)
    }

    @Test
    @DisplayName("T1.2.8 — parse(\"100.5\", JPY) 0 位小數，拋錯")
    fun `T1 2 8`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.parse("100.5", "JPY")
        }
    }

    // ---- T1.3 運算 ----

    @Test
    @DisplayName("T1.3.1 — TWD(10050) + TWD(2550) = TWD(12600)")
    fun `T1 3 1`() {
        val result = Money.of(10050, "TWD").plus(Money.of(2550, "TWD"))
        assertEquals(Money.of(12600, "TWD"), result)
    }

    @Test
    @DisplayName("T1.3.2 — TWD(10000) - TWD(15000) = TWD(-5000)")
    fun `T1 3 2`() {
        val result = Money.of(10000, "TWD").minus(Money.of(15000, "TWD"))
        assertEquals(Money.of(-5000, "TWD"), result)
    }

    @Test
    @DisplayName("T1.3.3 — TWD(100) + JPY(100) 幣別不符，禁止跨幣運算")
    fun `T1 3 3`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of(100, "TWD").plus(Money.of(100, "JPY"))
        }
    }

    @Test
    @DisplayName("T1.3.4 — sum(emptyList(), TWD) = TWD(0)")
    fun `T1 3 4`() {
        assertEquals(Money.of(0, "TWD"), Money.sum(emptyList(), "TWD"))
    }

    @Test
    @DisplayName("T1.3.5 — 連續加 0.1（TWD）共 10 次，恰為 TWD(100)，無浮點誤差")
    fun `T1 3 5`() {
        var total = Money.of(0, "TWD")
        repeat(10) {
            total = total.plus(Money.parse("0.1", "TWD"))
        }
        assertEquals(Money.of(100, "TWD"), total)
    }

    @Test
    @DisplayName("T1.3.6 — TWD(10000).percentOf(TWD(30000)) = 33.33")
    fun `T1 3 6`() {
        val percent = Money.of(10000, "TWD").percentOf(Money.of(30000, "TWD"))
        assertEquals(33.33, percent, 0.0001)
    }

    @Test
    @DisplayName("T1.3.7 — percentOf 分母為 0，回傳 0.0，不得為 NaN/Infinity")
    fun `T1 3 7`() {
        val percent = Money.of(10000, "TWD").percentOf(Money.of(0, "TWD"))
        assertEquals(0.0, percent)
        assertFalse(percent.isNaN())
        assertFalse(percent.isInfinite())
    }
}
