package com.clarezafinanceira.app.domain

import java.time.LocalDate
import java.time.YearMonth

data class EntryInput(
    val type: MovementType,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val date: LocalDate,
    val monthly: Boolean = false,
    val startPeriod: YearMonth = YearMonth.from(date),
    val habitualDay: Int = date.dayOfMonth,
) {
    init {
        require(name.isNotBlank())
        require(amountCents > 0)
        require(type != MovementType.EXPENSE || category != null)
        require(!monthly || habitualDay in 1..31)
    }
}

/** Writing boundary; recurrence scope changes deliberately have no operation yet. */
interface FinancialEntryStore {
    suspend fun findMovement(id: String): Movement?
    suspend fun save(input: EntryInput, movementId: String? = null)
}
