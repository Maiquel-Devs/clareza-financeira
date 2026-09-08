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
class HistoryRepositoryTest {
    private lateinit var db: FinancialDatabase
    private lateinit var repository: HistoryRepository
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
        repository = HistoryRepository(MonthlyAnalysisRepository(db), java.time.Clock.fixed(now, java.time.ZoneOffset.UTC))
    }
    @After fun close() { db.close() }

    private suspend fun year(year: Int = 2026) = withTimeout(10000) { repository.observeYearHistory(year).first() }
    @Test fun movementAppears() = runBlocking {
        db.movementDao().insert(movement())
        assertEquals(month, year().single().period)
    }
    @Test fun recurrenceAloneAppears() = runBlocking {
        saveRecurrence()
        assertEquals(20000L, year().single().forecastExpensesCents)
    }
    @Test fun emptyMonthDoesNotAppear() = runBlocking { assertTrue(year().isEmpty()) }
    @Test fun futureMonthDoesNotAppear() = runBlocking {
        db.movementDao().insert(movement(period = month.plusMonths(1)))
        saveRecurrence(recurrence(start = month.plusMonths(1)))
        assertTrue(year().isEmpty())
    }
    @Test fun currentMonthIsMarked() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(1)))
        assertEquals(listOf(true, false), year().map { it.isCurrentPeriod })
    }
    @Test fun monthsAreDescendingWithGaps() = runBlocking {
        for (m in listOf(1, 4, 2)) db.movementDao().insert(movement("m$m", period = YearMonth.of(2026,m)))
        assertEquals(listOf(4,2,1), year().map { it.period.monthValue })
    }
    @Test fun expenseWithoutIncomeHasNullLeftover() = runBlocking {
        db.movementDao().insert(movement())
        assertEquals(0L, year().single().incomeCents)
        assertNull(year().single().forecastLeftoverCents)
    }
    @Test fun excludedOnlyRecurrenceLeavesEmptyMonth() = runBlocking {
        saveRecurrence()
        db.recurrenceDao().addException(exception(excluded = true))
        assertTrue(year().isEmpty())
    }
    @Test fun recurrenceDoesNotAppearBeforeStart() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(1)))
        assertEquals(listOf(month, month.minusMonths(1)), year().map { it.period })
    }
    @Test fun recurrenceDoesNotAppearAfterEnd() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(2), end = month.minusMonths(1)))
        assertEquals(listOf(month.minusMonths(1),month.minusMonths(2)),year().map { it.period })
    }
    @Test fun permanentVersionAffectsCorrectCards() = runBlocking {
        val r = recurrence(start = month.minusMonths(2))
        saveRecurrence(r)
        db.recurrenceDao().addVersion(version(r,30000,month.minusMonths(1),"new"))
        assertEquals(listOf(30000L,30000L,20000L),year().map { it.forecastExpensesCents })
    }
    @Test fun exceptionAffectsOnlyItsMonth() = runBlocking {
        saveRecurrence(recurrence(start = month.minusMonths(2)))
        db.recurrenceDao().addException(exception(period = month.minusMonths(1)))
        assertEquals(listOf(20000L,5000L,20000L),year().map { it.forecastExpensesCents })
    }
    @Test fun flowReactsToMovementChanges() = runBlocking {
        val emissions = Channel<List<com.clarezafinanceira.app.domain.MonthlyHistoryItem>>(Channel.UNLIMITED)
        val job = launch { repository.observeYearHistory(2026).collect { emissions.send(it) } }
        try {
            assertTrue(withTimeout(10000) { emissions.receive() }.isEmpty())
            db.movementDao().insert(movement())
            assertEquals(10000L,withTimeout(10000) { emissions.receive() }.single().forecastExpensesCents)
            db.movementDao().deleteById("m")
            assertTrue(withTimeout(10000) { emissions.receive() }.isEmpty())
        } finally { job.cancelAndJoin() }
    }
    @Test fun anotherYearHasItsOwnMonths() = runBlocking {
        db.movementDao().insert(movement(period = YearMonth.of(2025,4)))
        assertEquals(YearMonth.of(2025,4),year(2025).single().period)
        assertTrue(year().isEmpty())
    }
    @Test fun emptyYearHasNoCards() = runBlocking { assertTrue(year(2025).isEmpty()) }
    @Test fun futureYearHasNoCards() = runBlocking {
        saveRecurrence()
        assertTrue(year(2027).isEmpty())
    }
}