package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.domain.*
import java.time.YearMonth
import kotlinx.coroutines.flow.*

/** Projections only: versions, exceptions and effective amounts are resolved by the engine. */
class FinancialDetailRepository(private val monthly: MonthlyAnalysisSource) : FinancialDetailSource {
    override fun observeCategory(period: YearMonth, category: ExpenseCategory): Flow<CategoryDetail> =
        monthly.observeMonthlyAnalysis(period).map { analysis ->
            CategoryDetail(period, category, analysis.expensesByCategory[category] ?: 0L,
                analysis.expenseItems.filter { it.category == category })
        }.distinctUntilChanged()

    override fun observeItem(reference: FinancialItemReference): Flow<AnalysisItem?> =
        monthly.observeMonthlyAnalysis(reference.period).map { analysis ->
            (analysis.incomeSources + analysis.expenseItems).singleOrNull {
                it.origin == reference.origin && it.sourceId == reference.sourceId
            }
        }.distinctUntilChanged()
}
