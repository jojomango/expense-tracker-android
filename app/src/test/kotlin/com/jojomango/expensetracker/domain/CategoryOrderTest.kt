package com.jojomango.expensetracker.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** TESTCASES.md T6.4 — 分類顯示順序（SPEC.md §3.3 v2.5、UX-003）。 */
class CategoryOrderTest {
    private val seeds = DefaultCategories.seedDefaults(ZhTestCategoryNames)

    private fun names(
        categories: List<Category>,
        type: CategoryType,
        names: CategoryNames = ZhTestCategoryNames,
    ) = sortedForDisplay(categories.filter { it.type == type }).map { it.displayName(names) }

    private fun custom(
        name: String,
        existing: List<Category>,
    ) = Category("c-$name", name, CategoryType.EXPENSE, "🐶", "#123456", sortOrder = nextCategorySortOrder(existing, CategoryType.EXPENSE))

    @Test
    @DisplayName("T6.4.1 — 預設支出分類依顯示順序：飲食、交通、居住、購物、娛樂、醫療、其他")
    fun `T6 4 1`() = assertEquals(listOf("飲食", "交通", "居住", "購物", "娛樂", "醫療", "其他"), names(seeds, CategoryType.EXPENSE))

    @Test
    @DisplayName("T6.4.2 — 預設收入分類依顯示順序：薪資、獎金、投資、其他")
    fun `T6 4 2`() = assertEquals(listOf("薪資", "獎金", "投資", "其他"), names(seeds, CategoryType.INCOME))

    @Test
    @DisplayName("T6.4.3 — 新增自訂支出分類「寵物」：在「醫療」之後、「其他」之前")
    fun `T6 4 3`() {
        val all = seeds + custom("寵物", seeds)
        assertEquals(listOf("飲食", "交通", "居住", "購物", "娛樂", "醫療", "寵物", "其他"), names(all, CategoryType.EXPENSE))
    }

    @Test
    @DisplayName("T6.4.4 — 依序新增「寵物」「旅行」：依新增先後，都在「其他」之前")
    fun `T6 4 4`() {
        val withPet = seeds + custom("寵物", seeds)
        val all = withPet + custom("旅行", withPet)
        assertEquals(listOf("寵物", "旅行", "其他"), names(all, CategoryType.EXPENSE).takeLast(3))
    }

    @Test
    @DisplayName("T6.4.5 — 「其他」改名為「雜支」：仍排在支出的最後")
    fun `T6 4 5`() {
        val renamed =
            seeds.map {
                if (it.defaultKey == DefaultCategoryKey.OTHER_EXPENSE) it.renamed("雜支", ZhTestCategoryNames) else it
            }
        val all = renamed + custom("寵物", renamed)
        assertEquals("雜支", names(all, CategoryType.EXPENSE).last())
    }

    @Test
    @DisplayName("T6.4.6 — 切換語言（繁中 ↔ 英文）：順序不變")
    fun `T6 4 6`() {
        val zh = sortedForDisplay(seeds).map { it.id }
        val en = sortedForDisplay(seeds).map { it.id }
        assertEquals(zh, en)
        assertEquals(
            listOf("Food", "Transport", "Housing", "Shopping", "Leisure", "Medical", "Other"),
            names(seeds, CategoryType.EXPENSE, EnTestCategoryNames),
        )
    }

    @Test
    @DisplayName("T6.4.7／T6.4.8 — 沒有順序的舊資料：預設分類照代號，其餘依名稱接在預設之後、「其他」之前")
    fun `legacy fill`() {
        val inputs =
            listOf(
                LegacyCategoryOrderInput("food", "飲食", CategoryType.EXPENSE, DefaultCategoryKey.FOOD),
                LegacyCategoryOrderInput("other", "其他", CategoryType.EXPENSE, DefaultCategoryKey.OTHER_EXPENSE),
                LegacyCategoryOrderInput("z", "寵物", CategoryType.EXPENSE, null),
                LegacyCategoryOrderInput("a", "旅行", CategoryType.EXPENSE, null),
                LegacyCategoryOrderInput("bonus", "獎金", CategoryType.INCOME, DefaultCategoryKey.BONUS),
                LegacyCategoryOrderInput("side", "接案", CategoryType.INCOME, null),
            )
        val orders = legacySortOrders(inputs)
        assertEquals(0, orders["food"])
        assertEquals(OTHER_CATEGORY_SORT_ORDER, orders["other"])
        // 自訂分類依名稱排序，從預設分類的最大值（支出：醫療 5）之後接
        val customs = listOf("a", "z").sortedBy { id -> inputs.first { it.id == id }.name }
        assertEquals(listOf(6, 7), customs.map { orders.getValue(it) })
        assertEquals(1, orders["bonus"])
        assertEquals(3, orders["side"], "收入的預設最大是投資 2，自訂從 3 開始")
        assertTrue(orders.values.all { it in 0..OTHER_CATEGORY_SORT_ORDER })
    }
}
