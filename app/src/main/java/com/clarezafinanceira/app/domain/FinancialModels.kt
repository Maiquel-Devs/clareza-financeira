package com.clarezafinanceira.app.domain

import java.time.LocalDate
import java.time.YearMonth

enum class MovementType { INCOME, EXPENSE }
enum class ExpenseCategory { FOOD, HOUSING, TRANSPORT, LEISURE, HEALTH, OTHER }

// Only financial inputs are needed here; persistence timestamps stay in the data layer.
data class Movement(
    val id: String,
    val type: MovementType,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val date: LocalDate,
)

data class Recurrence(
    val id: String,
    val type: MovementType,
    val startPeriod: YearMonth,
    val endPeriod: YearMonth? = null,
)

data class RecurrenceVersion(
    val id: String,
    val recurrenceId: String,
    val validFrom: YearMonth,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val habitualDay: Int,
)

data class RecurrenceException(
    val id: String,
    val recurrenceId: String,
    val period: YearMonth,
    val overrideName: String? = null,
    val overrideAmountCents: Long? = null,
    val overrideCategory: ExpenseCategory? = null,
    val overrideHabitualDay: Int? = null,
    val excluded: Boolean = false,
)
