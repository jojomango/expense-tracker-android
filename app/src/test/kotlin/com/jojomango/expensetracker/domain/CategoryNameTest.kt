package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T10.2 — 分類顯示名稱（SPEC.md §3.3、§3.9）。 */
class CategoryNameTest {
    private val food = DefaultCategories.seedDefaults(ZhTestCategoryNames).first { it.defaultKey == DefaultCategoryKey.FOOD }
    private val pet = Category("c-pet", "寵物", CategoryType.EXPENSE, "🐶", "#123456")

    @Test
    @DisplayName("T10.2.1 — 預設分類 food、未改名，繁中 → 飲食")
    fun `T10 2 1`() = assertEquals("飲食", food.displayName(ZhTestCategoryNames))

    @Test
    @DisplayName("T10.2.2 — 預設分類 food、未改名，英文 → Food")
    fun `T10 2 2`() = assertEquals("Food", food.displayName(EnTestCategoryNames))

    @Test
    @DisplayName("T10.2.3 — food 被改名為「吃飯」，英文 → 吃飯（改過名就不翻譯）")
    fun `T10 2 3`() {
        val renamed = food.renamed("吃飯", ZhTestCategoryNames)
        assertEquals("吃飯", renamed.displayName(EnTestCategoryNames))
        assertNull(renamed.defaultKey)
        assertEquals(true, renamed.isDefault, "改名後仍是預設分類，一樣不可刪除")
    }

    @Test
    @DisplayName("T10.2.3 — 只改圖示或顏色、名稱欄位維持顯示的名稱：不算改名，繼續跟著語言")
    fun `editing without renaming keeps key`() {
        val edited = food.renamed(food.displayName(EnTestCategoryNames), EnTestCategoryNames)
        assertEquals(DefaultCategoryKey.FOOD, edited.defaultKey)
        assertEquals("飲食", edited.displayName(ZhTestCategoryNames))
    }

    @Test
    @DisplayName("T10.2.4 — 使用者自訂分類「寵物」，英文 → 寵物")
    fun `T10 2 4`() = assertEquals("寵物", pet.displayName(EnTestCategoryNames))

    @Test
    @DisplayName("T10.2.5 — 未分類，英文 → Uncategorized")
    fun `T10 2 5`() {
        assertEquals("Uncategorized", categoryDisplayName(null, listOf(food), EnTestCategoryNames))
        assertEquals("Uncategorized", categoryDisplayName("deleted-id", listOf(food), EnTestCategoryNames))
        assertEquals("Food", categoryDisplayName(food.id, listOf(food), EnTestCategoryNames))
    }

    @Test
    @DisplayName("T10.2.6 — 支出與收入的「其他」各有代號，英文都顯示 Other")
    fun `T10 2 6`() {
        val seeds = DefaultCategories.seedDefaults(ZhTestCategoryNames)
        val others = seeds.filter { it.name == "其他" }
        assertEquals(2, others.size)
        assertNotEquals(others[0].defaultKey, others[1].defaultKey)
        others.forEach { assertEquals("Other", it.displayName(EnTestCategoryNames)) }
    }

    @Test
    @DisplayName("T10.2.7 — 統計彙總的分類名稱依目前語言，不是固定繁中")
    fun `T10 2 7`() {
        val tx =
            listOf(
                transaction(categoryId = food.id, amount = 300),
                transaction(categoryId = null, amount = 100),
            )
        val en = Budget.summarizeByCategory(tx, listOf(food), TransactionType.EXPENSE, "TWD", EnTestCategoryNames)
        assertEquals(listOf("Food", "Uncategorized"), en.map { it.name })
        val zh = Budget.summarizeByCategory(tx, listOf(food), TransactionType.EXPENSE, "TWD", ZhTestCategoryNames)
        assertEquals(listOf("飲食", "未分類"), zh.map { it.name })
    }

    @Test
    @DisplayName("每個預設分類代號的 id 不重複，且可以用 id 找回來")
    fun `key ids`() {
        val ids = DefaultCategoryKey.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        DefaultCategoryKey.entries.forEach { assertEquals(it, DefaultCategoryKey.fromId(it.id)) }
        assertNull(DefaultCategoryKey.fromId("nope"))
        assertNull(DefaultCategoryKey.fromId(null))
    }

    private fun transaction(
        categoryId: String?,
        amount: Long,
    ) = Transaction(
        id = "t-$amount",
        walletId = "w",
        type = TransactionType.EXPENSE,
        amount = amount,
        categoryId = categoryId,
        date = kotlinx.datetime.LocalDate(2026, 10, 1),
        note = null,
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(0),
    )
}
