package com.clarezafinanceira.app.presentation

import androidx.lifecycle.*
import com.clarezafinanceira.app.data.repository.HistoryRepository
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.entry.EntryViewModel
import com.clarezafinanceira.app.presentation.entry.RecurrenceViewModel
import com.clarezafinanceira.app.presentation.history.HistoryViewModel
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class TemporalUpdateTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private val source = MonthlyAnalysisSource { period ->
        flowOf(MonthlyAnalysisEngine().analyze(period, movements = listOf(
            Movement("income", MovementType.INCOME, "Income", 10000, null, period.atDay(1)))))
    }

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.clear(); Dispatchers.resetMain() }
    private fun <T : ViewModel> keep(key: String, model: T): T = model.also { models.put(key, it) }
    private fun clock(start: String) = object : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("America/Sao_Paulo")
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
        override fun instant(): Instant = Instant.parse(start).plusMillis(dispatcher.scheduler.currentTime)
    }
    private fun history(clock: Clock, calendar: CurrentPeriod, handle: SavedStateHandle = SavedStateHandle()) =
        keep("history", HistoryViewModel(HistoryRepository(source, clock, calendar), clock, handle, calendar))

    @Test fun foregroundMonthBoundaryUpdatesDashboardHistoryAndCurrentMarker() = runTest(dispatcher) {
        val clock = clock("2026-10-01T02:59:59Z") // September in the injected zone.
        val calendar = CurrentPeriod(clock)
        val dashboard = keep("dashboard", MonthlyAnalysisViewModel(source, clock, calendar))
        val history = history(clock, calendar)
        backgroundScope.launch { calendar.observeWhileActive() }
        runCurrent()
        assertEquals(YearMonth.of(2026, 9), dashboard.uiState.value.period)
        assertEquals(9, history.uiState.value.items.size)
        advanceTimeBy(1000); runCurrent()
        assertEquals(YearMonth.of(2026, 10), dashboard.uiState.value.period)
        assertTrue(dashboard.uiState.value is MonthlyAnalysisUiState.Success)
        assertEquals(10, history.uiState.value.items.size)
        assertEquals(listOf(YearMonth.of(2026, 10)),
            history.uiState.value.items.filter { it.isCurrentPeriod }.map { it.period })
    }

    @Test fun foregroundYearBoundaryUpdatesCurrentYearAndNavigationLimits() = runTest(dispatcher) {
        val clock = clock("2027-01-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val dashboard = keep("dashboard", MonthlyAnalysisViewModel(source, clock, calendar))
        val history = history(clock, calendar)
        backgroundScope.launch { calendar.observeWhileActive() }
        runCurrent()
        history.selectYear(2027)
        assertEquals(2026, history.selectedYear.value)
        advanceTimeBy(1000); runCurrent()
        assertEquals(YearMonth.of(2027, 1), dashboard.uiState.value.period)
        assertEquals(2027, history.uiState.value.year)
        assertEquals(2027, history.uiState.value.currentYear)
        assertTrue(history.uiState.value.items.single().isCurrentPeriod)
        history.selectYear(2026); runCurrent()
        assertTrue(history.uiState.value.year < history.uiState.value.currentYear)
        assertTrue(history.uiState.value.items.none { it.isCurrentPeriod })
        history.selectYear(2027); runCurrent()
        assertEquals(2027, history.uiState.value.year)
        history.selectYear(2028)
        assertEquals(2027, history.selectedYear.value)
    }

    @Test fun explicitlySelectedMonthsStayFixedIncludingTheThenCurrentMonth() = runTest(dispatcher) {
        val clock = clock("2026-10-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val august = keep("august", MonthlyAnalysisViewModel(source, clock, calendar))
        val september = keep("september", MonthlyAnalysisViewModel(source, clock, calendar))
        august.selectPeriod(YearMonth.of(2026, 8))
        september.selectPeriod(YearMonth.of(2026, 9))
        backgroundScope.launch { calendar.observeWhileActive() }
        advanceTimeBy(1000); runCurrent()
        assertEquals(YearMonth.of(2026, 8), august.uiState.value.period)
        assertEquals(YearMonth.of(2026, 9), september.uiState.value.period)
    }

    @Test fun explicitlySelectedYearStaysFixedWhileCurrentYearAndMarkerChange() = runTest(dispatcher) {
        val clock = clock("2027-01-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val history = history(clock, calendar)
        history.selectYear(2026)
        backgroundScope.launch { calendar.observeWhileActive() }
        runCurrent()
        assertTrue(history.uiState.value.items.first().isCurrentPeriod)
        advanceTimeBy(1000); runCurrent()
        assertEquals(2026, history.uiState.value.year)
        assertEquals(2027, history.uiState.value.currentYear)
        assertTrue(history.uiState.value.items.none { it.isCurrentPeriod })
    }

    @Test fun savedCurrentContextFollowsNewYearButSavedExplicitSelectionDoesNot() = runTest(dispatcher) {
        val clock = clock("2027-01-01T03:00:00Z")
        val calendar = CurrentPeriod(clock)
        val current = history(clock, calendar, SavedStateHandle(mapOf("year" to 2026, "followsCalendar" to true)))
        runCurrent()
        assertEquals(2027, current.selectedYear.value)
        val selected = history(clock, calendar, SavedStateHandle(mapOf("year" to 2026, "followsCalendar" to false)))
        runCurrent()
        assertEquals(2026, selected.selectedYear.value)
        assertEquals(2027, selected.uiState.value.currentYear)
    }

    @Test fun lifecycleReturnRefreshesAfterBackgroundAndCancelsBoundaryWait() = runTest(dispatcher) {
        val clock = clock("2027-01-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val dashboard = keep("dashboard", MonthlyAnalysisViewModel(source, clock, calendar))
        val history = history(clock, calendar)
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry.createUnsafe(this)
            override val lifecycle: Lifecycle = registry
        }
        backgroundScope.launch {
            owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { calendar.observeWhileActive() }
        }
        owner.registry.currentState = Lifecycle.State.STARTED
        runCurrent()
        owner.registry.currentState = Lifecycle.State.CREATED
        runCurrent()
        advanceTimeBy(2000); runCurrent()
        assertEquals(YearMonth.of(2026, 12), calendar.period.value)
        owner.registry.currentState = Lifecycle.State.STARTED
        runCurrent()
        assertEquals(YearMonth.of(2027, 1), dashboard.uiState.value.period)
        assertEquals(2027, history.uiState.value.currentYear)
        assertTrue(history.uiState.value.items.single().isCurrentPeriod)
        owner.registry.currentState = Lifecycle.State.DESTROYED
    }

    @Test fun repositoryAlreadyCollectedRebuildsMonthsWithoutFinancialWrites() = runTest(dispatcher) {
        val clock = clock("2026-10-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val emissions = mutableListOf<List<MonthlyHistoryItem>>()
        backgroundScope.launch { HistoryRepository(source, clock, calendar).observeYearHistory(2026).collect { emissions += it } }
        runCurrent()
        advanceTimeBy(1000)
        calendar.refresh(); runCurrent()
        assertEquals(listOf(9, 10), emissions.map { it.size })
        assertFalse(emissions.last().first { it.period.monthValue == 9 }.isCurrentPeriod)
        assertTrue(emissions.last().first().isCurrentPeriod)
    }

    @Test fun openNewAndEditingDraftsKeepAllFieldsAndSaveChosenDatesAfterRollover() = runTest(dispatcher) {
        val clock = clock("2027-01-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        val saved = mutableListOf<EntryInput>()
        val store = object : FinancialEntryStore {
            override suspend fun findMovement(id: String) =
                Movement(id, MovementType.EXPENSE, "Original", 1000, ExpenseCategory.FOOD, LocalDate.of(2026, 8, 15))
            override suspend fun save(input: EntryInput, movementId: String?) { saved += input }
        }
        val new = keep("new", EntryViewModel(store, clock))
        val edit = keep("edit", EntryViewModel(store, clock, "existing"))
        runCurrent()
        new.change { it.copy(name = "Draft", amount = "12,50", category = ExpenseCategory.HOUSING,
            monthly = true, startMonth = "08/2026", habitualDay = "31") }
        edit.change { it.copy(name = "Edited", amount = "20,00") }
        val newBefore = new.state.value
        val editBefore = edit.state.value
        backgroundScope.launch { calendar.observeWhileActive() }
        advanceTimeBy(1000); runCurrent()
        assertEquals(newBefore, new.state.value)
        assertEquals(editBefore, edit.state.value)
        val later = keep("later", EntryViewModel(store, clock))
        assertEquals(LocalDate.of(2027, 1, 1), later.state.value.date)
        assertEquals("01/2027", later.state.value.startMonth)
        new.save(); edit.save(); runCurrent()
        assertEquals(listOf(newBefore.date, editBefore.date), saved.map { it.date })
        assertEquals(YearMonth.of(2026, 8), saved.first().startPeriod)
        assertEquals(31, saved.first().habitualDay)
    }

    @Test fun recurrenceDraftAndPendingScopeKeepChosenPeriodAcrossYearBoundary() = runTest(dispatcher) {
        val clock = clock("2027-01-01T02:59:59Z")
        val calendar = CurrentPeriod(clock)
        var writtenPeriod: YearMonth? = null
        var writtenChanges: RecurrenceChanges? = null
        val store = object : RecurrenceStore {
            override suspend fun findRecurrence(id: String, period: YearMonth) =
                AnalysisItem(id, ItemOrigin.RECURRENCE, MovementType.EXPENSE, "Rent", 10000,
                    ExpenseCategory.HOUSING, habitualDay = 5)
            override suspend fun updateRecurrenceForPeriod(id: String, period: YearMonth, changes: RecurrenceChanges) {
                writtenPeriod = period
                writtenChanges = changes
            }
            override suspend fun updateRecurrenceFromPeriod(id: String, period: YearMonth, changes: RecurrenceChanges) = error("Unexpected scope")
            override suspend fun stopRecurrence(id: String, period: YearMonth, keepPeriod: Boolean) = error("Unexpected stop")
            override suspend fun deleteRecurrence(id: String) = error("Unexpected delete")
        }
        val chosen = YearMonth.of(2026, 8)
        val form = keep("recurrence", RecurrenceViewModel(store, "rent", chosen))
        runCurrent()
        form.change { it.copy(name = "Draft rent", amount = "200,00", habitualDay = "15") }
        form.save()
        val draft = form.state.value
        val dialog = form.dialog.value
        backgroundScope.launch { calendar.observeWhileActive() }
        advanceTimeBy(1000); runCurrent()
        assertEquals(draft, form.state.value)
        assertEquals(dialog, form.dialog.value)
        assertEquals(chosen, form.period)
        form.applyScope(false); runCurrent()
        assertEquals(chosen, writtenPeriod)
        assertEquals(RecurrenceChanges(name = "Draft rent", amountCents = 20000, habitualDay = 15), writtenChanges)
    }
}
