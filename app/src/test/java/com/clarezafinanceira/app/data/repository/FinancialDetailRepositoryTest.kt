package com.clarezafinanceira.app.data.repository

import androidx.room.Room
import com.clarezafinanceira.app.data.local.*
import com.clarezafinanceira.app.domain.MonthlyAnalysis
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.math.BigDecimal
import java.time.Instant
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class FinancialDetailRepositoryTest {
    private lateinit var db: FinancialDatabase
    private lateinit var repository: MonthlyAnalysisRepository
    private val month = YearMonth.of(2026, 9)
    private val now = Instant.parse("2026-09-01T00:00:00Z")
    private fun movement(id: String = "m", type: MovementType = MovementType.EXPENSE,
        amount: Long = 10000, period: YearMonth = month) = MovementEntity(
        id, type, id, amount, if (type == MovementType.EXPENSE) ExpenseCategory.FOOD else null,
        period.atDay(15), now, now)
    private fun recurrence(start: YearMonth = month, end: YearMonth? = null,
        type: MovementType = MovementType.EXPENSE) = RecurrenceEntity("r", type, start, end, now, now)
    private fun version(r: RecurrenceEntity, amount: Long = 20000,
        period: YearMonth = r.startPeriod, id: String = "v") = RecurrenceVersionEntity(
        id, r.id, period, "Recurring", amount,
        if (r.type == MovementType.EXPENSE) ExpenseCategory.HOUSING else null, 31, now)
    private fun exception(period: YearMonth = month, excluded: Boolean = false) =
        RecurrenceExceptionEntity("e", "r", period, null, 5000, null, null, excluded, now, now)
    private suspend fun saveRecurrence(r: RecurrenceEntity = recurrence()) {
        db.recurrenceDao().create(r, version(r))
    }

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(),
            FinancialDatabase::class.java).build()
        repository = MonthlyAnalysisRepository(db)
    }
    @After fun close() { db.close() }

    private val category get() = com.clarezafinanceira.app.domain.ExpenseCategory.HOUSING
    private val details get() = FinancialDetailRepository(repository)
    private suspend fun list(period: YearMonth = month, selected: com.clarezafinanceira.app.domain.ExpenseCategory = category) =
        withTimeout(10000) { details.observeCategory(period,selected).first() }
    private suspend fun item(id: String = "r", origin: com.clarezafinanceira.app.domain.ItemOrigin = com.clarezafinanceira.app.domain.ItemOrigin.RECURRENCE,
        period: YearMonth = month) = withTimeout(10000) { details.observeItem(com.clarezafinanceira.app.domain.FinancialItemReference(origin,id,period)).first() }

    @Test fun categoryContainsPunctualExpense() = runBlocking {
        db.movementDao().insert(movement().copy(category = ExpenseCategory.HOUSING))
        assertEquals("m",list().items.single().sourceId)
    }
    @Test fun categoryContainsEffectiveRecurrence() = runBlocking {
        saveRecurrence()
        assertEquals("r",list().items.single().sourceId)
    }
    @Test fun categoryFiltersOtherCategoriesAndIncome() = runBlocking {
        db.movementDao().insert(movement())
        saveRecurrence(recurrence(type = MovementType.INCOME))
        assertTrue(list().items.isEmpty())
    }
    @Test fun versionChangesListAndDetailStartingInCorrectMonth() = runBlocking {
        val r = recurrence(start = month.minusMonths(1))
        saveRecurrence(r)
        db.recurrenceDao().addVersion(version(r,30000,month,"v2"))
        assertEquals(20000L,list(month.minusMonths(1)).totalCents)
        assertEquals(30000L,list().items.single().amountCents)
        assertEquals(30000L,item()!!.amountCents)
        assertEquals("v2",item()!!.versionId)
    }
    @Test fun exceptionUsesEffectiveAmountNameDayAndCategory() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception().copy(overrideName = "Especial",overrideHabitualDay = 9,
            overrideCategory = ExpenseCategory.FOOD))
        assertTrue(list().items.isEmpty())
        val effective = list(selected = com.clarezafinanceira.app.domain.ExpenseCategory.FOOD).items.single()
        assertEquals(5000L,effective.amountCents)
        assertEquals("Especial",effective.name)
        assertEquals(9,effective.habitualDay)
        assertEquals(effective,item())
    }
    @Test fun exceptionDoesNotChangeOtherMonths() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(1)))
        db.recurrenceDao().addException(exception())
        assertEquals(5000L,list().totalCents)
        assertEquals(20000L,item(period = month.minusMonths(1))!!.amountCents)
    }
    @Test fun excludedOccurrenceIsAbsentFromListAndDetail() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception(excluded = true))
        assertTrue(list().items.isEmpty())
        assertNull(item())
    }
    @Test fun beforeStartHasNoItem() = runBlocking {
        saveRecurrence(recurrence(start = month.plusMonths(1)))
        assertTrue(list().items.isEmpty())
        assertNull(item())
    }
    @Test fun afterEndHasNoItem() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(2),end = month.minusMonths(1)))
        assertTrue(list().items.isEmpty())
        assertNull(item())
    }
    @Test fun totalAndCountMatchAnalysisWithMixedOrigins() = runBlocking {
        saveRecurrence()
        db.movementDao().insert(movement().copy(category = ExpenseCategory.HOUSING))
        assertEquals(2,list().items.size)
        assertEquals(repository.getMonthlyAnalysis(month).expensesByCategory[category],list().totalCents)
        assertEquals(30000L,list().totalCents)
    }
    @Test fun sameIdAcrossOriginsDoesNotConfuseIdentity() = runBlocking {
        saveRecurrence()
        db.movementDao().insert(movement("r",amount = 1234))
        assertEquals(20000L,item()!!.amountCents)
        assertEquals(1234L,item(origin = com.clarezafinanceira.app.domain.ItemOrigin.MOVEMENT)!!.amountCents)
    }
    @Test fun punctualIncomeHasDateWithoutCategory() = runBlocking {
        db.movementDao().insert(movement(type = MovementType.INCOME))
        val result = item("m",com.clarezafinanceira.app.domain.ItemOrigin.MOVEMENT)!!
        assertEquals(month.atDay(15),result.date)
        assertNull(result.category)
    }
    @Test fun recurringIncomeHasEffectiveHabitualDay() = runBlocking {
        saveRecurrence(recurrence(type = MovementType.INCOME))
        assertEquals(31,item()!!.habitualDay)
        assertNull(item()!!.category)
    }
    @Test fun historicalReferenceResolvesHistoricalVersion() = runBlocking {
        val r = recurrence(start = month.minusMonths(1))
        saveRecurrence(r)
        db.recurrenceDao().addVersion(version(r,30000,month,"new"))
        assertEquals(20000L,item(period = month.minusMonths(1))!!.amountCents)
    }
    @Test fun missingIdIsUnavailable() = runBlocking { assertNull(item("missing")) }
    @Test fun deleteRemovesOnlyRequestedMovementAndIsIdempotent() = runBlocking {
        db.movementDao().insert(movement())
        db.movementDao().insert(movement("other"))
        saveRecurrence()
        val store = FinancialEntryRepository(db,java.time.Clock.systemUTC())
        store.deleteMovement("m")
        store.deleteMovement("m")
        assertNull(db.movementDao().findById("m"))
        assertNotNull(db.movementDao().findById("other"))
        assertNotNull(item())
    }
    @Test fun deletionUpdatesDetailCategoryDashboardAndHistoryFlows() = runBlocking {
        db.movementDao().insert(movement().copy(category = ExpenseCategory.HOUSING))
        val emissions = Channel<Boolean>(Channel.UNLIMITED)
        val reference = com.clarezafinanceira.app.domain.FinancialItemReference(com.clarezafinanceira.app.domain.ItemOrigin.MOVEMENT,"m",month)
        val history = HistoryRepository(repository,java.time.Clock.fixed(now,java.time.ZoneOffset.UTC))
        val job = launch {
            kotlinx.coroutines.flow.combine(details.observeItem(reference), details.observeCategory(month,category),
                repository.observeMonthlyAnalysis(month),history.observeYearHistory(2026)) { detail, category, analysis, months ->
                detail == null && category.items.isEmpty() && analysis.isEmpty && months.isEmpty()
            }.collect { emissions.send(it) }
        }
        try {
            assertFalse(withTimeout(10000) { emissions.receive() })
            FinancialEntryRepository(db,java.time.Clock.systemUTC()).deleteMovement("m")
            withTimeout(10000) { while (!emissions.receive()) { /* wait for all projections */ } }
        } finally { job.cancelAndJoin() }
    }
    @Test fun itemFlowReactsToExclusionWithoutFailure() = runBlocking {
        saveRecurrence()
        val emissions = Channel<com.clarezafinanceira.app.domain.AnalysisItem?>(Channel.UNLIMITED)
        val job = launch { details.observeItem(com.clarezafinanceira.app.domain.FinancialItemReference(
            com.clarezafinanceira.app.domain.ItemOrigin.RECURRENCE,"r",month)).collect { emissions.send(it) } }
        try {
            assertNotNull(withTimeout(10000) { emissions.receive() })
            db.recurrenceDao().addException(exception(excluded = true))
            assertNull(withTimeout(10000) { emissions.receive() })
        } finally { job.cancelAndJoin() }
    }
}