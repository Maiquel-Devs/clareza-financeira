package com.clarezafinanceira.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.MonthlyAnalysisSource
import com.clarezafinanceira.app.domain.CurrentPeriod
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyAnalysisViewModel(
    private val source: MonthlyAnalysisSource,
    clock: Clock,
    currentPeriod: CurrentPeriod = CurrentPeriod(clock),
) : ViewModel() {
    private var followsCalendar = true
    private val mutableSelectedPeriod = MutableStateFlow(currentPeriod.period.value)
    val selectedPeriod: StateFlow<YearMonth> = mutableSelectedPeriod.asStateFlow()

    init {
        viewModelScope.launch {
            currentPeriod.period.collect { if (followsCalendar) mutableSelectedPeriod.value = it }
        }
    }

    val uiState: StateFlow<MonthlyAnalysisUiState> = selectedPeriod.flatMapLatest { period ->
        flow<MonthlyAnalysisUiState> {
            emit(MonthlyAnalysisUiState.Loading(period))
            source.observeMonthlyAnalysis(period).collect { analysis ->
                emit(MonthlyAnalysisUiState.Success(period, analysis))
            }
        }.catch { cause -> emit(MonthlyAnalysisUiState.Error(period, cause)) }
    }.stateIn(
        scope = viewModelScope,
        // Keep analysis current while this ViewModel remains on the back stack,
        // avoiding a fresh loading state when returning to the screen.
        started = SharingStarted.Eagerly,
        initialValue = MonthlyAnalysisUiState.Loading(selectedPeriod.value),
    )

    fun selectPeriod(period: YearMonth) {
        // An explicit selection stays fixed, even if it is today's month;
        // a later calendar rollover must not move the selected consultation.
        followsCalendar = false
        mutableSelectedPeriod.value = period
    }
}
