package com.clarezafinanceira.app.presentation

import androidx.lifecycle.SavedStateHandle
import com.clarezafinanceira.app.domain.HistorySource
import com.clarezafinanceira.app.presentation.history.HistoryViewModel
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun selectingYearSwitchesSourceAndEmptyState() = runTest(dispatcher) {
        val years = mutableListOf<Int>()
        val vm = HistoryViewModel(HistorySource { years.add(it); flowOf(emptyList()) },clock)
        runCurrent()
        vm.selectYear(2025)
        runCurrent()
        assertEquals(listOf(2026,2025),years)
        assertEquals(2025,vm.uiState.value.year)
        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.items.isEmpty())
    }
    @Test fun futureYearIsRejected() {
        val vm = HistoryViewModel(HistorySource { flowOf(emptyList()) },clock)
        vm.selectYear(2027)
        assertEquals(2026,vm.selectedYear.value)
    }
    @Test fun selectedYearIsSavedAndRestored() {
        val handle = SavedStateHandle()
        HistoryViewModel(HistorySource { flowOf(emptyList()) },clock,handle).selectYear(2024)
        val restored = HistoryViewModel(HistorySource { flowOf(emptyList()) },clock,
            SavedStateHandle(mapOf("year" to handle.get<Int>("year"))))
        assertEquals(2024,restored.selectedYear.value)
    }
    @Test fun failureIsNotReportedAsEmptyYear() = runTest(dispatcher) {
        val vm = HistoryViewModel(HistorySource { flow { error("unavailable") } },clock)
        runCurrent()
        assertTrue(vm.uiState.value.failed)
        assertFalse(vm.uiState.value.loading)
    }
}
