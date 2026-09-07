package com.clarezafinanceira.app.presentation.entry

import com.clarezafinanceira.app.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class EntryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-09-07T01:00:00Z"), ZoneId.of("America/Sao_Paulo"))
    private val store = FakeStore()
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }
    private fun form() = EntryViewModel(store, clock)
    private fun valid(vm: EntryViewModel) { vm.change { it.copy(name = "  Venda  ", amount = "10,50", type = MovementType.INCOME) } }

    @Test fun initialStateUsesLocalToday() {
        val state = form().state.value
        assertEquals(LocalDate.of(2026, 9, 6), state.date)
        assertEquals("09/2026", state.startMonth)
        assertFalse(state.monthly)
        assertEquals(MovementType.EXPENSE, state.type)
        assertEquals("", state.amount)
    }
    @Test fun invalidFieldsShowErrors() {
        val vm = form(); vm.save()
        assertEquals(setOf("name", "category", "amount"), vm.state.value.errors.keys)
        assertEquals(0, store.calls)
    }
    @Test fun saveSuccessTrimsAndEmitsSuccess() = runTest(dispatcher) {
        val vm = form(); valid(vm); vm.save(); runCurrent()
        assertTrue(vm.state.value.saved)
        assertEquals("Venda", store.input!!.name)
        assertEquals(1050L, store.input!!.amountCents)
        assertEquals(LocalDate.of(2026, 9, 6), store.input!!.date)
    }
    @Test fun customDateAndIncomeCategoryAreNormalized() = runTest(dispatcher) {
        val vm = form(); valid(vm)
        vm.change { it.copy(date = LocalDate.of(2025, 2, 3), category = ExpenseCategory.FOOD) }
        vm.save(); runCurrent()
        assertEquals(LocalDate.of(2025, 2, 3), store.input!!.date)
        assertNull(store.input!!.category)
    }
    @Test fun failureKeepsFieldsAndAllowsRetry() = runTest(dispatcher) {
        val vm = form(); valid(vm); store.fail = true; vm.save(); runCurrent()
        assertFalse(vm.state.value.saved); assertFalse(vm.state.value.saving)
        assertNotNull(vm.state.value.error); assertEquals("10,50", vm.state.value.amount)
        store.fail = false; vm.save(); runCurrent(); assertTrue(vm.state.value.saved)
    }
    @Test fun repeatedSaveCannotDuplicateWhilePendingOrAfterSuccess() = runTest(dispatcher) {
        val vm = form(); valid(vm); store.gate = CompletableDeferred()
        vm.save(); vm.save(); runCurrent(); vm.save()
        assertEquals(1, store.calls); assertTrue(vm.state.value.saving)
        vm.change { it.copy(name = "changed") }; assertEquals("  Venda  ", vm.state.value.name)
        store.gate!!.complete(Unit); runCurrent(); vm.save(); runCurrent()
        assertEquals(1, store.calls)
    }
    @Test fun monthlyFieldsAreValidated() {
        val vm = form(); valid(vm)
        vm.change { it.copy(monthly = true, habitualDay = "32", startMonth = "13/2026") }; vm.save()
        assertEquals(setOf("month", "day"), vm.state.value.errors.keys)
    }
    @Test fun monthlySwitchRetainsFieldsButIgnoresHiddenErrors() = runTest(dispatcher) {
        val vm = form(); valid(vm)
        vm.change { it.copy(monthly = true, habitualDay = "bad", startMonth = "bad") }
        assertTrue(vm.state.value.monthly)
        vm.change { it.copy(monthly = false) }; vm.save(); runCurrent()
        assertTrue(vm.state.value.saved); assertFalse(store.input!!.monthly)
    }
    @Test fun monthlySavesChosenMonthAndDay() = runTest(dispatcher) {
        val vm = form(); valid(vm)
        vm.change { it.copy(monthly = true, habitualDay = "31", startMonth = "02/2027") }
        vm.save(); runCurrent()
        assertEquals(YearMonth.of(2027, 2), store.input!!.startPeriod)
        assertEquals(31, store.input!!.habitualDay)
    }
    @Test fun editingLoadsAndPreservesIdentityAndKind() = runTest(dispatcher) {
        val vm = EntryViewModel(store, clock, "existing"); assertTrue(vm.state.value.loading); runCurrent()
        assertTrue(vm.state.value.editing); assertEquals("Original", vm.state.value.name)
        vm.change { it.copy(name = "Edited", monthly = true, type = MovementType.EXPENSE) }
        assertFalse(vm.state.value.monthly); assertEquals(MovementType.INCOME, vm.state.value.type)
        vm.save(); runCurrent(); assertEquals("existing", store.id)
    }
    @Test fun missingMovementCannotBeSavedAsNew() = runTest(dispatcher) {
        store.missing = true
        val vm = EntryViewModel(store, clock, "missing"); runCurrent(); vm.save(); runCurrent()
        assertTrue(vm.state.value.loadFailed); assertEquals(0, store.calls)
    }
    @Test fun monthRange() {
        listOf("00/2026", "12/0000", "9/2026", "09/26").forEach { assertNull(EntryViewModel.parseMonth(it)) }
    }

    private class FakeStore : FinancialEntryStore {
        var calls = 0; var fail = false; var missing = false
        var input: EntryInput? = null; var id: String? = null
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun findMovement(id: String) = if (missing) null else
            Movement(id, MovementType.INCOME, "Original", 100L, null, LocalDate.of(2026, 9, 1))
        override suspend fun save(input: EntryInput, movementId: String?) {
            calls++; gate?.await(); if (fail) error("disk")
            this.input = input; id = movementId
        }
    }
}
