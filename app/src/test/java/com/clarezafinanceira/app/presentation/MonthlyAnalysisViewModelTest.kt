package com.clarezafinanceira.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.clarezafinanceira.app.domain.MonthlyAnalysis
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import com.clarezafinanceira.app.domain.MonthlyAnalysisSource
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MonthlyAnalysisViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2031-01-01T01:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val initial = YearMonth.of(2030, 12)
    private val next = initial.plusMonths(1)
    private val store = ViewModelStore()
    private val source = ControlledSource()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun create(input: MonthlyAnalysisSource = source): MonthlyAnalysisViewModel =
        ViewModelProvider.create(store, MonthlyAnalysisViewModelFactory(input, clock))[
            MonthlyAnalysisViewModel::class.java
        ]

    private fun analysis(period: YearMonth = initial) = MonthlyAnalysisEngine().analyze(period)

    @Test fun initialPeriodUsesClockAndItsTimeZone() {
        assertEquals(initial, create().selectedPeriod.value)
    }

    @Test fun startsLoadingAndRemainsLoadingUntilEmission() = runTest(dispatcher) {
        val vm = create()
        assertEquals(MonthlyAnalysisUiState.Loading(initial), vm.uiState.value)
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Loading(initial), vm.uiState.value)
    }

    @Test fun emissionBecomesSuccessWithoutCopyingAnalysis() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        val value = analysis()
        source.stream(initial).emit(Result.success(value))
        runCurrent()
        val state = vm.uiState.value as MonthlyAnalysisUiState.Success
        assertSame(value, state.analysis)
        assertEquals(vm.selectedPeriod.value, state.period)
    }

    @Test fun selectionCancelsPreviousObservationAndLoadsNewPeriod() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        vm.selectPeriod(next)
        assertEquals(next, vm.selectedPeriod.value)
        runCurrent()
        assertEquals(setOf(next), source.active)
        assertEquals(MonthlyAnalysisUiState.Loading(next), vm.uiState.value)
        source.stream(next).emit(Result.success(analysis(next)))
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Success(next, analysis(next)), vm.uiState.value)
    }

    @Test fun laterEmissionsUpdateSelectedMonth() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        source.stream(initial).emit(Result.success(analysis()))
        runCurrent()
        // Deliberately distinctive source data: the ViewModel must forward it unchanged.
        val updated = analysis().copy(registeredIncomeCents = 12345)
        source.stream(initial).emit(Result.success(updated))
        runCurrent()
        assertSame(updated, (vm.uiState.value as MonthlyAnalysisUiState.Success).analysis)
    }

    @Test fun oldMonthCannotReplaceNewMonthState() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        vm.selectPeriod(next)
        runCurrent()
        source.stream(next).emit(Result.success(analysis(next)))
        runCurrent()
        source.stream(initial).emit(Result.success(analysis()))
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Success(next, analysis(next)), vm.uiState.value)
    }

    @Test fun sourceFailureBecomesErrorWithOriginalCause() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        val failure = IllegalStateException("Read failed")
        source.stream(initial).emit(Result.failure(failure))
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Error(initial, failure), vm.uiState.value)
    }

    @Test fun synchronousSourceFailureAlsoBecomesError() = runTest(dispatcher) {
        val failure = IllegalStateException("Cannot open source")
        val vm = create(MonthlyAnalysisSource { throw failure })
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Error(initial, failure), vm.uiState.value)
    }

    @Test fun emptyAnalysisIsSuccess() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        source.stream(initial).emit(Result.success(analysis()))
        runCurrent()
        assertTrue((vm.uiState.value as MonthlyAnalysisUiState.Success).analysis.isEmpty)
    }

    @Test fun canSelectAnotherMonthAfterError() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        source.stream(initial).emit(Result.failure(IllegalStateException()))
        runCurrent()
        vm.selectPeriod(next)
        runCurrent()
        source.stream(next).emit(Result.success(analysis(next)))
        runCurrent()
        assertEquals(MonthlyAnalysisUiState.Success(next, analysis(next)), vm.uiState.value)
    }

    @Test fun selectingSamePeriodDoesNotRestartObservation() = runTest(dispatcher) {
        val vm = create()
        runCurrent()
        vm.selectPeriod(initial)
        runCurrent()
        assertEquals(listOf(initial), source.observed)
    }

    @Test fun clearingViewModelCancelsObservation() = runTest(dispatcher) {
        create()
        runCurrent()
        store.clear()
        runCurrent()
        assertTrue(source.active.isEmpty())
    }

    @Test fun factoryRejectsUnsupportedClass() {
        assertThrows(IllegalArgumentException::class.java) {
            MonthlyAnalysisViewModelFactory(source, clock).create(ViewModel::class.java)
        }
    }

    private class ControlledSource : MonthlyAnalysisSource {
        private val streams = mutableMapOf<YearMonth, MutableSharedFlow<Result<MonthlyAnalysis>>>()
        val active = mutableSetOf<YearMonth>()
        val observed = mutableListOf<YearMonth>()
        fun stream(period: YearMonth) = streams.getOrPut(period) { MutableSharedFlow() }

        override fun observeMonthlyAnalysis(period: YearMonth): Flow<MonthlyAnalysis> = flow {
            observed += period
            active += period
            try {
                stream(period).collect { emit(it.getOrThrow()) }
            } finally {
                active -= period
            }
        }
    }
}
