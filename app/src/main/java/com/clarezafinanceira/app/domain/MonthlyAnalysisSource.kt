package com.clarezafinanceira.app.domain

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

/** Observation-only boundary, independent of persistence. */
fun interface MonthlyAnalysisSource {
    fun observeMonthlyAnalysis(period: YearMonth): Flow<MonthlyAnalysis>
}
