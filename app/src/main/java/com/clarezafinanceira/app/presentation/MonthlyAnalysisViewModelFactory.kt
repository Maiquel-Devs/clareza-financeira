package com.clarezafinanceira.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clarezafinanceira.app.domain.MonthlyAnalysisSource
import java.time.Clock
import com.clarezafinanceira.app.domain.CurrentPeriod

class MonthlyAnalysisViewModelFactory(
    private val source: MonthlyAnalysisSource,
    private val clock: Clock,
    private val currentPeriod: CurrentPeriod = CurrentPeriod(clock),
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == MonthlyAnalysisViewModel::class.java) {
            "Unsupported ViewModel: ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return MonthlyAnalysisViewModel(source, clock, currentPeriod) as T
    }
}
