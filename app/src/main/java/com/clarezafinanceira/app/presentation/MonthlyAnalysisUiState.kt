package com.clarezafinanceira.app.presentation

import com.clarezafinanceira.app.domain.MonthlyAnalysis
import java.time.YearMonth

sealed interface MonthlyAnalysisUiState {
    val period: YearMonth

    data class Loading(override val period: YearMonth) : MonthlyAnalysisUiState
    data class Success(
        override val period: YearMonth,
        val analysis: MonthlyAnalysis,
    ) : MonthlyAnalysisUiState
    data class Error(
        override val period: YearMonth,
        val cause: Throwable,
    ) : MonthlyAnalysisUiState
}
