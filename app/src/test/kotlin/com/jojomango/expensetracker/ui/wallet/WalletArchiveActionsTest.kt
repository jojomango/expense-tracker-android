package com.jojomango.expensetracker.ui.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jojomango.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TESTCASES.md T8.6.1、T8.6.2、T8.6.9（UI-SPEC.md §11）：錢包管理頁的已封存列、編輯頁的封存／刪除鍵。 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "zh-rTW")
class WalletArchiveActionsTest {
    @get:Rule
    val compose = createComposeRule()

    private val events = mutableListOf<String>()

    private fun showArchivedRow() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                ArchivedWalletRow(name = "舊帳", onOpen = { events += "open" }, onUnarchive = { events += "unarchive" })
            }
        }
    }

    @Test
    fun `T8_6_1 點已封存的一列開檢視頁`() {
        showArchivedRow()
        compose.onNodeWithText("舊帳").performClick()
        assertEquals(listOf("open"), events)
    }

    @Test
    fun `T8_6_2 點列尾「取消封存」直接取消封存，不開檢視頁`() {
        showArchivedRow()
        compose.onNodeWithText("取消封存").performClick()
        assertEquals(listOf("unarchive"), events)
    }

    @Test
    fun `T8_6_9 有文字的按鈕無障礙描述只唸文字`() {
        compose.setContent {
            ExpenseTrackerTheme(darkTheme = false) {
                Column {
                    ArchivedWalletRow(name = "舊帳", onOpen = {}, onUnarchive = {})
                    ArchiveAndDeleteSection(isLastActive = false, onArchive = {}, onDelete = {})
                }
            }
        }
        listOf("取消封存", "封存錢包", "刪除錢包").forEach { label ->
            compose.onNodeWithText(label).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
        }
    }
}
