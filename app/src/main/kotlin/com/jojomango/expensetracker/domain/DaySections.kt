package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** 首頁一週裡依序出現的東西：某一天的交易，或跨年時插進來的年份分隔（UI-SPEC §4.3 v2.7）。 */
sealed class WeekEntry<out T> {
    data class Year(
        val year: Int,
    ) : WeekEntry<Nothing>()

    data class Day<T>(
        val date: LocalDate,
        val items: List<T>,
    ) : WeekEntry<T>()
}

/** 日小標題的文字，由 `ui` 從 string resources 傳進來（domain 不放介面文字）。[weekdays] 從週一排到週日。 */
data class DayTitleLabels(
    val today: String,
    val yesterday: String,
    val weekdays: List<String>,
    val weekdayFirst: Boolean,
)

/** 週內依日分段、年份分隔與日小標題（UI-SPEC §4.3 v2.7、UX-006）。純函式，「今天」由參數傳入。 */
object DaySections {
    /**
     * 把已經依週分好、組內由新到舊排序的 [weeks] 再切成日段，回傳的每個 list 對應同一個週組。
     * 年份分隔在「這一天的年份跟上一個出現的日段不同」時插入；第一個日段拿 [referenceDate] 的年份比，
     * 所以今年沒有記錄時最上方也會出現年份。跨年的週不拆開，分隔就插在週裡面。
     */
    fun <T> entriesOf(
        weeks: List<WeekGroup<T>>,
        referenceDate: LocalDate,
        dateOf: (T) -> LocalDate,
    ): List<List<WeekEntry<T>>> {
        var previousYear = referenceDate.year
        return weeks.map { week ->
            buildList {
                // groupBy 保留第一次出現的順序，週組內已經由新到舊，所以日段也是由新到舊
                week.items.groupBy(dateOf).forEach { (date, items) ->
                    if (date.year != previousYear) add(WeekEntry.Year(date.year))
                    previousYear = date.year
                    add(WeekEntry.Day(date, items))
                }
            }
        }
    }

    /** 「今天 · 10/28 週三」／「Today · Wed 10/28」；永遠不帶年份（TESTCASES.md T7.3）。 */
    fun dayTitle(
        date: LocalDate,
        referenceDate: LocalDate,
        labels: DayTitleLabels,
    ): String {
        val monthDay = "${date.monthNumber}/${date.dayOfMonth}"
        val weekday = labels.weekdays[date.dayOfWeek.ordinal]
        val dayText = if (labels.weekdayFirst) "$weekday $monthDay" else "$monthDay $weekday"
        val prefix =
            when (date) {
                referenceDate -> "${labels.today} · "
                referenceDate.minus(1, DateTimeUnit.DAY) -> "${labels.yesterday} · "
                else -> ""
            }
        return prefix + dayText
    }
}
