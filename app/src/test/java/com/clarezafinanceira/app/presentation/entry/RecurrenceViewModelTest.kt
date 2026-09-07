package com.clarezafinanceira.app.presentation.entry

import com.clarezafinanceira.app.domain.*
import java.time.YearMonth
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class RecurrenceViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeRecurrences()
    private val period = YearMonth.of(2026,9)
    @Before fun before() { Dispatchers.setMain(dispatcher) }
    @After fun after() { Dispatchers.resetMain() }
    private fun form() = RecurrenceViewModel(store,"r",period)
    @Test fun loadsEffectiveMonthlyFields() = runTest(dispatcher) {
        val vm = form(); runCurrent()
        assertTrue(vm.state.value.monthly); assertTrue(vm.state.value.editing)
        assertEquals("100,00",vm.state.value.amount); assertEquals("5",vm.state.value.habitualDay)
        assertNull(vm.dialog.value)
    }
    @Test fun scopeIsRequestedOnlyAfterEditedFormIsSaved() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.change { it.copy(amount = "150,00") }
        assertNull(vm.dialog.value); vm.save()
        assertEquals(RecurrenceDialog.SCOPE,vm.dialog.value); assertEquals(0,store.calls)
        vm.applyScope(false); runCurrent()
        assertEquals("period",store.operation); assertEquals(15000L,store.patch!!.amountCents)
        assertNull(store.patch!!.name); assertTrue(vm.state.value.saved)
    }
    @Test fun permanentScopeExpressesIntent() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.change { it.copy(name = "Nova") }; vm.save(); vm.applyScope(true); runCurrent()
        assertEquals("from",store.operation); assertEquals("Nova",store.patch!!.name)
    }
    @Test fun unchangedSaveDoesNotAskScopeOrWrite() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.change { it.copy(name = " Base ") }; vm.save(); runCurrent()
        assertNull(vm.dialog.value); assertEquals(0,store.calls); assertTrue(vm.state.value.saved)
    }
    @Test fun cancellingAllDialogsNeverWrites() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.change { it.copy(amount = "150,00") }; vm.save(); vm.dismiss()
        vm.applyScope(true); vm.requestStop(); vm.dismiss(); vm.stop(false)
        vm.requestDelete(); vm.dismiss(); vm.delete(); runCurrent()
        assertEquals(0,store.calls); assertEquals("150,00",vm.state.value.amount)
    }
    @Test fun stopNeedsConfirmationAndForwardsKeepChoice() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.stop(true); assertEquals(0,store.calls)
        vm.requestStop(); assertEquals(RecurrenceDialog.STOP,vm.dialog.value)
        vm.stop(true); runCurrent(); assertEquals("stop:true",store.operation)
    }
    @Test fun stopRemovingForwardsChoice() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.requestStop(); vm.stop(false); runCurrent()
        assertEquals("stop:false",store.operation)
    }
    @Test fun deleteNeedsExplicitConfirmation() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.delete(); assertEquals(0,store.calls)
        vm.requestDelete(); assertEquals(RecurrenceDialog.DELETE,vm.dialog.value)
        vm.delete(); runCurrent(); assertEquals("delete",store.operation)
    }
    @Test fun repeatedClickCannotDuplicateAnyOperation() = runTest(dispatcher) {
        for (action in RecurrenceDialog.entries) {
            val vm = form(); runCurrent(); store.gate = CompletableDeferred()
            when(action) {
                RecurrenceDialog.SCOPE -> { vm.change { it.copy(amount = "150,00") }; vm.save() }
                RecurrenceDialog.STOP -> vm.requestStop()
                RecurrenceDialog.DELETE -> vm.requestDelete()
            }
            val confirm = { when(action) {
                RecurrenceDialog.SCOPE -> vm.applyScope(false)
                RecurrenceDialog.STOP -> vm.stop(false)
                RecurrenceDialog.DELETE -> vm.delete()
            } }
            val previous = store.calls
            confirm(); confirm(); runCurrent(); confirm(); runCurrent()
            assertEquals(previous+1,store.calls); assertTrue(vm.state.value.saving)
            store.gate!!.complete(Unit); runCurrent(); confirm(); runCurrent()
            assertEquals(previous+1,store.calls)
        }
    }
    @Test fun persistenceFailureKeepsEditsAndAllowsRetry() = runTest(dispatcher) {
        val vm = form(); runCurrent(); store.fail = true
        vm.change { it.copy(amount = "150,00") }; vm.save(); vm.applyScope(false); runCurrent()
        assertFalse(vm.state.value.saved); assertNotNull(vm.state.value.error)
        assertEquals("150,00",vm.state.value.amount)
        store.fail = false; vm.save(); vm.applyScope(false); runCurrent(); assertTrue(vm.state.value.saved)
    }
    @Test fun stopAndDeleteFailuresDoNotReportSuccess() = runTest(dispatcher) {
        val vm = form(); runCurrent(); store.fail = true
        vm.requestStop(); vm.stop(false); runCurrent(); assertFalse(vm.state.value.saved)
        vm.requestDelete(); vm.delete(); runCurrent(); assertFalse(vm.state.value.saved)
        assertNotNull(vm.state.value.error)
    }
    @Test fun loadFailureBlocksWrites() = runTest(dispatcher) {
        store.fail = true; val vm = form(); runCurrent(); vm.save(); vm.requestDelete(); vm.delete()
        assertTrue(vm.state.value.loadFailed); assertEquals(0,store.calls)
    }
    @Test fun invalidFieldsDoNotOpenScope() = runTest(dispatcher) {
        val vm = form(); runCurrent(); vm.change { it.copy(name = "", amount = "0", habitualDay = "32", category = null) }; vm.save()
        assertEquals(setOf("name","amount","day","category"),vm.state.value.errors.keys)
        assertNull(vm.dialog.value)
    }
    @Test fun periodFormatterIsExplicitIncludingYearRollover() {
        assertEquals("setembro de 2026",period.portuguesePeriod())
        assertEquals("janeiro de 2027",period.withMonth(12).plusMonths(1).portuguesePeriod())
    }
    private class FakeRecurrences : RecurrenceStore {
        var calls = 0; var operation: String? = null; var patch: RecurrenceChanges? = null
        var fail = false; var gate: CompletableDeferred<Unit>? = null
        override suspend fun findRecurrence(id: String,period: YearMonth): AnalysisItem {
            if(fail) error("test")
            return AnalysisItem(id,ItemOrigin.RECURRENCE,MovementType.EXPENSE,"Base",10000L,ExpenseCategory.HOUSING,habitualDay = 5)
        }
        private suspend fun write(op: String, changes: RecurrenceChanges? = null) {
            calls++; operation = op; patch = changes; gate?.await(); if(fail) error("test")
        }
        override suspend fun updateRecurrenceForPeriod(id: String,period: YearMonth,changes: RecurrenceChanges) = write("period",changes)
        override suspend fun updateRecurrenceFromPeriod(id: String,period: YearMonth,changes: RecurrenceChanges) = write("from",changes)
        override suspend fun stopRecurrence(id: String,period: YearMonth,keepPeriod: Boolean) = write("stop:$keepPeriod")
        override suspend fun deleteRecurrence(id: String) = write("delete")
    }
}
