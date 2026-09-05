package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 一週的交易分組結果：[range] 為該週區間，[items] 為落在該週、依日期排序過的項目。
 */
data class WeekGroup<T>(
    val range: DateRange,
    val items: List<T>,
)

/**
 * 週相關的曆法計算：給定日期 + 週起始日 -> 該週的 `[start, end]` 區間，
 * 以及把任意帶日期的清單依週分組。純函式，零 Android 依賴（見 SPEC.md P1）。
 */
object Week {
    /**
     * [date] 所屬的那一週的區間，週首由 [weekStartDay] 決定。
     */
    fun rangeOf(
        date: LocalDate,
        weekStartDay: DayOfWeek,
    ): DateRange {
        val offset = (date.dayOfWeek.value - weekStartDay.value + 7) % 7
        val start = date.minus(offset, DateTimeUnit.DAY)
        val end = start.plus(6, DateTimeUnit.DAY)
        return DateRange(start, end)
    }

    /**
     * 把 [items] 依各自的日期（由 [dateOf] 取得）分到所屬的週。
     * 組間依週首**倒序**（最新的週在前），組內依日期**正序**排序。
     */
    fun <T> groupByWeek(
        items: List<T>,
        weekStartDay: DayOfWeek,
        dateOf: (T) -> LocalDate,
    ): List<WeekGroup<T>> {
        if (items.isEmpty()) return emptyList()
        return items
            .groupBy { rangeOf(dateOf(it), weekStartDay) }
            .entries
            .sortedByDescending { (range, _) -> range.start }
            .map { (range, groupItems) -> WeekGroup(range, groupItems.sortedBy(dateOf)) }
    }
}
