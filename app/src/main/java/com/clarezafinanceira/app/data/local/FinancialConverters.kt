package com.clarezafinanceira.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoField

class FinancialConverters {
    @TypeConverter fun dateToLong(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun longToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    // Proleptic month keeps SQLite comparisons chronological, including negative years.
    @TypeConverter fun periodToLong(value: YearMonth?): Long? = value?.getLong(ChronoField.PROLEPTIC_MONTH)
    @TypeConverter fun longToPeriod(value: Long?): YearMonth? = value?.let {
        YearMonth.of(Math.floorDiv(it, 12).toInt(), Math.floorMod(it, 12) + 1)
    }

    // ISO-8601 preserves nanoseconds; epoch milliseconds would lose precision.
    @TypeConverter fun instantToString(value: Instant?): String? = value?.toString()
    @TypeConverter fun stringToInstant(value: String?): Instant? = value?.let(Instant::parse)
    @TypeConverter fun typeToString(value: MovementType?): String? = value?.storageCode
    @TypeConverter fun stringToType(value: String?): MovementType? =
        value?.let { code -> MovementType.entries.single { it.storageCode == code } }
    @TypeConverter fun categoryToString(value: ExpenseCategory?): String? = value?.storageCode
    @TypeConverter fun stringToCategory(value: String?): ExpenseCategory? =
        value?.let { code -> ExpenseCategory.entries.single { it.storageCode == code } }
}
