package com.clarezafinanceira.app.presentation.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import java.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HistoryUiState(val year: Int, val currentYear: Int,
    val items: List<MonthlyHistoryItem> = emptyList(), val loading: Boolean = false,
    val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(source: HistorySource, clock: Clock,
    private val savedState: SavedStateHandle = SavedStateHandle(),
    private val currentPeriod: CurrentPeriod = CurrentPeriod(clock)) : ViewModel() {
    private val currentYear get() = currentPeriod.period.value.year
    init {
        if (!savedState.contains("followsCalendar"))
            savedState["followsCalendar"] = !savedState.contains("year")
        savedState["year"] = if (savedState.get<Boolean>("followsCalendar") == true) currentYear
            else (savedState.get<Int>("year") ?: currentYear).coerceIn(1, currentYear)
        viewModelScope.launch {
            currentPeriod.period.collect {
                if (savedState.get<Boolean>("followsCalendar") == true) savedState["year"] = it.year
            }
        }
    }
    val selectedYear = savedState.getStateFlow("year", currentYear)
    val uiState = combine(selectedYear, currentPeriod.period) { year, current -> year to current.year }
        .distinctUntilChanged().flatMapLatest { (year, currentYear) ->
        source.observeYearHistory(year).map { HistoryUiState(year, currentYear, it) }
            .onStart { emit(HistoryUiState(year, currentYear, loading = true)) }
            .catch { emit(HistoryUiState(year, currentYear, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly,
        HistoryUiState(selectedYear.value, currentYear, loading = true))

    fun selectYear(year: Int) {
        if (year in 1..currentYear) {
            savedState["followsCalendar"] = false
            savedState["year"] = year
        }
    }
}
