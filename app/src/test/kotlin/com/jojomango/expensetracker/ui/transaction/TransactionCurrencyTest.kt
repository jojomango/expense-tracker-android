package com.jojomango.expensetracker.ui.transaction

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.5.1～T8.5.5（UI-SPEC.md §15.2）：記帳頁選幣別。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class TransactionCurrencyTest {
    @get:Rule
    val compose = createComposeRule()

    private val cebu = Wallet("cebu", "宿霧", "PHP", BudgetMode.WEEKLY, 1_000_000L)

    private fun tx(currency: String) =
        Transaction(
            id = "t-$currency",
            walletId = cebu.id,
            type = TransactionType.EXPENSE,
            amount = 4_000,
            currency = currency,
            categoryId = null,
            date = LocalDate(2026, 11, 11),
            createdAt = Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

    @Test
    fun `T8_5_1 新增交易用錢包的預設幣別，上一筆用 USD 也一樣`() {
        assertEquals("PHP", initialCurrency(cebu, existing = null))
    }

    @Test
    fun `T8_5_5 編輯一筆 USD 交易，初始幣別為 USD`() {
        assertEquals("USD", initialCurrency(cebu, existing = tx("USD")))
    }

    @Test
    fun `T8_5_2 幣別列表標題「選擇幣別」，第一列是預設幣別，接著是錢包用過的其他幣別`() {
        var picked: String? = null
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                CurrencySheetList(codes = listOf("PHP", "USD", "TWD"), selected = "PHP", onSelect = { picked = it })
            }
        }
        compose.onNodeWithText("選擇幣別").assertIsDisplayed()
        val rows = compose.onAllNodesWithTag(CURRENCY_SHEET_ROW_TAG)
        rows[0].assertTextContains("菲律賓披索 · PHP · ₱")
        rows[1].assertTextContains("美元 · USD · $")
        compose.onNodeWithText("美元 · USD · $").performClick()
        assertEquals("USD", picked)
    }

    @Test
    fun `T8_5_2 金額區的幣別可點，無障礙描述唸出名稱`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                CurrencyButton(walletName = "宿霧", currency = "USD", onClick = {})
            }
        }
        compose.onNodeWithContentDescription("幣別：美元，點兩下更換").assertIsDisplayed()
    }

    @Test
    fun `T8_5_3 選 USD 後輸入 40 記成 40 美元（測案寫 4000，見 PR「需要人類決策」）`() {
        val state = AddEditTransactionUiState(wallet = cebu, currency = "USD", amountDigits = "40")
        assertEquals(4_000L, state.amount)
    }

    @Test
    fun `T8_5_4 已輸入數字後把 PHP 換成 JPY，數字保留、依無小數重新換算`() {
        val php = AddEditTransactionUiState(wallet = cebu, currency = "PHP", amountDigits = "1200")
        assertEquals(120_000L, php.amount)
        val jpy = php.copy(currency = "JPY")
        assertEquals("1200", jpy.amountDigits)
        assertEquals(1_200L, jpy.amount)
    }
}
