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
class MonthlyAnalysisRepositoryTest {
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

    @Test fun emptyMonth() = runBlocking {
        assertEquals(MonthlyAnalysisEngine().analyze(month), repository.getMonthlyAnalysis(month))
    }

    @Test fun savedMovementAppearsWithSourceDetails() = runBlocking {
        val m = movement()
        db.movementDao().insert(m)
        assertEquals(m.id, repository.getMonthlyAnalysis(month).expenseItems.single().sourceId)
    }

    @Test fun movementsOutsideMonthAreNotLoaded() = runBlocking {
        db.movementDao().insert(movement(period = month.minusMonths(1)))
        db.movementDao().insert(movement(id = "next", period = month.plusMonths(1)))
        assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
        assertTrue(db.monthlyAnalysisDao().snapshot(month).movements.isEmpty())
    }

    @Test fun punctualIncomeCounts() = runBlocking {
        db.movementDao().insert(movement(type = MovementType.INCOME))
        assertEquals(10000L, repository.getMonthlyAnalysis(month).registeredIncomeCents)
    }

    @Test fun punctualExpenseCountsAsRegistered() = runBlocking {
        db.movementDao().insert(movement())
        assertEquals(10000L, repository.getMonthlyAnalysis(month).registeredExpenseCents)
    }

    @Test fun activeRecurrenceAppears() = runBlocking {
        saveRecurrence()
        assertEquals("r", repository.getMonthlyAnalysis(month).expenseItems.single().sourceId)
    }

    @Test fun beforeStartDoesNotAppearOrLoadChildren() = runBlocking {
        saveRecurrence(recurrence(start = month.plusMonths(1)))
        assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
        val snapshot = db.monthlyAnalysisDao().snapshot(month)
        assertTrue(snapshot.recurrences.isEmpty())
        assertTrue(snapshot.versions.isEmpty())
    }

