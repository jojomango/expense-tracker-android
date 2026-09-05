package com.jojomango.expensetracker.data

import androidx.room.TypeConverter
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * Room type converters. `LocalDate` 存成 ISO 曆日字串（`YYYY-MM-DD`），`Instant`
 * 存成 epoch milliseconds——都是 Room 原生型別（String/Long）能直接索引/排序的形式。
 */
internal class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate): String = date.toString()

    @TypeConverter
    fun stringToLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @TypeConverter
    fun instantToEpochMillis(instant: Instant): Long = instant.toEpochMilliseconds()

    @TypeConverter
    fun epochMillisToInstant(value: Long): Instant = Instant.fromEpochMilliseconds(value)
}
