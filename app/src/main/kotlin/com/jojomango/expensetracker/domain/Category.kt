package com.jojomango.expensetracker.domain

/** SPEC.md §3.3：分類全域共用，不隸屬於特定錢包。 */
enum class CategoryType { EXPENSE, INCOME }

private val hexColorPattern = Regex("^#[0-9A-Fa-f]{6}$")

/**
 * 未分類（`categoryId == null`）固定顯示色——UI-SPEC.md §2.2。
 */
const val UNCATEGORIZED_COLOR = "#7A7A80"

/**
 * SPEC.md §3.3 分類。[color] 綁在分類本身、從一開始就存在（Android 版沒有網頁版
 * 「分類色是後來才加的欄位」那個歷史包袱，見 TASKS.md Phase 2 交接筆記），
 * 只接受 `#rrggbb` 六位格式，不接受三位縮寫（見 TESTCASES.md T6.2）。
 */
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
    val isDefault: Boolean = false,
) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
        require(hexColorPattern.matches(color)) { "Category color must be #rrggbb, was \"$color\"" }
    }
}

/** 嘗試刪除系統預設分類時拋出（TESTCASES.md T6.1）。 */
class DefaultCategoryException(
    message: String,
) : IllegalStateException(message)

/** 系統預設分類可改名但不可刪除——SPEC.md §3.3。 */
fun assertCanDeleteCategory(category: Category) {
    if (category.isDefault) {
        throw DefaultCategoryException("Cannot delete default category: ${category.name}")
    }
}

/**
 * 給定 [categoryId]（`null` 代表未分類，或指向一個已經不在 [categories] 裡的
 * 已刪除分類），回傳應顯示的顏色。顏色永遠來自分類本身，不由任何排序/彙總結果
 * 決定（見 TESTCASES.md T6.3 分類色穩定性）。
 */
fun colorOf(
    categoryId: String?,
    categories: List<Category>,
): String = categories.firstOrNull { it.id == categoryId }?.color ?: UNCATEGORIZED_COLOR

/**
 * 首次啟動建立的預設分類種子——SPEC.md §3.3，色票照 UI-SPEC.md §2.2 表格填。
 */
object DefaultCategories {
    fun seedDefaults(
        idGenerator: () -> String = {
            java.util.UUID
                .randomUUID()
                .toString()
        },
    ): List<Category> =
        listOf(
            Category(idGenerator(), "飲食", CategoryType.EXPENSE, "🍜", "#C1502E", isDefault = true),
            Category(idGenerator(), "交通", CategoryType.EXPENSE, "🚗", "#3F8F6A", isDefault = true),
            Category(idGenerator(), "居住", CategoryType.EXPENSE, "🏠", "#A8792F", isDefault = true),
            Category(idGenerator(), "購物", CategoryType.EXPENSE, "🛒", "#2F6F9F", isDefault = true),
            Category(idGenerator(), "娛樂", CategoryType.EXPENSE, "🎬", "#8A5FBF", isDefault = true),
            Category(idGenerator(), "醫療", CategoryType.EXPENSE, "💊", "#C04A6E", isDefault = true),
            Category(idGenerator(), "其他", CategoryType.EXPENSE, "📦", "#7A7A80", isDefault = true),
            Category(idGenerator(), "薪資", CategoryType.INCOME, "💰", "#2F8F63", isDefault = true),
            Category(idGenerator(), "獎金", CategoryType.INCOME, "🎁", "#C98B2E", isDefault = true),
            Category(idGenerator(), "投資", CategoryType.INCOME, "📈", "#4A6FA8", isDefault = true),
            Category(idGenerator(), "其他", CategoryType.INCOME, "📦", "#7A7A80", isDefault = true),
        )
}
