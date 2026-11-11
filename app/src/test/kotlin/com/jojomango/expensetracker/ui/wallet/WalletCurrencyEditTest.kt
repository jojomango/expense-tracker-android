package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.5.9、T8.5.10（UI-SPEC.md §15.1）：編輯錢包時改預設幣別。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class WalletCurrencyEditTest {
    @get:Rule
    val compose = createComposeRule()

    private val editing =
        WalletEditUiState(
            isLoading = false,
            walletId = "cebu",
            name = "宿霧",
            currency = "PHP",
            budgetMode = BudgetMode.WEEKLY,
            budgetAmountDigits = "10000",
            originalCurrency = "PHP",
            originalBudgetAmountDigits = "10000",
        )

    @Test
    fun `T8_5_9 週預算錢包把 PHP 改成 USD：預算清空、要重新輸入、不能儲存；改回 PHP 恢復原金額`() {
        val usd = editing.withCurrency("USD")
        assertEquals("", usd.budgetAmountDigits)
        assertTrue(usd.currencyChangedWithBudget)
        assertFalse(usd.canSubmit)

        assertTrue(usd.copy(budgetAmountDigits = "500").canSubmit)

        val back = usd.withCurrency("PHP")
        assertEquals("10000", back.budgetAmountDigits)
        assertFalse(back.currencyChangedWithBudget)
        assertTrue(back.canSubmit)
    }

    @Test
    fun `T8_5_9 不設預算的錢包改幣別不需要重新輸入`() {
        val none = editing.copy(budgetMode = BudgetMode.NONE, budgetAmountDigits = "").withCurrency("USD")
        assertFalse(none.currencyChangedWithBudget)
        assertTrue(none.canSubmit)
    }

    @Test
    fun `T8_5_10 與 T8_5_9 編輯錢包時幣別欄下方有說明；改了幣別時預算欄下方提示重新輸入`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                Column {
                    WalletCurrencyNote(isEditing = true)
                    BudgetAmountField(digits = "", currencyChanged = true, onChange = {})
                }
            }
        }
        compose.onNodeWithText("改預設幣別不會改變已記錄交易的幣別。").assertIsDisplayed()
        compose.onNodeWithText("幣別改了，請重新輸入預算金額。").assertIsDisplayed()
    }

    @Test
    fun `T8_5_10 新增錢包時不顯示說明`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                WalletCurrencyNote(isEditing = false)
            }
        }
        compose.onNodeWithText("改預設幣別不會改變已記錄交易的幣別。").assertDoesNotExist()
    }
}
