package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SPEC.md §3.1 Wallet 建構規則 + 「不可刪除最後一個錢包」（見 TESTCASES.md
 * T4.1.3，實際透過 Room 走一趟的整合測試在 `androidTest`）。
 */
class WalletTest {
    @Test
    @DisplayName("budgetMode = NONE 且 budgetAmount = null，合法")
    fun `none mode with null budget is valid`() {
        Wallet("w1", "日常", "TWD", BudgetMode.NONE, null)
    }

    @Test
    @DisplayName("budgetMode = NONE 但 budgetAmount 非 null，拋錯")
    fun `none mode with non-null budget throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Wallet("w1", "日常", "TWD", BudgetMode.NONE, 100)
        }
    }

    @Test
    @DisplayName("budgetMode = WEEKLY 且 budgetAmount 為正數，合法")
    fun `weekly mode with positive budget is valid`() {
        Wallet("w1", "日常", "TWD", BudgetMode.WEEKLY, 300000)
    }

    @Test
    @DisplayName("budgetMode = TOTAL 且 budgetAmount = 0，合法（T3.2.6 邊界）")
    fun `total mode with zero budget is valid`() {
        Wallet("w1", "旅遊", "JPY", BudgetMode.TOTAL, 0)
    }

    @Test
    @DisplayName("budgetMode = WEEKLY 但 budgetAmount 為 null，拋錯")
    fun `weekly mode with null budget throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Wallet("w1", "日常", "TWD", BudgetMode.WEEKLY, null)
        }
    }

    @Test
    @DisplayName("budgetMode = TOTAL 但 budgetAmount 為負數，拋錯")
    fun `total mode with negative budget throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Wallet("w1", "旅遊", "JPY", BudgetMode.TOTAL, -1)
        }
    }

    @Test
    @DisplayName("name 為空白，拋錯")
    fun `blank name throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            Wallet("w1", "  ", "TWD", BudgetMode.NONE, null)
        }
    }

    @Test
    @DisplayName("T4.1.3 — 只剩一個錢包時，assertCanDeleteWallet 拋 LastWalletException")
    fun `T4 1 3`() {
        assertThrows(LastWalletException::class.java) { assertCanDeleteWallet(1) }
        assertThrows(LastWalletException::class.java) { assertCanDeleteWallet(0) }
    }

    @Test
    @DisplayName("還有一個以上的錢包時，assertCanDeleteWallet 不拋錯")
    fun `can delete when more than one wallet remains`() {
        assertCanDeleteWallet(2)
    }
}
