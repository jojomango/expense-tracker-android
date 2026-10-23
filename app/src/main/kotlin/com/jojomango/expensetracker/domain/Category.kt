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
    /**
     * 未改名的預設分類才有代號：名稱依目前語言顯示（SPEC.md §3.3 v2.3）。使用者改名後清成 `null`，
     * 從此顯示使用者取的名稱——但 [isDefault] 不變，一樣不可刪除。[name] 對有代號的分類只是後備值
     * （例如舊版 App 匯入新備份時會看到它）。
     */
    val defaultKey: DefaultCategoryKey? = null,
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
 * 預設分類的固定代號（SPEC.md §3.3 v2.3）。[id] 寫進資料庫與備份檔，**之後不能改**。
 * 支出與收入各有一個「其他」，所以是兩個代號（T10.2.6）。圖示與顏色照 UI-SPEC.md §2.2。
 */
enum class DefaultCategoryKey(
    val id: String,
    val type: CategoryType,
    val icon: String,
    val color: String,
) {
    FOOD("food", CategoryType.EXPENSE, "🍜", "#C1502E"),
    TRANSPORT("transport", CategoryType.EXPENSE, "🚗", "#3F8F6A"),
    HOUSING("housing", CategoryType.EXPENSE, "🏠", "#A8792F"),
    SHOPPING("shopping", CategoryType.EXPENSE, "🛒", "#2F6F9F"),
    ENTERTAINMENT("entertainment", CategoryType.EXPENSE, "🎬", "#8A5FBF"),
    MEDICAL("medical", CategoryType.EXPENSE, "💊", "#C04A6E"),
    OTHER_EXPENSE("other_expense", CategoryType.EXPENSE, "📦", "#7A7A80"),
    SALARY("salary", CategoryType.INCOME, "💰", "#2F8F63"),
    BONUS("bonus", CategoryType.INCOME, "🎁", "#C98B2E"),
    INVESTMENT("investment", CategoryType.INCOME, "📈", "#4A6FA8"),
    OTHER_INCOME("other_income", CategoryType.INCOME, "📦", "#7A7A80"),
    ;

    companion object {
        fun fromId(id: String?): DefaultCategoryKey? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 預設分類與「未分類」在目前語言下的名稱。實作在 `ui`（讀 Android string resources），
 * domain 只定義介面——CLAUDE.md：domain 不放介面文字。
 */
interface CategoryNames {
    fun nameOf(key: DefaultCategoryKey): String

    val uncategorized: String
}

/** T10.2.1～T10.2.4：有代號就照目前語言，沒有就是使用者取的名稱。 */
fun Category.displayName(names: CategoryNames): String = defaultKey?.let(names::nameOf) ?: name

/** T10.2.5：`null` 或已經被刪除的分類都顯示「未分類」。 */
fun categoryDisplayName(
    categoryId: String?,
    categories: List<Category>,
    names: CategoryNames,
): String = categories.firstOrNull { it.id == categoryId }?.displayName(names) ?: names.uncategorized

/**
 * 編輯分類後的名稱。編輯頁的名稱欄位預填的是**顯示名稱**，所以只改圖示或顏色時欄位不變——
 * 這不算改名，代號保留、繼續跟著語言（T10.2.3）。欄位內容跟顯示名稱不同才算改名。
 */
fun Category.renamed(
    newName: String,
    names: CategoryNames,
): Category = if (newName == displayName(names)) this else copy(name = newName, defaultKey = null)

/**
 * 首次啟動建立的預設分類種子——SPEC.md §3.3，色票照 UI-SPEC.md §2.2 表格填。
 */
object DefaultCategories {
    /** [names] 決定寫進資料庫的後備名稱（見 [Category.defaultKey]）；畫面上顯示的永遠是目前語言。 */
    fun seedDefaults(
        names: CategoryNames,
        idGenerator: () -> String = {
            java.util.UUID
                .randomUUID()
                .toString()
        },
    ): List<Category> =
        DefaultCategoryKey.entries.map { key ->
            Category(idGenerator(), names.nameOf(key), key.type, key.icon, key.color, isDefault = true, defaultKey = key)
        }
}
