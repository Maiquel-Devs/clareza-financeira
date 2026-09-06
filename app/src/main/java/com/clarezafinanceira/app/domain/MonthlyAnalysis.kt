package com.clarezafinanceira.app.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

enum class ItemOrigin { MOVEMENT, RECURRENCE }

data class AnalysisItem(
    val sourceId: String,
    val origin: ItemOrigin,
    val type: MovementType,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val date: LocalDate? = null,
    val habitualDay: Int? = null,
    val versionId: String? = null,
    val exceptionId: String? = null,
)

data class MonthlyAnalysis(
    val period: YearMonth,
    /** Only one-off income actually registered in this month. */
    val registeredIncomeCents: Long,
    val recurringIncomeCents: Long,
    /** Registered income plus effective recurring income. */
    val consideredIncomeCents: Long,
    /** Only one-off expenses; recurrences are forecasts. */
    val registeredExpenseCents: Long,
    val forecastExpenseCents: Long,
    val forecastRemainingCents: Long?,
    /** Percentage units: 150.0000 means 150%, not a ratio of 1.5. */
    val expensePercentageOfIncome: BigDecimal?,
    val expensesByCategory: Map<ExpenseCategory, Long>,
    val incomeSources: List<AnalysisItem>,
    val expenseItems: List<AnalysisItem>,
) {
    val isEmpty: Boolean get() = incomeSources.isEmpty() && expenseItems.isEmpty()
}
