package com.jojomango.expensetracker.ui.wallet

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Settings
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
import com.jojomango.expensetracker.ui.home.HomeContext
import com.jojomango.expensetracker.ui.home.buildHomeUiState
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.6.1、T8.6.3～T8.6.5、T8.6.7、T8.6.9（UI-SPEC.md §11.1）：封存錢包檢視頁。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class ArchivedWalletViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 14)
    private val wallet =
        Wallet(id = "old", name = "舊帳", currency = "TWD", budgetMode = BudgetMode.WEEKLY, budgetAmount = 500_000, archived = true)

    private fun tx(id: String) =
        Transaction(
            id = id,
            walletId = wallet.id,
            type = TransactionType.EXPENSE,
            amount = 18_000,
            currency = "TWD",
            categoryId = null,
            date = LocalDate(2026, 11, 12),
            note = "便當$id",
            createdAt = Instant.fromEpochMilliseconds(0),
            updatedAt = Instant.fromEpochMilliseconds(0),
        )

    private val events = mutableListOf<String>()

    private fun show(
        transactions: List<Transaction>,
        pendingDeleteCount: Int? = null,
    ) {
        val home = buildHomeUiState(wallet, HomeContext(listOf(wallet), emptyList(), Settings()), transactions, today)
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                ArchivedWalletContent(
                    state = ArchivedWalletUiState(home = home, pendingDeleteTransactionCount = pendingDeleteCount),
                    onBack = { events += "back" },
                    onUnarchive = { events += "unarchive" },
                    onRequestDelete = { events += "requestDelete" },
                    onConfirmDelete = { events += "confirmDelete" },
                    onDismissDelete = { events += "dismissDelete" },
                    today = today,
                )
            }
        }
    }

    @Test
    fun `T8_6_1 標題是錢包名稱，下方「已封存」`() {
        show(emptyList())
        compose.onNodeWithText("舊帳").assertIsDisplayed()
        compose.onNodeWithText("已封存").assertIsDisplayed()
    }

    @Test
    fun `T8_6_3 有交易時顯示預算卡與交易列表，沒有「記帳」按鈕`() {
        show(listOf(tx("1"), tx("2")))
        compose.onNodeWithText("本週還可以花").assertIsDisplayed()
        compose.onNodeWithText("便當1").assertIsDisplayed()
        // Robolectric 的預設螢幕很矮，第二列在可視範圍外，只確認它有被畫出來
        compose.onNodeWithText("便當2").assertExists()
        compose.onNodeWithText("記帳").assertDoesNotExist()
    }

    @Test
    fun `T8_6_4 交易列不能點、不能左滑刪除，也沒有 TalkBack 刪除動作`() {
        show(listOf(tx("1")))
        compose.onAllNodesWithTag("transaction-row").assertCountEquals(1)
        compose
            .onNodeWithText("便當1", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose
            .onAllNodesWithTag("transaction-row")[0]
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
            .performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("便當1").assertIsDisplayed()
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun `T8_6_5 沒有交易時顯示「這個錢包沒有任何交易。」，沒有「記第一筆」`() {
        show(emptyList())
        compose.onNodeWithText("這個錢包沒有任何交易。").assertIsDisplayed()
        compose.onNodeWithText("記第一筆").assertDoesNotExist()
    }

    @Test
    fun `T8_6_6 右上角「取消封存」圖示鍵`() {
        show(emptyList())
        compose.onNodeWithContentDescription("取消封存").performClick()
        assertEquals(listOf("unarchive"), events)
    }

    @Test
    fun `T8_6_7 更多選項選單裡的「刪除錢包」`() {
        show(emptyList())
        compose.onNodeWithText("刪除錢包").assertDoesNotExist()
        compose.onNodeWithContentDescription("更多選項").performClick()
        compose.onNodeWithText("刪除錢包").performClick()
        assertEquals(listOf("requestDelete"), events)
    }

    @Test
    fun `T8_6_7 刪除確認顯示交易筆數，按「取消」只關掉對話框`() {
        show(listOf(tx("1"), tx("2"), tx("3")), pendingDeleteCount = 3)
        compose.onNodeWithText("刪除「舊帳」？").assertIsDisplayed()
        compose.onNodeWithText("這個錢包的 3 筆交易會一併刪除，無法復原。").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
        assertEquals(listOf("dismissDelete"), events)
    }

    @Test
    fun `T8_6_8 刪除確認按「刪除」`() {
        show(emptyList(), pendingDeleteCount = 0)
        compose.onNodeWithText("刪除").performClick()
        assertEquals(listOf("confirmDelete"), events)
    }

    @Test
    fun `T8_6_9 選單裡的「刪除錢包」無障礙只唸文字`() {
        show(emptyList())
        compose.onNodeWithContentDescription("更多選項").performClick()
        compose
            .onNodeWithText("刪除錢包")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }
}
