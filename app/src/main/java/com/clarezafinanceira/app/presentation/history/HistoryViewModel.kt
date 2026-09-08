package com.clarezafinanceira.app.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

data class HistoryUiState(val year: Int, val currentYear: Int,
    val items: List<MonthlyHistoryItem> = emptyList(), val loading: Boolean = false,
    val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(source: HistorySource, clock: Clock,
    private val savedState: SavedStateHandle = SavedStateHandle()) : ViewModel() {
    private val currentYear = YearMonth.now(clock).year
    init { savedState["year"] = (savedState.get<Int>("year") ?: currentYear).coerceIn(1, currentYear) }
    val selectedYear = savedState.getStateFlow("year", currentYear)
    val uiState = selectedYear.flatMapLatest { year ->
        source.observeYearHistory(year).map { HistoryUiState(year, currentYear, it) }
            .onStart { emit(HistoryUiState(year, currentYear, loading = true)) }
            .catch { emit(HistoryUiState(year, currentYear, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        HistoryUiState(selectedYear.value, currentYear, loading = true))

    fun selectYear(year: Int) { if (year in 1..currentYear) savedState["year"] = year }
}
