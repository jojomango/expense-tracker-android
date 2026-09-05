package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * TESTCASES.md T6 — Category. Domain-only, no mocks (see CLAUDE.md 禁令 2).
 */
class CategoryTest {
    private fun category(
        isDefault: Boolean = false,
        color: String = "#C1502E",
    ) = Category("c1", "測試分類", CategoryType.EXPENSE, "🍜", color, isDefault)

    // ---- T6.1 分類刪除規則 ----

    @Test
    @DisplayName("T6.1.1 — isDefault == true，assertCanDeleteCategory 拋出明確錯誤")
    fun `T6 1 1`() {
        assertThrows(DefaultCategoryException::class.java) {
            assertCanDeleteCategory(category(isDefault = true))
        }
    }

    @Test
    @DisplayName("T6.1.2 — isDefault == false，不拋錯")
    fun `T6 1 2`() {
        assertCanDeleteCategory(category(isDefault = false))
    }

    // ---- T6.2 分類色驗證 ----

    @Test
    @DisplayName("T6.2.1 — color = #C1502E，通過")
    fun `T6 2 1`() {
        category(color = "#C1502E")
    }

    @Test
    @DisplayName("T6.2.2 — color = \"\"，拋錯")
    fun `T6 2 2`() {
        assertThrows(IllegalArgumentException::class.java) { category(color = "") }
    }

    @Test
    @DisplayName("T6.2.3 — color = \"red\"，拋錯（只接受 #rrggbb）")
    fun `T6 2 3`() {
        assertThrows(IllegalArgumentException::class.java) { category(color = "red") }
    }

    @Test
    @DisplayName("T6.2.4 — color = \"#FFF\"，拋錯（不接受三位縮寫）")
    fun `T6 2 4`() {
        assertThrows(IllegalArgumentException::class.java) { category(color = "#FFF") }
    }

    @Test
    @DisplayName("T6.2.5 — 預設分類種子：11 個分類都有 color，且與 UI-SPEC.md §2.2 表格完全一致")
    fun `T6 2 5`() {
        val seeds = DefaultCategories.seedDefaults()
        assertEquals(11, seeds.size)

        val expected =
            listOf(
                Triple("飲食", "🍜", "#C1502E"),
                Triple("交通", "🚗", "#3F8F6A"),
                Triple("居住", "🏠", "#A8792F"),
                Triple("購物", "🛒", "#2F6F9F"),
                Triple("娛樂", "🎬", "#8A5FBF"),
                Triple("醫療", "💊", "#C04A6E"),
                Triple("其他", "📦", "#7A7A80"),
                Triple("薪資", "💰", "#2F8F63"),
                Triple("獎金", "🎁", "#C98B2E"),
                Triple("投資", "📈", "#4A6FA8"),
                Triple("其他", "📦", "#7A7A80"),
            )
        assertEquals(expected, seeds.map { Triple(it.name, it.icon, it.color) })
        assertEquals(7, seeds.count { it.type == CategoryType.EXPENSE })
        assertEquals(4, seeds.count { it.type == CategoryType.INCOME })
        assertEquals(true, seeds.all { it.isDefault })
    }

    // ---- T6.3 分類色穩定性 ----

    @Test
    @DisplayName("T6.3.1 — 同一分類在本週彙總第 1 名、本月彙總第 3 名，兩次取得的顏色相同")
    fun `T6 3 1`() {
        val cat = category(color = "#4A6FA8")
        val categories = listOf(cat)
        // 不管排名context為何，colorOf 只看 categoryId，不看排序位置
        val colorAsFirst = colorOf(cat.id, categories)
        val colorAsThird = colorOf(cat.id, categories)
        assertEquals(colorAsFirst, colorAsThird)
        assertEquals("#4A6FA8", colorAsFirst)
    }

    @Test
    @DisplayName("T6.3.2 — summarizeByCategory 的回傳不含任何顏色資訊")
    fun `T6 3 2`() {
        // 用 java.lang.Class 反射（不需要 kotlin-reflect 這個額外依賴）確認
        // CategorySummary 沒有任何跟顏色相關的欄位——顏色一律用 colorOf() 現查。
        val hasColorField =
            CategorySummary::class.java.declaredFields.any {
                it.name.contains("color", ignoreCase = true)
            }
        assertFalse(hasColorField)
    }

    @Test
    @DisplayName("T6.3.3 — categoryId == null（未分類），顏色為 #7A7A80")
    fun `T6 3 3`() {
        assertEquals("#7A7A80", colorOf(null, emptyList()))
        assertEquals(UNCATEGORIZED_COLOR, colorOf(null, listOf(category())))
    }
}
