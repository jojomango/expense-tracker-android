package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.jojomango.expensetracker.domain.Budget
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.MultiCurrency
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.5.3、T8.5.6、T8.5.7、T8.5.11（UI-SPEC.md §15.3、§15.5）：首頁的多幣別顯示。
 * 測案寫「US$」，但內建幣別表的美元符號一直是「$」，這裡照現行符號（見 PR「需要人類決策」）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class HomeMultiCurrencyTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
    private val weekStart = DayOfWeek.MONDAY
    private val cebu = Wallet("cebu", "宿霧", "PHP", BudgetMode.WEEKLY, 1_000_000L)
    private var n = 0

    private fun tx(
        amount: Long,
        currency: String,
    ) = Transaction(
        id = "t${n++}",
        walletId = cebu.id,
        type = TransactionType.EXPENSE,
        amount = amount,
        currency = currency,
        categoryId = null,
        date = today,
        createdAt = Instant.fromEpochMilliseconds(n.toLong()),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun showHome(items: List<Transaction>) {
        val weekly = Budget.calculateWeeklyBalance(cebu, items, weekStart, today)
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                HomeContent(
                    state =
                        HomeUiState(
                            isLoading = false,
                            wallets = listOf(cebu),
                            currentWallet = cebu,
                            weekStartDay = weekStart,
                            weeklyBalance = weekly,
                            weeklyExpenseTotal = Budget.calculateWeeklyExpenseTotal(cebu, items, weekStart, today),
                            daysLeftInWeek = Budget.daysLeftInWeek(weekStart, today),
                            otherCurrencyExpenses = MultiCurrency.otherCurrencyExpenses(cebu, items, weekStart, today),
                            transactionGroups = Week.groupByWeek(items, weekStart) { it.date },
                        ),
                    padding = PaddingValues(),
                    onAddTransaction = {},
                    onEditTransaction = {},
                    onOpenSettings = {},
                    onSwitchWalletRequested = {},
                    onDelete = {},
                    today = today,
                )
            }
        }
    }

    @Test
    fun `T8_5_3 與 T8_5_6 交易列用自己的幣別；預算卡出現「另有」；小計並排、預設幣別在前`() {
        showHome(listOf(tx(120_000, "PHP"), tx(4_000, "USD")))
        compose.onNodeWithText("₱8,800.00").assertIsDisplayed()
        compose.onNodeWithText("另有 $40.00").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("-$40.00"))
        compose.onNodeWithText("-$40.00").assertIsDisplayed()
        compose.onNodeWithContentDescription("今天 · 11/11 週三，淨額 -₱1,200.00 · -$40.00").assertExists()
    }

    @Test
    fun `T8_5_7 錢包只有一種幣別時沒有「另有」`() {
        showHome(listOf(tx(120_000, "PHP")))
        compose.onNodeWithText("另有", substring = true).assertDoesNotExist()
    }

    @Test
    fun `T8_5_11 錢包切換 sheet 的餘額只算預設幣別`() {
        val text = formatWalletBalance(cebu, listOf(tx(120_000, "PHP"), tx(4_000, "USD")), weekStart, today)
        assertEquals(Money.of(880_000, "PHP").format(), text)
    }
}
