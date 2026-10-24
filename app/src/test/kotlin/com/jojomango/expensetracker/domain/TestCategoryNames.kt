package com.jojomango.expensetracker.domain

/** 測試用的分類名稱表（真正的名稱在 Android string resources，domain 不放介面文字）。 */
object ZhTestCategoryNames : CategoryNames {
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
}

object EnTestCategoryNames : CategoryNames {
    override fun nameOf(key: DefaultCategoryKey): String =
        when (key) {
            DefaultCategoryKey.FOOD -> "Food"
            DefaultCategoryKey.TRANSPORT -> "Transport"
            DefaultCategoryKey.HOUSING -> "Housing"
            DefaultCategoryKey.SHOPPING -> "Shopping"
            DefaultCategoryKey.ENTERTAINMENT -> "Leisure"
            DefaultCategoryKey.MEDICAL -> "Medical"
            DefaultCategoryKey.OTHER_EXPENSE -> "Other"
            DefaultCategoryKey.SALARY -> "Salary"
            DefaultCategoryKey.BONUS -> "Bonus"
            DefaultCategoryKey.INVESTMENT -> "Investment"
            DefaultCategoryKey.OTHER_INCOME -> "Other"
        }

    override val uncategorized = "Uncategorized"
}

val ZhWeekTitleLabels = WeekTitleLabels(thisWeek = "本週", lastWeek = "上週")
val EnWeekTitleLabels = WeekTitleLabels(thisWeek = "This week", lastWeek = "Last week")
