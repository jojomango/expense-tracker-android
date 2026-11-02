package com.jojomango.expensetracker.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TESTCASES.md T4.6.5～T4.6.8 的對話框按鈕（UI-SPEC.md §13.2）。 */
@RunWith(RobolectricTestRunner::class)
class BackupUpdateDialogTest {
    @get:Rule
    val compose = createComposeRule()

    private val clicks = mutableListOf<BackupUpdateChoice>()

    private fun show(fileName: String?) {
        compose.setContent {
            BackupUpdateDialog(
                prompt = BackupUpdatePrompt(fileName, Instant.parse("2026-10-31T04:00:00Z")),
                onChoice = { clicks += it },
            )
        }
    }

    @Test
    fun `T4_6_5 顯示檔名與取代說明`() {
        show("backup.json")
        compose.onNodeWithText("Update your last backup?").assertIsDisplayed()
        compose.onNodeWithText("File: backup.json").assertIsDisplayed()
        compose.onNodeWithText("This replaces the file with your current data.").assertIsDisplayed()
    }

    @Test
    fun `取不到檔名時省略檔案列`() {
        show(null)
        compose.onNodeWithText("File:", substring = true).assertDoesNotExist()
    }

    @Test
    fun `T4_6_6 到 T4_6_8 三個按鈕各自觸發`() {
        show("backup.json")
        compose.onNodeWithText("Update backup").performClick()
        compose.onNodeWithText("Save as new file").performClick()
        compose.onNodeWithText("Cancel").performClick()
        assertEquals(BackupUpdateChoice.entries.toList(), clicks)
    }
}
