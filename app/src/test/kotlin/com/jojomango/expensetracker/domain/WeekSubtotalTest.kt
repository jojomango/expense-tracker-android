package com.jojomango.expensetracker.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T2.5.6～T2.5.8 — 首頁週分組小計＝收入減支出的淨額（UI-SPEC §4.3 v2.6）。 */
class WeekSubtotalTest {
    private var n = 0

    private fun tx(
        type: TransactionType,
        amount: Long,
    ) = Transaction(
        id = "t${n++}",
        walletId = "w",
        type = type,
        amount = amount,
        categoryId = null,
        date = LocalDate(2026, 10, 21),
        note = null,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    @Test
    @DisplayName("T2.5.6 — 支出 300、200，收入 1000：小計 +500（收入減支出）")
    fun `T2 5 6`() {
        val net =
            Budget.netTotal(
                listOf(tx(TransactionType.EXPENSE, 30000), tx(TransactionType.EXPENSE, 20000), tx(TransactionType.INCOME, 100000)),
                "TWD",
            )
        assertEquals(50000, net.amount)
        assertEquals("+NT$500.00", net.formatSigned())
    }

    @Test
    @DisplayName("T2.5.7 — 只有支出 120、45：小計 -165")
    fun `T2 5 7`() {
        val net = Budget.netTotal(listOf(tx(TransactionType.EXPENSE, 12000), tx(TransactionType.EXPENSE, 4500)), "TWD")
        assertEquals(-16500, net.amount)
        assertEquals("-NT$165.00", net.formatSigned())
    }

    @Test
    @DisplayName("T2.5.8 — 支出 500、收入 500：小計 0，顯示時不帶正負號")
    fun `T2 5 8`() {
        val net = Budget.netTotal(listOf(tx(TransactionType.EXPENSE, 50000), tx(TransactionType.INCOME, 50000)), "TWD")
        assertEquals(0, net.amount)
        assertEquals("NT$0.00", net.formatSigned())
    }
}
