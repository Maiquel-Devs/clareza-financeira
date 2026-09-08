package com.clarezafinanceira.app.presentation

import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.detail.*
import java.time.YearMonth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class FinancialDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val period = YearMonth.of(2026,8)
    private val movement = AnalysisItem("m",ItemOrigin.MOVEMENT,MovementType.EXPENSE,"Mercado",10000,
        ExpenseCategory.FOOD,date = period.atDay(7))
    private val items = MutableStateFlow<AnalysisItem?>(movement)
    private val models = mutableListOf<FinancialDetailViewModel>()
    private var calls = 0
    private val source = object : FinancialDetailSource {
        override fun observeItem(reference: FinancialItemReference) = items
        override fun observeCategory(period: YearMonth,category: ExpenseCategory) = flowOf(CategoryDetail(period,category,0,emptyList()))
    }
    private fun create(origin: ItemOrigin = ItemOrigin.MOVEMENT, store: MovementDeletionStore = MovementDeletionStore { calls++ }) =
        FinancialDetailViewModel(source,store,FinancialItemReference(origin,"m",period)).also { models.add(it) }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { models.forEach { it.viewModelScope.cancel() }; Dispatchers.resetMain() }

    @Test fun requestingDeletionRequiresConfirmationAndDoesNotWrite() = runTest(dispatcher) {
        val vm = create(); runCurrent(); vm.requestDeletion(); runCurrent()
        assertEquals(DeletionState.CONFIRMING,vm.deletionState.value)
        assertEquals(0,calls)
    }
    @Test fun cancelDoesNotDelete() = runTest(dispatcher) {
        val vm = create(); runCurrent(); vm.requestDeletion(); vm.cancelDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(DeletionState.IDLE,vm.deletionState.value)
        assertEquals(0,calls)
    }
    @Test fun confirmWithoutRequestDoesNothing() = runTest(dispatcher) {
        val vm = create(); runCurrent(); vm.confirmDeletion(); runCurrent()
        assertEquals(0,calls)
    }
    @Test fun repeatedClicksDeleteOnceAndEmitCompletion() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val vm = create(store = MovementDeletionStore { assertEquals("m",it); calls++; gate.await() })
        runCurrent(); vm.requestDeletion(); vm.confirmDeletion(); vm.confirmDeletion(); runCurrent()
        vm.requestDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(1,calls)
        assertEquals(DeletionState.DELETING,vm.deletionState.value)
        gate.complete(Unit); runCurrent()
        assertEquals(DeletionState.DELETED,vm.deletionState.value)
        vm.requestDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(1,calls)
    }
    @Test fun recurrenceCannotBeDeletedFromDetail() = runTest(dispatcher) {
        val vm = create(ItemOrigin.RECURRENCE); runCurrent(); vm.requestDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(DeletionState.IDLE,vm.deletionState.value)
        assertEquals(0,calls)
    }
    @Test fun missingItemIsUnavailableAndCannotRequestDelete() = runTest(dispatcher) {
        val vm = create(); runCurrent(); items.value = null; runCurrent(); vm.requestDeletion()
        assertNull(vm.uiState.value.item)
        assertFalse(vm.uiState.value.loading)
        assertFalse(vm.uiState.value.failed)
        assertEquals(DeletionState.IDLE,vm.deletionState.value)
    }
    @Test fun editedItemUpdatesWhileKeepingHistoricalReference() = runTest(dispatcher) {
        val vm = create(); runCurrent(); items.value = movement.copy(amountCents = 17000); runCurrent()
        assertEquals(17000L,vm.uiState.value.item!!.amountCents)
        assertEquals(period,vm.uiState.value.reference.period)
    }
    @Test fun failedDeleteAllowsNewConfirmedAttempt() = runTest(dispatcher) {
        val vm = create(store = MovementDeletionStore { calls++; if (calls == 1) error("disk") })
        runCurrent(); vm.requestDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(DeletionState.FAILED,vm.deletionState.value)
        vm.requestDeletion(); vm.confirmDeletion(); runCurrent()
        assertEquals(DeletionState.DELETED,vm.deletionState.value)
        assertEquals(2,calls)
    }
}