    @Test fun afterEndDoesNotAppear() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(2), end = month.minusMonths(1)))
        assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
    }

    @Test fun latestEffectiveVersionSelectedAndFutureNotLoaded() = runBlocking {
        val r = recurrence(start = month.minusMonths(2))
        saveRecurrence(r)
        db.recurrenceDao().addVersion(version(r, 30000, month.minusMonths(1), "effective"))
        db.recurrenceDao().addVersion(version(r, 40000, month.plusMonths(1), "future"))
        assertEquals("effective", repository.getMonthlyAnalysis(month).expenseItems.single().versionId)
        assertEquals(listOf("v", "effective"), db.monthlyAnalysisDao().snapshot(month).versions.map { it.id })
    }

    @Test fun permanentChangeAppliesStartingAtItsPeriod() = runBlocking {
        val r = recurrence()
        saveRecurrence(r)
        db.recurrenceDao().addVersion(version(r, 30000, month.plusMonths(1), "next"))
        assertEquals(30000L, repository.getMonthlyAnalysis(month.plusMonths(1)).forecastExpenseCents)
    }

    @Test fun permanentChangePreservesEarlierHistory() = runBlocking {
        val r = recurrence()
        saveRecurrence(r)
        val before = repository.getMonthlyAnalysis(month)
        db.recurrenceDao().addVersion(version(r, 30000, month.plusMonths(1), "next"))
        assertEquals(before, repository.getMonthlyAnalysis(month))
    }

    @Test fun exceptionChangesOnlySelectedMonth() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception())
        assertEquals(5000L, repository.getMonthlyAnalysis(month).forecastExpenseCents)
        assertEquals(20000L, repository.getMonthlyAnalysis(month.plusMonths(1)).forecastExpenseCents)
        assertTrue(db.monthlyAnalysisDao().snapshot(month.plusMonths(1)).exceptions.isEmpty())
    }

    @Test fun excludedRemovesOnlySelectedMonth() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception(excluded = true))
        assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
        assertEquals(20000L, repository.getMonthlyAnalysis(month.plusMonths(1)).forecastExpenseCents)
    }

    @Test fun recurringIncomeCountsInConsideredIncome() = runBlocking {
        saveRecurrence(recurrence(type = MovementType.INCOME))
        val analysis = repository.getMonthlyAnalysis(month)
        assertEquals(20000L, analysis.consideredIncomeCents)
        assertEquals(0L, analysis.registeredIncomeCents)
        assertEquals(20000L, analysis.forecastRemainingCents)
    }

    @Test fun recurringExpenseOnlyCountsInForecast() = runBlocking {
        saveRecurrence()
        val analysis = repository.getMonthlyAnalysis(month)
        assertEquals(20000L, analysis.forecastExpenseCents)
        assertEquals(0L, analysis.registeredExpenseCents)
    }

    @Test fun flowEmitsInitialThenInsertedAndUpdatedMovement() = runBlocking {
        observe { emissions ->
            assertTrue(emissions.next().isEmpty)
            db.movementDao().insert(movement())
            assertEquals(10000L, emissions.next().forecastExpenseCents)
            db.movementDao().update(movement(amount = 15000))
            assertEquals(15000L, emissions.next().forecastExpenseCents)
        }
    }

    @Test fun deletingMovementUpdatesAnalysisAndFlow() = runBlocking {
        db.movementDao().insert(movement())
        observe { emissions ->
            assertEquals(10000L, emissions.next().registeredExpenseCents)
            db.movementDao().deleteById("m")
            assertTrue(emissions.next().isEmpty)
            assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
        }
    }

    @Test fun stoppingPreservesHistoryAndIncludesEndMonth() = runBlocking {
        saveRecurrence()
        val historical = repository.getMonthlyAnalysis(month)
        db.recurrenceDao().stop("r", month, now)
        assertEquals(historical, repository.getMonthlyAnalysis(month))
        assertTrue(repository.getMonthlyAnalysis(month.plusMonths(1)).isEmpty)
    }

    @Test fun physicalDeletionRemovesHistoricalContributionAndChildren() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception())
        db.recurrenceDao().deletePermanently("r")
        assertTrue(repository.getMonthlyAnalysis(month).isEmpty)
        assertTrue(db.recurrenceDao().versions("r").isEmpty())
        assertNull(db.recurrenceDao().exceptionFor("r", month))
    }

    @Test fun combinedFactsMatchDirectEngineAndExpectedTotals() = runBlocking {
        val income = movement("income", MovementType.INCOME, 100000)
        val expense = movement(amount = 15000)
        val r = recurrence()
        val v = version(r)
        val e = exception()
        db.movementDao().insert(income)
        db.movementDao().insert(expense)
        db.recurrenceDao().create(r, v)
        db.recurrenceDao().addException(e)
        val actual = repository.getMonthlyAnalysis(month)
        assertEquals(MonthlyAnalysisEngine().analyze(month, listOf(income.toDomain(), expense.toDomain()),
            listOf(r.toDomain()), listOf(v.toDomain()), listOf(e.toDomain())), actual)
        assertEquals(15000L, actual.registeredExpenseCents)
        assertEquals(20000L, actual.forecastExpenseCents)
        assertEquals(80000L, actual.forecastRemainingCents)
        assertEquals(BigDecimal("20.0000"), actual.expensePercentageOfIncome)
    }

    @Test fun flowReactsToCreationVersionExceptionAndPhysicalDeletion() = runBlocking {
        val r = recurrence(start = month.minusMonths(1))
        observe { emissions ->
            assertTrue(emissions.next().isEmpty)
            saveRecurrence(r)
            assertEquals(20000L, emissions.next().forecastExpenseCents)
            db.recurrenceDao().addVersion(version(r, 30000, month, "new"))
            assertEquals(30000L, emissions.next().forecastExpenseCents)
            db.recurrenceDao().addException(exception())
            assertEquals(5000L, emissions.next().forecastExpenseCents)
            db.recurrenceDao().deletePermanently("r")
            assertTrue(emissions.next().isEmpty)
        }
    }

    @Test fun flowReactsToStopping() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(1)))
        observe { emissions ->
            assertEquals(20000L, emissions.next().forecastExpenseCents)
            db.recurrenceDao().stop("r", month.minusMonths(1), now)
            assertTrue(emissions.next().isEmpty)
        }
    }

    @Test fun unrelatedChangesDoNotEmitDuplicateAnalysis() = runBlocking {
        observe { emissions ->
            assertTrue(emissions.next().isEmpty)
            db.movementDao().insert(movement("other", period = month.plusMonths(1)))
            // A subsequent relevant write acts as a barrier without relying on sleeps.
            db.movementDao().insert(movement())
            assertEquals(10000L, emissions.next().forecastExpenseCents)
        }
    }

    @Test fun missingVersionErrorPropagatesFromGetAndFlow() = runBlocking {
        // Deliberately corrupt the input using SQL, bypassing the approved transactional DAO.
        db.openHelper.writableDatabase.execSQL("""
            INSERT INTO recurrences (id, type, startPeriod, endPeriod, createdAt, updatedAt)
            VALUES ('broken', 'EXPENSE', 24320, NULL, '2026-09-01T00:00:00Z', '2026-09-01T00:00:00Z')
        """)
        var getFailed = false
        try { repository.getMonthlyAnalysis(month) } catch (_: IllegalArgumentException) { getFailed = true }
        assertTrue(getFailed)
        var flowFailed = false
        try { withTimeout(10000) { repository.observeMonthlyAnalysis(month).first() } }
        catch (_: IllegalArgumentException) { flowFailed = true }
        assertTrue(flowFailed)
    }

    @Test fun exactMonthBoundariesIncludingLeapDay() = runBlocking {
        val february = YearMonth.of(2024, 2)
        db.movementDao().insert(movement("first", period = february).copy(date = february.atDay(1)))
        db.movementDao().insert(movement("last", period = february).copy(date = february.atEndOfMonth()))
        db.movementDao().insert(movement("before", period = february).copy(date = february.atDay(1).minusDays(1)))
        db.movementDao().insert(movement("after", period = february).copy(date = february.atEndOfMonth().plusDays(1)))
        assertEquals(setOf("first", "last"), repository.getMonthlyAnalysis(february).expenseItems.map { it.sourceId }.toSet())
    }

    private suspend fun observe(block: suspend (Channel<MonthlyAnalysis>) -> Unit) = coroutineScope {
        val emissions = Channel<MonthlyAnalysis>(Channel.UNLIMITED)
        val job = launch { repository.observeMonthlyAnalysis(month).collect { emissions.send(it) } }
        try { block(emissions) } finally { job.cancelAndJoin(); emissions.close() }
    }

    private suspend fun Channel<MonthlyAnalysis>.next(): MonthlyAnalysis = withTimeout(10000) { receive() }
}
