package com.clarezafinanceira.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

/** Pure monthly calculation: no database access, clock, formatting or carry-over. */
class MonthlyAnalysisEngine {
    fun analyze(
        period: YearMonth,
        movements: List<Movement> = emptyList(),
        recurrences: List<Recurrence> = emptyList(),
        versions: List<RecurrenceVersion> = emptyList(),
        exceptions: List<RecurrenceException> = emptyList(),
    ): MonthlyAnalysis {
        requireUnique(movements.map { it.id })
        requireUnique(recurrences.map { it.id })
        requireUnique(versions.map { it.id })
        requireUnique(exceptions.map { it.id })
        requireUnique(versions.map { it.recurrenceId to it.validFrom })
        requireUnique(exceptions.map { it.recurrenceId to it.period })

        val versionsByRecurrence = versions.groupBy { it.recurrenceId }
        val exceptionsByKey = exceptions.associateBy { it.recurrenceId to it.period }
        val punctual = movements.filter { YearMonth.from(it.date) == period }.map {
            AnalysisItem(it.id, ItemOrigin.MOVEMENT, it.type, it.name,
                it.amountCents, it.category, date = it.date)
        }
        val recurring = recurrences.mapNotNull {
            resolveRecurrence(period, it, versionsByRecurrence[it.id].orEmpty(),
                exceptionsByKey[it.id to period])
        }
        val items = (punctual + recurring)
            .onEach(::validateItem)
            .sortedWith(compareBy<AnalysisItem> { it.origin.ordinal }.thenBy { it.sourceId })
        val income = items.filter { it.type == MovementType.INCOME }
        val expenses = items.filter { it.type == MovementType.EXPENSE }
        val registeredIncome = income.filter { it.origin == ItemOrigin.MOVEMENT }.total()
        val recurringIncome = income.filter { it.origin == ItemOrigin.RECURRENCE }.total()
        val consideredIncome = Math.addExact(registeredIncome, recurringIncome)
        val forecastExpenses = expenses.total()
        val byCategory = ExpenseCategory.entries.mapNotNull { category ->
            val categoryItems = expenses.filter { it.category == category }
            if (categoryItems.isEmpty()) null else category to categoryItems.total()
        }.toMap()

        return MonthlyAnalysis(
            period = period,
            registeredIncomeCents = registeredIncome,
            recurringIncomeCents = recurringIncome,
            consideredIncomeCents = consideredIncome,
            registeredExpenseCents = expenses.filter { it.origin == ItemOrigin.MOVEMENT }.total(),
            forecastExpenseCents = forecastExpenses,
            forecastRemainingCents = if (consideredIncome > 0)
                Math.subtractExact(consideredIncome, forecastExpenses) else null,
            expensePercentageOfIncome = if (consideredIncome > 0)
                BigDecimal.valueOf(forecastExpenses).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(consideredIncome), 4, RoundingMode.HALF_UP)
                else null,
            expensesByCategory = byCategory,
            incomeSources = income,
            expenseItems = expenses,
        )
    }

    private fun resolveRecurrence(
        period: YearMonth,
        recurrence: Recurrence,
        versions: List<RecurrenceVersion>,
        exception: RecurrenceException?,
    ): AnalysisItem? {
        require(recurrence.endPeriod == null || recurrence.endPeriod >= recurrence.startPeriod)
        if (period < recurrence.startPeriod ||
            (recurrence.endPeriod != null && period > recurrence.endPeriod)) return null
        if (exception?.excluded == true) return null
        val version = requireNotNull(versions.filter { it.validFrom <= period }.maxByOrNull { it.validFrom }) {
            "Active recurrence ${recurrence.id} has no effective version for $period"
        }
        require(version.validFrom >= recurrence.startPeriod)
        return AnalysisItem(
            sourceId = recurrence.id,
            origin = ItemOrigin.RECURRENCE,
            type = recurrence.type,
            name = exception?.overrideName ?: version.name,
            amountCents = exception?.overrideAmountCents ?: version.amountCents,
            category = exception?.overrideCategory ?: version.category,
            habitualDay = exception?.overrideHabitualDay ?: version.habitualDay,
            versionId = version.id,
            exceptionId = exception?.id,
        ).also { require(it.name.isNotBlank()) }
    }

    private fun validateItem(item: AnalysisItem) {
        require(item.amountCents > 0)
        require((item.type == MovementType.EXPENSE) == (item.category != null))
        require(item.habitualDay == null || item.habitualDay in 1..31)
    }

    private fun List<AnalysisItem>.total(): Long =
        fold(0L) { total, item -> Math.addExact(total, item.amountCents) }

    private fun <T> requireUnique(keys: List<T>) {
        require(keys.size == keys.toSet().size) { "Duplicate financial input keys" }
    }
}
