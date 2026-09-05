package com.jojomango.expensetracker.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 月份相關的曆法計算：給定日期 -> 該月的 `[start, end]` 區間。
 * 供「本月」分類支出佔比使用（TESTCASES.md T5）。
 */
object Month {
    fun rangeOf(date: LocalDate): DateRange {
        val start = LocalDate(date.year, date.month, 1)
        val end = start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return DateRange(start, end)
    }
}
