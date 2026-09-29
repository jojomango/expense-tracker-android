package com.jojomango.expensetracker.ui.home

import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Wallet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 首頁要顯示哪個錢包。非 TESTCASES.md 契約項目——Phase 7 實作 replace 匯入時發現：
 * 匯入會把現有錢包整批換掉，如果「使用者之前選的錢包」已經不存在，不能照用那個 id，
 * 否則首頁 currentWallet 變成 null、錢包清單又不是空的，兩個分支都不進，畫面一片空白。
 */
class CurrentWalletResolutionTest {
    private fun wallet(
        id: String,
        archived: Boolean = false,
    ) = Wallet(id = id, name = id, currency = "TWD", budgetMode = BudgetMode.NONE, budgetAmount = null, archived = archived)

    private val wallets = listOf(wallet("a"), wallet("b"), wallet("c"))

    @Test
    @DisplayName("手動選過的錢包優先於設定的預設錢包")
    fun `selected wins`() {
        assertEquals("b", resolveCurrentWalletId(wallets, selectedId = "b", defaultWalletId = "c"))
    }

    @Test
    @DisplayName("沒手動選過就用設定的預設錢包")
    fun `default when nothing selected`() {
        assertEquals("c", resolveCurrentWalletId(wallets, selectedId = null, defaultWalletId = "c"))
    }

    @Test
    @DisplayName("都沒有就用第一個未封存的錢包")
    fun `first active fallback`() {
        val list = listOf(wallet("x", archived = true), wallet("y"))
        assertEquals("y", resolveCurrentWalletId(list, selectedId = null, defaultWalletId = null))
    }

    @Test
    @DisplayName("手動選過的錢包已經不存在（例如被 replace 匯入換掉），改用預設錢包")
    fun `stale selection falls back to default`() {
        assertEquals("c", resolveCurrentWalletId(wallets, selectedId = "gone", defaultWalletId = "c"))
    }

    @Test
    @DisplayName("選過的跟預設的都不存在，退回第一個未封存的錢包，而不是回傳一個不存在的 id")
    fun `stale selection and stale default fall back to first active`() {
        assertEquals("a", resolveCurrentWalletId(wallets, selectedId = "gone", defaultWalletId = "also-gone"))
    }

    @Test
    @DisplayName("沒有任何錢包回傳 null（交給引導頁）")
    fun `no wallets`() {
        assertNull(resolveCurrentWalletId(emptyList(), selectedId = "a", defaultWalletId = "a"))
    }
}
