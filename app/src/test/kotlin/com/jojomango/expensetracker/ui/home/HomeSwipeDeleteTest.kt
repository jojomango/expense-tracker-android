package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.DateRange
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.WeekGroup
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeSwipeDeleteTest {
    @get:Rule
    val compose = createComposeRule()

    private val wallet = Wallet(id = "w", name = "日常", currency = "TWD", budgetMode = BudgetMode.NONE, budgetAmount = null)
    private val week = DateRange(LocalDate(2026, 10, 26), LocalDate(2026, 11, 1))

    private fun tx(
        id: String,
        note: String,
    ) = Transaction(
        id = id,
        walletId = wallet.id,
        type = TransactionType.EXPENSE,
        amount = 100,
        categoryId = null,
        date = LocalDate(2026, 10, 27),
        note = note,
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private var items by mutableStateOf(listOf<Transaction>())

    private fun showHome() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                HomeContent(
                    state =
                        HomeUiState(
                            isLoading = false,
                            wallets = listOf(wallet),
                            currentWallet = wallet,
                            transactionGroups = listOf(WeekGroup(week, items)),
                        ),
                    padding = PaddingValues(),
                    onAddTransaction = {},
                    onEditTransaction = {},
                    onOpenSettings = {},
                    onSwitchWalletRequested = {},
                    onDelete = { deleted -> items = items.filterNot { it.id == deleted.id } },
                )
            }
        }
    }

    @Test
    fun `T8_2_6 左滑刪除第一列後，補上來的那一列要正常顯示，不留下空白列`() {
        items = listOf(tx("a", "甲甲"), tx("b", "乙乙"), tx("c", "丙丙"))
        showHome()

        compose.onAllNodesWithTag("transaction-row")[0].performTouchInput { swipeLeft() }
        compose.waitForIdle()

        compose.onAllNodesWithTag("transaction-row").assertCountEquals(2)
        compose.onNodeWithText("乙乙", substring = true).assertIsDisplayed()
        compose.onNodeWithText("丙丙", substring = true).assertIsDisplayed()
    }

    @Test
    fun `T8_2_7 刪除後還原，那一列回到原位且正常顯示`() {
        val first = tx("a", "甲甲")
        items = listOf(first, tx("b", "乙乙"))
        showHome()

        compose.onAllNodesWithTag("transaction-row")[0].performTouchInput { swipeLeft() }
        compose.waitForIdle()
        items = listOf(first) + items
        compose.waitForIdle()

        compose.onAllNodesWithTag("transaction-row").assertCountEquals(2)
        compose.onNodeWithText("甲甲", substring = true).assertIsDisplayed()
        compose.onNodeWithText("乙乙", substring = true).assertIsDisplayed()
    }
}
