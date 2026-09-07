package com.clarezafinanceira.app.domain

import java.time.YearMonth

/** Null means unchanged; income never accepts a category. */
data class RecurrenceChanges(
    val name: String? = null,
    val amountCents: Long? = null,
    val category: ExpenseCategory? = null,
    val habitualDay: Int? = null,
) {
    init {
        require(name == null || name.isNotBlank())
        require(amountCents == null || amountCents > 0)
        require(habitualDay == null || habitualDay in 1..31)
    }
    val isEmpty: Boolean get() = this == RecurrenceChanges()
}

interface RecurrenceStore {
    suspend fun findRecurrence(id: String, period: YearMonth): AnalysisItem?
    suspend fun updateRecurrenceForPeriod(id: String, period: YearMonth, changes: RecurrenceChanges)
    suspend fun updateRecurrenceFromPeriod(id: String, period: YearMonth, changes: RecurrenceChanges)
    suspend fun stopRecurrence(id: String, period: YearMonth, keepPeriod: Boolean)
    suspend fun deleteRecurrence(id: String)
}
