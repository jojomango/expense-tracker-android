package com.jojomango.expensetracker.domain

import kotlinx.datetime.LocalDate

/**
 * 一段封閉區間 `[start, end]`（皆含端點），供 [Week] 與 [Month] 共用。
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end
}
