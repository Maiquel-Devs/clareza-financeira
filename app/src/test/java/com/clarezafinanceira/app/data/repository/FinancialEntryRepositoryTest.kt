package com.clarezafinanceira.app.data.repository

import androidx.room.Room
import com.clarezafinanceira.app.data.local.FinancialDatabase
import com.clarezafinanceira.app.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class FinancialEntryRepositoryTest {
    private lateinit var db: FinancialDatabase
    private val instant = Instant.parse("2026-09-07T12:00:00Z")
    private val clock = Clock.fixed(instant, ZoneOffset.UTC)
    private val date = LocalDate.of(2026, 9, 7)
    private val month = YearMonth.from(date)
    private lateinit var repository: FinancialEntryRepository
    @Before fun before() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), FinancialDatabase::class.java).build()
        repository = FinancialEntryRepository(db, clock)
    }
    @After fun after() { db.close() }
    private fun input(type: MovementType = MovementType.INCOME, monthly: Boolean = false) =
        EntryInput(type, "  Entrada  ", 12345L,
            if (type == MovementType.EXPENSE) ExpenseCategory.FOOD else null, date, monthly)
    private suspend fun movements() = db.movementDao().observeBetween(date.minusYears(100), date.plusYears(100)).first()

    @Test fun createsOneOffIncomeOnly() = runBlocking {
        repository.save(input())
        val row = movements().single()
        assertEquals(12345L, row.amountCents); assertEquals("Entrada", row.name)
        assertNull(row.category); assertEquals(date, row.date)
        assertEquals(instant, row.createdAt); assertEquals(instant, row.updatedAt)
        assertTrue(db.recurrenceDao().observeAll().first().isEmpty())
    }
    @Test fun expenseHasCategory() = runBlocking {
        repository.save(input(MovementType.EXPENSE))
        assertEquals("FOOD", movements().single().category!!.name)
    }
    @Test fun expenseWithoutCategoryRejected() {
        assertThrows(IllegalArgumentException::class.java) { input(MovementType.EXPENSE).copy(category = null) }
    }
    @Test fun incomeNeverStoresCategory() = runBlocking {
        repository.save(input().copy(category = ExpenseCategory.FOOD))
        assertNull(movements().single().category)
    }
    @Test fun chosenDateIsStored() = runBlocking {
        repository.save(input().copy(date = date.minusDays(2)))
        assertEquals(date.minusDays(2), movements().single().date)
    }
    @Test fun editPreservesIdCreatedAtAndUpdatesTimestampAndAffectedMonths() = runBlocking {
        repository.save(input()); val original = movements().single()
        val later = FinancialEntryRepository(db, Clock.offset(clock, Duration.ofHours(1)))
        later.save(input().copy(name = "Editada", amountCents = 500L, date = date.plusMonths(1)), original.id)
        val edited = movements().single()
        assertEquals(original.id, edited.id); assertEquals(original.createdAt, edited.createdAt)
        assertEquals(instant.plusSeconds(3600), edited.updatedAt)
        val analysis = MonthlyAnalysisRepository(db)
        assertEquals(0L, analysis.getMonthlyAnalysis(month).registeredIncomeCents)
        assertEquals(500L, analysis.getMonthlyAnalysis(month.plusMonths(1)).registeredIncomeCents)
        assertTrue(analysis.getMonthlyAnalysis(month.minusMonths(1)).isEmpty)
    }
    @Test fun monthlyIncomeCreatesRuleAndFirstVersionOnly() = runBlocking {
        repository.save(input(monthly = true))
        val rule = db.recurrenceDao().observeAll().first().single()
        val version = db.recurrenceDao().versions(rule.id).single()
        assertEquals(month, rule.startPeriod); assertNull(rule.endPeriod)
        assertEquals(rule.startPeriod, version.validFrom); assertEquals(rule.id, version.recurrenceId)
        assertNull(version.category); assertEquals(12345L, version.amountCents)
        assertTrue(movements().isEmpty())
    }
    @Test fun monthlyExpenseUsesCategoryAndHabitualDayDoesNotGateForecast() = runBlocking {
        repository.save(input(MovementType.EXPENSE, true).copy(habitualDay = 31))
        val rule = db.recurrenceDao().observeAll().first().single()
        assertEquals("FOOD", db.recurrenceDao().versions(rule.id).single().category!!.name)
        assertEquals(12345L, MonthlyAnalysisRepository(db).getMonthlyAnalysis(month).forecastExpenseCents)
        assertTrue(movements().isEmpty())
    }
    @Test fun invalidDaysAndBlankNameRejected() {
        listOf(0, 32, -1).forEach { day ->
            assertThrows(IllegalArgumentException::class.java) { input(monthly = true).copy(habitualDay = day) }
        }
        assertThrows(IllegalArgumentException::class.java) { input().copy(name = "  ") }
    }
    @Test fun secondInsertFailureRollsBackNewRule() = runBlocking {
        val ids = ArrayDeque(listOf("rule-1", "version-collision", "rule-2", "version-collision"))
        val colliding = FinancialEntryRepository(db, clock) { ids.removeFirst() }
        colliding.save(input(monthly = true))
        try { colliding.save(input(monthly = true)); fail("Should violate primary key") }
        catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertNull(db.recurrenceDao().findById("rule-2"))
        assertEquals(1, db.recurrenceDao().observeAll().first().size)
    }
    @Test fun existingAnalysisFlowUpdatesAfterBothKindsOfSave() = runBlocking {
        val emissions = kotlinx.coroutines.channels.Channel<MonthlyAnalysis>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val collector = launch { MonthlyAnalysisRepository(db).observeMonthlyAnalysis(month).collect { emissions.send(it) } }
        try {
            withTimeout(10000) {
                assertTrue(emissions.receive().isEmpty)
                repository.save(input())
                assertEquals(12345L, emissions.receive().registeredIncomeCents)
                repository.save(input(MovementType.EXPENSE, true))
                assertEquals(12345L, emissions.receive().forecastExpenseCents)
            }
        } finally { collector.cancelAndJoin(); emissions.close() }
    }
}
