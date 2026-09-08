package com.clarezafinanceira.app.domain

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/** Stable identity; effective display data always comes from the selected month's analysis. */
data class FinancialItemReference(val origin: ItemOrigin, val sourceId: String, val period: YearMonth)

data class CategoryDetail(val period: YearMonth, val category: ExpenseCategory,
    val totalCents: Long, val items: List<AnalysisItem>)

interface FinancialDetailSource {
    fun observeCategory(period: YearMonth, category: ExpenseCategory): Flow<CategoryDetail>
    fun observeItem(reference: FinancialItemReference): Flow<AnalysisItem?>
}

fun interface MovementDeletionStore {
    suspend fun deleteMovement(id: String)
}
