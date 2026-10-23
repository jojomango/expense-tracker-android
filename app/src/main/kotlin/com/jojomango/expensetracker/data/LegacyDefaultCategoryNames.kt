package com.jojomango.expensetracker.data

import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.DefaultCategoryKey

/**
 * v2.3 以前的預設分類只存繁中名稱、沒有代號。這張表是**資料相容**用的，不是介面文字：
 * - schema v1 → v2 遷移與匯入舊備份時，用「名稱＋收支類型」認出未改名的預設分類（T10.3.1、T10.3.4）
 * - 新建預設分類時寫進資料庫的後備名稱（舊版 App 匯入新備份時看得懂）
 *
 * 畫面上的名稱一律來自 string resources（`ui/i18n`），不要從這裡拿。
 */
internal object LegacyDefaultCategoryNames : CategoryNames {
    override fun nameOf(key: DefaultCategoryKey): String =
        when (key) {
            DefaultCategoryKey.FOOD -> "飲食"
            DefaultCategoryKey.TRANSPORT -> "交通"
            DefaultCategoryKey.HOUSING -> "居住"
            DefaultCategoryKey.SHOPPING -> "購物"
            DefaultCategoryKey.ENTERTAINMENT -> "娛樂"
            DefaultCategoryKey.MEDICAL -> "醫療"
            DefaultCategoryKey.OTHER_EXPENSE -> "其他"
            DefaultCategoryKey.SALARY -> "薪資"
            DefaultCategoryKey.BONUS -> "獎金"
            DefaultCategoryKey.INVESTMENT -> "投資"
            DefaultCategoryKey.OTHER_INCOME -> "其他"
        }

    override val uncategorized = "未分類"

    /** 未改名的預設分類（名稱與收支類型都對得上）才有代號；自訂分類一律沒有。 */
    fun keyFor(
        name: String,
        type: String,
        isDefault: Boolean,
    ): DefaultCategoryKey? =
        if (!isDefault) {
            null
        } else {
            DefaultCategoryKey.entries.firstOrNull { it.type.name == type && nameOf(it) == name }
        }
}
