package com.jojomango.expensetracker.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performScrollToIndex
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.domain.Week
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.2.9～T8.2.11（UI-SPEC §4.2／§4.3 v2.7，UX-006、UX-007）。 */
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class HomeListTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 10, 28)
    private val wallet = Wallet(id = "w", name = "日常", currency = "TWD", budgetMode = BudgetMode.NONE, budgetAmount = null)
    private var n = 0
    private var items by mutableStateOf(listOf<Transaction>())

    private fun tx(
        date: LocalDate,
        note: String? = null,
    ) = Transaction(
        id = "t${n++}",
        walletId = wallet.id,
        type = TransactionType.EXPENSE,
        amount = 33000,
        categoryId = null,
        date = date,
        note = note,
        createdAt = Instant.fromEpochMilliseconds(n.toLong()),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun showHome() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                HomeContent(
                    state =
                        HomeUiState(
                            isLoading = false,
                            wallets = listOf(wallet),
                            currentWallet = wallet,
                            weekStartDay = DayOfWeek.MONDAY,
                            transactionGroups = Week.groupByWeek(items, DayOfWeek.MONDAY) { it.date },
                        ),
                    padding = PaddingValues(),
                    onAddTransaction = {},
                    onEditTransaction = {},
                    onOpenSettings = {},
                    onSwitchWalletRequested = {},
                    onDelete = { deleted -> items = items.filterNot { it.id == deleted.id } },
                    today = today,
                )
            }
        }
    }

    @Test
    fun `T8_2_9 有交易的日子出現日小標題與當日小計，列的副標只有備註`() {
        items = listOf(tx(LocalDate(2026, 10, 27), note = "午餐"), tx(LocalDate(2026, 10, 26)))
        showHome()
        // 日小標題把日期與小計合成一句給 TalkBack（clearAndSetSemantics），所以用描述找
        compose.onNodeWithContentDescription("昨天 · 10/27 週二，淨額 -NT$330.00").assertIsDisplayed()
        compose.onNodeWithContentDescription("10/26 週一，淨額 -NT$330.00").assertIsDisplayed()
        compose.onNodeWithText("午餐").assertIsDisplayed()
        compose.onNodeWithText("10/27 · 午餐").assertDoesNotExist()
        compose.onNodeWithText("10/26").assertDoesNotExist()
    }

    @Test
    fun `T8_2_10 無障礙自訂動作刪除，效果同左滑刪除`() {
        items = listOf(tx(today, note = "甲"), tx(today, note = "乙"))
        showHome()
        compose.onAllNodesWithTag("transaction-row")[0].performCustomAccessibilityActionWithLabel("刪除")
        compose.waitForIdle()
        compose.onAllNodesWithTag("transaction-row").assertCountEquals(1)
    }

    @Test
    fun `T8_2_11 捲動後預算卡捲出畫面，標題區仍可見`() {
        items = (0 until 30).map { tx(today.minus(it, DateTimeUnit.DAY)) }
        showHome()
        compose.onNodeWithText("本週支出").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToIndex(20)
        compose.onNodeWithText("本週支出").assertIsNotDisplayed()
        compose.onNodeWithText("日常").assertIsDisplayed()
    }
}
