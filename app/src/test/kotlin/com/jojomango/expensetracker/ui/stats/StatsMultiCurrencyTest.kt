package com.jojomango.expensetracker.ui.stats

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jojomango.expensetracker.domain.BudgetMode
import com.jojomango.expensetracker.domain.Money
import com.jojomango.expensetracker.domain.Transaction
import com.jojomango.expensetracker.domain.TransactionType
import com.jojomango.expensetracker.domain.Wallet
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

/** TESTCASES.md T8.5.7、T8.5.8（UI-SPEC.md §15.4）：統計頁的幣別 chip。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class StatsMultiCurrencyTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 11, 11)
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
        createdAt = Instant.fromEpochMilliseconds(0),
        updatedAt = Instant.fromEpochMilliseconds(0),
    )

    private val mixed = listOf(tx(120_000, "PHP"), tx(4_000, "USD"))

    private fun build(selected: String?) =
        buildStatsUiState(cebu, mixed, emptyList(), StatsQuery(StatsPeriod.WEEK, selected, DayOfWeek.MONDAY, today))

    @Test
    fun `T8_5_8 預設選 PHP；選 USD 後圓環與趨勢只算 USD`() {
        val php = build(selected = null)
        assertEquals(listOf("PHP", "USD"), php.currencies)
        assertEquals("PHP", php.currency)
        assertEquals(Money.of(120_000, "PHP"), php.periodTotal)

        val usd = build(selected = "USD")
        assertEquals(Money.of(4_000, "USD"), usd.periodTotal)
        assertEquals(Money.of(4_000, "USD"), usd.trendPoints.last().total)
    }

    @Test
    fun `T8_5_8 選了錢包沒有的幣別（例如剛切換錢包）時回到預設選取`() {
        assertEquals("PHP", build(selected = "JPY").currency)
    }

    @Test
    fun `T8_5_8 兩種幣別時出現 PHP、USD chip，點 USD 會回報選取`() {
        var picked: String? = null
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                StatsCurrencyChips(currencies = listOf("PHP", "USD"), selected = "PHP", onSelect = { picked = it })
            }
        }
        compose.onNodeWithText("PHP").assertIsSelected()
        compose.onNodeWithText("USD").assertIsDisplayed().performClick()
        assertEquals("USD", picked)
    }

    @Test
    fun `T8_5_7 只有一種幣別時沒有幣別 chip 列`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                StatsCurrencyChips(currencies = listOf("PHP"), selected = "PHP", onSelect = {})
            }
        }
        compose.onNodeWithText("PHP").assertDoesNotExist()
    }
}
