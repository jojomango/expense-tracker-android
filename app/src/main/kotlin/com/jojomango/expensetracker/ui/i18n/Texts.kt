package com.jojomango.expensetracker.ui.i18n

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.jojomango.expensetracker.R
import com.jojomango.expensetracker.domain.CategoryNames
import com.jojomango.expensetracker.domain.DayTitleLabels
import com.jojomango.expensetracker.domain.DefaultCategoryKey
import com.jojomango.expensetracker.domain.WeekTitleLabels

/** domain 的 [CategoryNames] 接到 string resources（SPEC.md §3.3、§3.9）。 */
class ResourceCategoryNames(
    private val context: Context,
) : CategoryNames {
    override fun nameOf(key: DefaultCategoryKey): String = context.getString(key.stringRes)

    override val uncategorized: String get() = context.getString(R.string.category_uncategorized)
}

@get:StringRes
val DefaultCategoryKey.stringRes: Int
    get() =
        when (this) {
            DefaultCategoryKey.FOOD -> R.string.category_food
            DefaultCategoryKey.TRANSPORT -> R.string.category_transport
            DefaultCategoryKey.HOUSING -> R.string.category_housing
            DefaultCategoryKey.SHOPPING -> R.string.category_shopping
            DefaultCategoryKey.ENTERTAINMENT -> R.string.category_entertainment
            DefaultCategoryKey.MEDICAL -> R.string.category_medical
            DefaultCategoryKey.OTHER_EXPENSE, DefaultCategoryKey.OTHER_INCOME -> R.string.category_other
            DefaultCategoryKey.SALARY -> R.string.category_salary
            DefaultCategoryKey.BONUS -> R.string.category_bonus
            DefaultCategoryKey.INVESTMENT -> R.string.category_investment
        }

/** 語言改變時 Activity 會重建，Configuration 也會跟著變；用它當 key，名稱表跟著更新。 */
@Composable
fun rememberCategoryNames(): CategoryNames {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { ResourceCategoryNames(context) }
}

/** 首頁日小標題的文字（UI-SPEC §4.3 v2.7）。星期順序跟 [kotlinx.datetime.DayOfWeek] 一樣從週一開始。 */
fun dayTitleLabels(context: Context) =
    DayTitleLabels(
        today = context.getString(R.string.date_today),
        yesterday = context.getString(R.string.date_yesterday),
        weekdays = context.resources.getStringArray(R.array.weekday_short).toList(),
        weekdayFirst = context.resources.getBoolean(R.bool.day_title_weekday_first),
    )

fun weekTitleLabels(context: Context) =
    WeekTitleLabels(
        thisWeek = context.getString(R.string.week_title_this_week),
        lastWeek = context.getString(R.string.week_title_last_week),
    )

/**
 * ViewModel 要顯示的訊息：只帶 resource id 與參數，到畫面上才變成文字——
 * ViewModel 活得比 Activity 久，語言切換後不會留下舊語言的字串。
 */
data class UiMessage(
    @StringRes val id: Int,
    val args: List<Any> = emptyList(),
) {
    fun resolve(context: Context): String = context.getString(id, *args.toTypedArray())
}
