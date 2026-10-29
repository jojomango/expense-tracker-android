package com.jojomango.expensetracker.ui.transaction

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jojomango.expensetracker.domain.Category
import com.jojomango.expensetracker.domain.CategoryType
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class AddScreenPartsTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate(2026, 10, 28)
    private val categories =
        listOf(
            Category(id = "food", name = "飲食", type = CategoryType.EXPENSE, icon = "🍜", color = "#E57373"),
            Category(id = "bus", name = "交通", type = CategoryType.EXPENSE, icon = "🚗", color = "#64B5F6"),
        )

    private fun showDateRow(date: LocalDate) {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                DateAndNoteRow(date = date, note = "", today = today, onSelectDate = {}, onNoteChange = {})
            }
        }
    }

    @Test
    fun `T8_1_8 選今天時日曆 pill 未選取、沒有可見文字，描述為選日期`() {
        showDateRow(today)
        compose.onNodeWithContentDescription("選日期").assertIsDisplayed()
        compose.onNodeWithText("今天").assertIsSelected()
        compose.onNodeWithText("10/28").assertDoesNotExist()
    }

    @Test
    fun `T8_1_9 選了其他日期，圖示 pill 顯示 M-D 並帶狀態描述，今天昨天都未選取`() {
        showDateRow(LocalDate(2026, 10, 25))
        compose.onNodeWithText("10/25").assertIsDisplayed()
        compose.onNodeWithContentDescription("選日期").assertIsDisplayed()
        compose
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "10/25"))
            .assertIsSelected()
        compose.onNodeWithText("今天").assertIsNotSelected()
        compose.onNodeWithText("昨天").assertIsNotSelected()
    }

    @Test
    fun `T8_1_10 單列模式點分類會選取，備註欄保有焦點`() {
        var selected: String? = null
        val focus = FocusRequester()
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                Column {
                    TextField(value = "", onValueChange = {}, modifier = Modifier.testTag("note").focusRequester(focus))
                    NoteFocusedCategories(
                        layout = NoteCategoryLayout.ROW,
                        categories = categories,
                        selectedCategoryId = null,
                        onSelect = { selected = it },
                        onShowGrid = {},
                    )
                }
            }
        }
        compose.runOnIdle { focus.requestFocus() }
        compose.onNodeWithText("交通").performClick()
        assertEquals("bus", selected)
        compose.onNodeWithTag("note").assertIsFocused()
    }

    @Test
    fun `T8_1_11 摘要列未選分類時顯示選擇分類，點了回到網格`() {
        var showGrid = false
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                NoteFocusedCategories(
                    layout = NoteCategoryLayout.SUMMARY,
                    categories = categories,
                    selectedCategoryId = null,
                    onSelect = {},
                    onShowGrid = { showGrid = true },
                )
            }
        }
        // 摘要列整條合成一句給 TalkBack 唸（clearAndSetSemantics），所以用描述找
        compose.onNodeWithContentDescription("選擇分類").assertIsDisplayed().performClick()
        assertTrue(showGrid)
    }

    @Test
    fun `T8_1_11 摘要列以一句話唸出目前分類，且是按鈕`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                NoteFocusedCategories(
                    layout = NoteCategoryLayout.SUMMARY,
                    categories = categories,
                    selectedCategoryId = "food",
                    onSelect = {},
                    onShowGrid = {},
                )
            }
        }
        compose
            .onNodeWithContentDescription("更換分類，目前是飲食")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Button))
    }
}
