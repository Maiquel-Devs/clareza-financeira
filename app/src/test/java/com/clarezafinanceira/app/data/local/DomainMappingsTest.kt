package com.clarezafinanceira.app.data.local

import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import com.clarezafinanceira.app.domain.MovementType as DomainMovementType
import com.clarezafinanceira.app.domain.ExpenseCategory as DomainExpenseCategory
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class DomainMappingsTest {
    private val now = Instant.parse("2026-09-06T12:00:00Z")
    private val period = YearMonth.of(2026, 9)

    @Test fun movementMappingPreservesEveryFinancialFieldAndEveryCategory() {
        ExpenseCategory.entries.forEach { category ->
            val entity = MovementEntity("m", MovementType.EXPENSE, "Expense", 123,
                category, LocalDate.of(2026, 9, 6), now, now)
            val mapped = entity.toDomain()
            assertEquals(entity.id, mapped.id)
            assertEquals(entity.name, mapped.name)
            assertEquals(entity.amountCents, mapped.amountCents)
            assertEquals(entity.date, mapped.date)
            assertEquals(DomainMovementType.EXPENSE, mapped.type)
            assertEquals(DomainExpenseCategory.valueOf(category.name), mapped.category)
        }
        val income = MovementEntity("i", MovementType.INCOME, "Income", 500,
            null, period.atDay(1), now, now).toDomain()
        assertEquals(DomainMovementType.INCOME, income.type)
        assertNull(income.category)
    }

    @Test fun recurrenceAndVersionMappingPreservesHistoryFields() {
        val recurrence = RecurrenceEntity("r", MovementType.EXPENSE, period, period.plusMonths(3), now, now)
        val version = RecurrenceVersionEntity("v", "r", period, "Rent", 10000,
            ExpenseCategory.HOUSING, 31, now)
        val mapped = recurrence.toDomain()
        assertEquals(recurrence.id, mapped.id)
        assertEquals(DomainMovementType.EXPENSE, mapped.type)
        assertEquals(recurrence.startPeriod, mapped.startPeriod)
        assertEquals(recurrence.endPeriod, mapped.endPeriod)
        val mappedVersion = version.toDomain()
        assertEquals(version.id, mappedVersion.id)
        assertEquals(version.recurrenceId, mappedVersion.recurrenceId)
        assertEquals(version.validFrom, mappedVersion.validFrom)
        assertEquals(version.name, mappedVersion.name)
        assertEquals(version.amountCents, mappedVersion.amountCents)
        assertEquals(DomainExpenseCategory.HOUSING, mappedVersion.category)
        assertEquals(version.habitualDay, mappedVersion.habitualDay)
    }

    @Test fun exceptionMappingPreservesOverridesAndExclusion() {
        val entity = RecurrenceExceptionEntity("e", "r", period, "Holiday", 100,
            ExpenseCategory.LEISURE, 20, false, now, now)
        val mapped = entity.toDomain()
        assertEquals(entity.id, mapped.id)
        assertEquals(entity.recurrenceId, mapped.recurrenceId)
        assertEquals(entity.period, mapped.period)
        assertEquals(entity.overrideName, mapped.overrideName)
        assertEquals(entity.overrideAmountCents, mapped.overrideAmountCents)
        assertEquals(DomainExpenseCategory.LEISURE, mapped.overrideCategory)
        assertEquals(entity.overrideHabitualDay, mapped.overrideHabitualDay)
        assertFalse(mapped.excluded)
        assertTrue(entity.copy(excluded = true).toDomain().excluded)
        val empty = entity.copy(overrideName = null, overrideAmountCents = null,
            overrideCategory = null, overrideHabitualDay = null).toDomain()
        assertNull(empty.overrideName)
        assertNull(empty.overrideAmountCents)
        assertNull(empty.overrideCategory)
        assertNull(empty.overrideHabitualDay)
    }

    @Test fun mappedPersistenceInputsFeedEngineWithoutDatabase() {
        val income = MovementEntity("i", MovementType.INCOME, "Salary", 100000, null,
            period.atDay(1), now, now)
        val recurrence = RecurrenceEntity("r", MovementType.EXPENSE, period, null, now, now)
        val version = RecurrenceVersionEntity("v", "r", period, "Rent", 50000,
            ExpenseCategory.HOUSING, 31, now)
        val exception = RecurrenceExceptionEntity("e", "r", period, null, 40000, null,
            null, false, now, now)
        val result = MonthlyAnalysisEngine().analyze(period, listOf(income.toDomain()),
            listOf(recurrence.toDomain()), listOf(version.toDomain()), listOf(exception.toDomain()))
        assertEquals(60000L, result.forecastRemainingCents)
        assertEquals(40000L, result.forecastExpenseCents)
        assertEquals(0L, result.registeredExpenseCents)
    }
}
