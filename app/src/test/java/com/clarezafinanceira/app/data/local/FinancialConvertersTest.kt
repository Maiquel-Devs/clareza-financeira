package com.clarezafinanceira.app.data.local

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class FinancialConvertersTest {
    private val converters = FinancialConverters()

    @Test fun datesAndPeriodsRoundTrip() {
        listOf(LocalDate.MIN, LocalDate.of(2024, 2, 29), LocalDate.MAX).forEach {
            assertEquals(it, converters.longToDate(converters.dateToLong(it)))
        }
        listOf(YearMonth.of(-999999999, 1), YearMonth.of(-1, 12),
            YearMonth.of(2026, 9), YearMonth.of(999999999, 12)).forEach {
            assertEquals(it, converters.longToPeriod(converters.periodToLong(it)))
        }
    }

    @Test fun instantsKeepNanoseconds() {
        listOf(Instant.MIN, Instant.parse("1960-01-01T00:00:00.123456789Z"),
            Instant.parse("2026-09-06T12:30:15.987654321Z"), Instant.MAX).forEach {
            assertEquals(it, converters.stringToInstant(converters.instantToString(it)))
        }
    }

    @Test fun enumsUseStableStrings() {
        MovementType.entries.forEach {
            assertEquals(it.name, converters.typeToString(it))
            assertEquals(it, converters.stringToType(converters.typeToString(it)))
        }
        ExpenseCategory.entries.forEach {
            assertEquals(it.name, converters.categoryToString(it))
            assertEquals(it, converters.stringToCategory(converters.categoryToString(it)))
        }
        assertThrows(NoSuchElementException::class.java) { converters.stringToType("0") }
    }

    @Test fun nullsRoundTrip() {
        assertNull(converters.longToDate(converters.dateToLong(null)))
        assertNull(converters.longToPeriod(converters.periodToLong(null)))
        assertNull(converters.stringToInstant(converters.instantToString(null)))
        assertNull(converters.stringToType(converters.typeToString(null)))
        assertNull(converters.stringToCategory(converters.categoryToString(null)))
    }
}
