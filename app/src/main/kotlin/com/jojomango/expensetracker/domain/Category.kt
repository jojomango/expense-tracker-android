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
    /** 顯示順序（SPEC.md §3.3 v2.5），同一種 [type] 內由小到大；不因改名或切換語言而改變。 */
    val sortOrder: Int = 0,
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
    /** SPEC.md §3.3 v2.5 的固定順序；兩個「其他」是 [OTHER_CATEGORY_SORT_ORDER]，永遠最後。 */
    val sortOrder: Int,
) {
    FOOD("food", CategoryType.EXPENSE, "🍜", "#C1502E", 0),
    TRANSPORT("transport", CategoryType.EXPENSE, "🚗", "#3F8F6A", 1),
    HOUSING("housing", CategoryType.EXPENSE, "🏠", "#A8792F", 2),
    SHOPPING("shopping", CategoryType.EXPENSE, "🛒", "#2F6F9F", 3),
    ENTERTAINMENT("entertainment", CategoryType.EXPENSE, "🎬", "#8A5FBF", 4),
    MEDICAL("medical", CategoryType.EXPENSE, "💊", "#C04A6E", 5),
    OTHER_EXPENSE("other_expense", CategoryType.EXPENSE, "📦", "#7A7A80", OTHER_CATEGORY_SORT_ORDER),
    SALARY("salary", CategoryType.INCOME, "💰", "#2F8F63", 0),
    BONUS("bonus", CategoryType.INCOME, "🎁", "#C98B2E", 1),
    INVESTMENT("investment", CategoryType.INCOME, "📈", "#4A6FA8", 2),
    OTHER_INCOME("other_income", CategoryType.INCOME, "📦", "#7A7A80", OTHER_CATEGORY_SORT_ORDER),
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
            Category(
                idGenerator(),
                names.nameOf(key),
                key.type,
                key.icon,
                key.color,
                isDefault = true,
                defaultKey = key,
                sortOrder = key.sortOrder,
            )
        }
}

/**
 * 兩個「其他」的固定順序（SPEC.md §3.3 v2.5、D15）。用固定值而不是代號判斷「永遠最後」：
 * 「其他」被改名後代號會清掉，但順序值還在，所以仍排在最後（T6.4.5）。
 */
const val OTHER_CATEGORY_SORT_ORDER = 1_000_000

/** 記帳頁網格與分類管理頁共用的順序：`sortOrder` 由小到大，相同時依名稱（只在升級前的資料可能發生）。 */
fun sortedForDisplay(categories: List<Category>): List<Category> = categories.sortedWith(compareBy({ it.sortOrder }, { it.name }))

/** 新分類排在同類型既有分類之後、「其他」之前（T6.4.3、T6.4.4）。 */
fun nextCategorySortOrder(
    existing: List<Category>,
    type: CategoryType,
): Int = (existing.filter { it.type == type && it.sortOrder < OTHER_CATEGORY_SORT_ORDER }.maxOfOrNull { it.sortOrder } ?: -1) + 1

/** [legacySortOrders] 的輸入：升級前的資料或舊備份裡的一個分類。 */
data class LegacyCategoryOrderInput(
    val id: String,
    val name: String,
    val type: CategoryType,
    val defaultKey: DefaultCategoryKey?,
)

/**
 * 沒有順序的舊資料怎麼補值（SPEC.md §3.3 v2.5，T6.4.7／T6.4.8）：有代號的預設分類照代號的固定值；
 * 其餘（自訂分類、升級前就改過名而認不出來的預設分類）依名稱排序，接在該類型預設分類的最大值之後。
 * 資料庫升級與匯入舊備份都用這一個函式，兩邊結果才會一樣。
 */
fun legacySortOrders(entries: List<LegacyCategoryOrderInput>): Map<String, Int> {
    val result = mutableMapOf<String, Int>()
    entries.forEach { entry -> entry.defaultKey?.let { result[entry.id] = it.sortOrder } }
    entries
        .filter { it.defaultKey == null }
        .groupBy { it.type }
        .forEach { (type, others) ->
            val start =
                DefaultCategoryKey.entries
                    .filter { it.type == type && it.sortOrder < OTHER_CATEGORY_SORT_ORDER }
                    .maxOf { it.sortOrder } + 1
            others.sortedBy { it.name }.forEachIndexed { index, entry -> result[entry.id] = start + index }
        }
    return result
}
