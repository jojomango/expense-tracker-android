package com.jojomango.expensetracker.ui.transaction

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** UX-005：備註聚焦時，鍵盤上方放得下單列就用單列，放不下改摘要列；門檻有緩衝，不在臨界值來回切。 */
class NoteCategoryLayoutTest {
    @Test
    fun `空間足夠用單列`() {
        assertEquals(NoteCategoryLayout.ROW, noteCategoryLayout(available = 446.dp, previous = NoteCategoryLayout.ROW))
    }

    @Test
    fun `空間不足用摘要列`() {
        assertEquals(NoteCategoryLayout.SUMMARY, noteCategoryLayout(available = 326.dp, previous = NoteCategoryLayout.ROW))
    }

    @Test
    fun `門檻附近維持原本的版面，不閃爍`() {
        val atThreshold = NOTE_ROW_MIN_HEIGHT
        assertEquals(NoteCategoryLayout.ROW, noteCategoryLayout(atThreshold, previous = NoteCategoryLayout.ROW))
        assertEquals(NoteCategoryLayout.SUMMARY, noteCategoryLayout(atThreshold, previous = NoteCategoryLayout.SUMMARY))
    }

    @Test
    fun `超過緩衝區才切換`() {
        val margin = NOTE_LAYOUT_HYSTERESIS + 1.dp
        assertEquals(NoteCategoryLayout.SUMMARY, noteCategoryLayout(NOTE_ROW_MIN_HEIGHT - margin, NoteCategoryLayout.ROW))
        assertEquals(NoteCategoryLayout.ROW, noteCategoryLayout(NOTE_ROW_MIN_HEIGHT + margin, NoteCategoryLayout.SUMMARY))
    }
}
