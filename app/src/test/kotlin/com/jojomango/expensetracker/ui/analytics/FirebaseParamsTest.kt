package com.jojomango.expensetracker.ui.analytics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FirebaseParamsTest {
    @Test
    @DisplayName("Firebase 事件參數不支援布林：is_edit 轉成字串 true／false，其他字串原樣保留")
    fun `booleans become strings`() {
        assertEquals(
            mapOf("type" to "expense", "is_edit" to "true"),
            toFirebaseParams(mapOf("type" to "expense", "is_edit" to true)),
        )
        assertEquals(mapOf("is_edit" to "false"), toFirebaseParams(mapOf("is_edit" to false)))
    }
}
