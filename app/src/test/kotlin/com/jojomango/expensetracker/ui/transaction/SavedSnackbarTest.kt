package com.jojomango.expensetracker.ui.transaction

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.ui.i18n.ResourceCategoryNames
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.1.4、T8.5.3、T8.5.12（UI-SPEC.md §5、§15.2）：記一筆成功後的 Snackbar。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class SavedSnackbarTest {
    @get:Rule
    val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val food = Category("food", "飲食", CategoryType.EXPENSE, "🍜", "#000000")
    private val transport = Category("transport", "交通", CategoryType.EXPENSE, "🚌", "#000000")

    private fun tx(
        amount: Long,
        currency: String,
        categoryId: String,
    ) = Transaction(
        id = "t",
        walletId = "w",
        type = TransactionType.EXPENSE,
        amount = amount,
        currency = currency,
        categoryId = categoryId,
        date = LocalDate(2026, 11, 12),
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private fun message(transaction: Transaction) =
        savedTransactionMessage(context, transaction, listOf(food, transport), ResourceCategoryNames(context))

    @Test
    fun `T8_1_4 記一筆後的訊息是「已記錄 飲食 NT$180_00」`() {
        assertEquals("已記錄 飲食 NT$180", message(tx(18_000, "TWD", "food")))
    }

    @Test
    fun `T8_5_3 選 USD 輸入 40，訊息金額用這筆交易的幣別`() {
        val amount = AddEditTransactionUiState(currency = "USD", amountDigits = "40").amount
        assertEquals("已記錄 交通 $40.00", message(tx(amount, "USD", "transport")))
    }

    @Test
    fun `T8_5_12 選 JPY 輸入 40，日圓沒有小數`() {
        val amount = AddEditTransactionUiState(currency = "JPY", amountDigits = "40").amount
        assertEquals(40L, amount)
        assertEquals("已記錄 飲食 ¥40", message(tx(amount, "JPY", "food")))
    }

    @Test
    fun `T8_1_4 首頁收到訊息時顯示 Snackbar，顯示後通知已消化`() {
        // 跟 MainActivity 一樣：onShown 會把訊息清成 null，Snackbar 不能因此被取消
        val message = mutableStateOf<String?>("已記錄 飲食 NT$180")
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                val host = remember { SnackbarHostState() }
                Scaffold(snackbarHost = { SnackbarHost(host) }) { _ ->
                    SavedTransactionSnackbar(message.value, host, onShown = { message.value = null })
                }
            }
        }
        compose.onNodeWithText("已記錄 飲食 NT$180").assertIsDisplayed()
        assertNull(message.value)
    }
}
