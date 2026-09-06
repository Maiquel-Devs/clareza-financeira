package com.clarezafinanceira.app.data.local

import com.clarezafinanceira.app.domain.Movement
import com.clarezafinanceira.app.domain.Recurrence
import com.clarezafinanceira.app.domain.RecurrenceException
import com.clarezafinanceira.app.domain.RecurrenceVersion
import com.clarezafinanceira.app.domain.MovementType as DomainMovementType
import com.clarezafinanceira.app.domain.ExpenseCategory as DomainExpenseCategory

fun MovementEntity.toDomain() = Movement(id, type.toDomain(), name, amountCents, category?.toDomain(), date)
fun RecurrenceEntity.toDomain() = Recurrence(id, type.toDomain(), startPeriod, endPeriod)
fun RecurrenceVersionEntity.toDomain() = RecurrenceVersion(
    id, recurrenceId, validFrom, name, amountCents, category?.toDomain(), habitualDay,
)
fun RecurrenceExceptionEntity.toDomain() = RecurrenceException(
    id, recurrenceId, period, overrideName, overrideAmountCents,
    overrideCategory?.toDomain(), overrideHabitualDay, excluded,
)

private fun MovementType.toDomain(): DomainMovementType = when (this) {
    MovementType.INCOME -> DomainMovementType.INCOME
    MovementType.EXPENSE -> DomainMovementType.EXPENSE
}
private fun ExpenseCategory.toDomain(): DomainExpenseCategory = when (this) {
    ExpenseCategory.FOOD -> DomainExpenseCategory.FOOD
    ExpenseCategory.HOUSING -> DomainExpenseCategory.HOUSING
    ExpenseCategory.TRANSPORT -> DomainExpenseCategory.TRANSPORT
    ExpenseCategory.LEISURE -> DomainExpenseCategory.LEISURE
    ExpenseCategory.HEALTH -> DomainExpenseCategory.HEALTH
    ExpenseCategory.OTHER -> DomainExpenseCategory.OTHER
}
