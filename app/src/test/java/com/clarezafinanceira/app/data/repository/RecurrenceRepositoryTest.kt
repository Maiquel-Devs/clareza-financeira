package com.clarezafinanceira.app.data.repository

import androidx.room.Room
import com.clarezafinanceira.app.data.local.FinancialDatabase
import com.clarezafinanceira.app.domain.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
class RecurrenceRepositoryTest {
    private lateinit var db: FinancialDatabase
    private lateinit var repo: FinancialEntryRepository
    private val clock = Clock.fixed(Instant.parse("2026-09-07T12:00:00Z"), ZoneOffset.UTC)
    private val sep = YearMonth.of(2026, 9)
    private lateinit var id: String
    @Before fun before() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), FinancialDatabase::class.java).build()
        repo = FinancialEntryRepository(db, clock)
        repo.save(EntryInput(MovementType.EXPENSE, "Base", 10000L, ExpenseCategory.HOUSING,
            sep.atDay(1), true, sep.withMonth(1), 5))
        id = db.recurrenceDao().observeAll().first().single().id
    }
    @After fun after() { db.close() }
    private suspend fun amount(month: Int): Long = MonthlyAnalysisRepository(db)
        .getMonthlyAnalysis(sep.withMonth(month)).forecastExpenseCents
    private suspend fun exception() = db.recurrenceDao().exceptionFor(id, sep)
    @Test fun monthlyAmountDoesNotChangeAdjacentPeriods() = runBlocking {
        val parent = db.recurrenceDao().findById(id)
        val versions = db.recurrenceDao().versions(id)
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        assertEquals(listOf(10000L, 15000L, 10000L), listOf(amount(8), amount(9), amount(10)))
        assertEquals(parent, db.recurrenceDao().findById(id)); assertEquals(versions, db.recurrenceDao().versions(id))
        assertNull(exception()!!.overrideName)
    }
    @Test fun permanentPreservesOldVersionAndPast() = runBlocking {
        val old = db.recurrenceDao().versions(id).single()
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        assertEquals(listOf(10000L, 15000L, 15000L), listOf(amount(8), amount(9), amount(10)))
        assertEquals(old, db.recurrenceDao().versions(id).first())
    }
    @Test fun versionAndExceptionCoexist() = runBlocking {
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        repo.updateRecurrenceForPeriod(id, sep.plusMonths(1), RecurrenceChanges(amountCents = 18000L))
        assertEquals(listOf(10000L,15000L,18000L,15000L), (8..11).map { amount(it) })
    }
    @Test fun laterMilestoneSurvivesHistoricalEdit() = runBlocking {
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        repo.updateRecurrenceFromPeriod(id, sep.withMonth(12), RecurrenceChanges(amountCents = 20000L))
        val future = db.recurrenceDao().versions(id).last()
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 16000L))
        assertEquals(20000L, amount(12)); assertEquals(future, db.recurrenceDao().versions(id).last())
        assertEquals(3, db.recurrenceDao().versions(id).size)
    }
    @Test fun stopKeepingPeriod() = runBlocking {
        repo.stopRecurrence(id, sep, true)
        assertEquals(listOf(10000L,10000L,0L), (8..10).map { amount(it) })
    }
    @Test fun stopRemovingPeriod() = runBlocking {
        repo.stopRecurrence(id, sep, false)
        assertEquals(listOf(10000L,0L,0L), (8..10).map { amount(it) })
    }
    @Test fun stopAtStartUsesExclusionAndValidInterval() = runBlocking {
        val jan = sep.withMonth(1)
        repo.stopRecurrence(id, jan, false)
        val parent = db.recurrenceDao().findById(id)!!
        assertEquals(parent.startPeriod, parent.endPeriod)
        assertTrue(db.recurrenceDao().exceptionFor(id, jan)!!.excluded)
        assertEquals(0L, amount(1)); assertEquals(0L, amount(2))
    }
    @Test fun repeatedStopDoesNotReopenRecurrence() = runBlocking {
        repo.stopRecurrence(id, sep, false); repo.stopRecurrence(id, sep.plusMonths(1), true)
        assertEquals(sep.minusMonths(1), db.recurrenceDao().findById(id)!!.endPeriod)
    }
    @Test fun deleteCascadesAndRemovesHistoricalParticipation() = runBlocking {
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        repo.deleteRecurrence(id)
        assertNull(db.recurrenceDao().findById(id)); assertTrue(db.recurrenceDao().versions(id).isEmpty())
        assertTrue(db.recurrenceDao().observeExceptions(id).first().isEmpty()); assertEquals(0L, amount(8))
    }
    @Test fun repeatedExceptionRetainsIdentityWithoutDuplicates() = runBlocking {
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        val old = exception()!!
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(amountCents = 18000L))
        assertEquals(old.id, exception()!!.id)
        assertEquals(1, db.recurrenceDao().observeExceptions(id).first().size)
        assertEquals(18000L, amount(9))
    }
    @Test fun sameMilestoneRetainsIdentityWithoutDuplicates() = runBlocking {
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 15000L))
        val old = db.recurrenceDao().versions(id).last()
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(amountCents = 18000L))
        assertEquals(old.id, db.recurrenceDao().versions(id).last().id)
        assertEquals(2, db.recurrenceDao().versions(id).size)
    }
    @Test fun nameOnlyOverride() = runBlocking {
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(name = "  Aluguel  "))
        assertEquals("Aluguel", repo.findRecurrence(id, sep)!!.name)
        assertNull(exception()!!.overrideAmountCents)
    }
    @Test fun categoryOverrideAndPermanentCategory() = runBlocking {
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(category = ExpenseCategory.FOOD))
        assertEquals(ExpenseCategory.FOOD, repo.findRecurrence(id, sep)!!.category)
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(category = ExpenseCategory.HEALTH))
        assertEquals(ExpenseCategory.HEALTH, repo.findRecurrence(id, sep.plusMonths(1))!!.category)
        assertEquals(ExpenseCategory.HOUSING, repo.findRecurrence(id, sep.minusMonths(1))!!.category)
    }
    @Test fun habitualDayOverrideAndPermanentDay() = runBlocking {
        repo.updateRecurrenceForPeriod(id, sep, RecurrenceChanges(habitualDay = 31))
        assertEquals(31, repo.findRecurrence(id, sep)!!.habitualDay)
        repo.updateRecurrenceFromPeriod(id, sep, RecurrenceChanges(habitualDay = 28))
        assertEquals(28, repo.findRecurrence(id, sep.plusMonths(1))!!.habitualDay)
        assertEquals(5, repo.findRecurrence(id, sep.minusMonths(1))!!.habitualDay)
    }
    @Test fun incomeRemainsUncategorizedAndLongCentsAreExact() = runBlocking {
        repo.save(EntryInput(MovementType.INCOME,"Renda",10000L,null,sep.atDay(1),true))
        val incomeId = db.recurrenceDao().observeAll().first().last().id
        val cents = 9007199254740993L
        repo.updateRecurrenceFromPeriod(incomeId,sep,RecurrenceChanges(amountCents = cents))
        assertEquals(cents,repo.findRecurrence(incomeId,sep)!!.amountCents)
        assertNull(repo.findRecurrence(incomeId,sep)!!.category)
        try { repo.updateRecurrenceForPeriod(incomeId,sep,RecurrenceChanges(category = ExpenseCategory.FOOD)); fail() }
        catch (_: IllegalArgumentException) { }
    }
    @Test fun noChangesDoNotWriteAndNoFutureMovementsAreCreated() = runBlocking {
        repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 10000L))
        repo.updateRecurrenceFromPeriod(id,sep,RecurrenceChanges())
        assertNull(exception()); assertEquals(1,db.recurrenceDao().versions(id).size)
        assertTrue(db.movementDao().observeBetween(sep.atDay(1),sep.plusYears(10).atEndOfMonth()).first().isEmpty())
    }
    @Test fun restoringBaseRemovesEmptyException() = runBlocking {
        repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 15000L))
        repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 10000L))
        assertNull(exception())
    }
    @Test fun permanentEditDoesNotPromoteUnchangedExceptionFields() = runBlocking {
        repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 18000L, name = "Especial"))
        repo.updateRecurrenceFromPeriod(id,sep,RecurrenceChanges(name = "Nova base"))
        assertEquals(18000L, amount(9)); assertEquals(10000L, amount(10))
        assertEquals("Nova base", repo.findRecurrence(id,sep)!!.name)
        assertNull(exception()!!.overrideName)
    }
    @Test fun permanentWriteRollsBackIfExceptionCleanupFails() = runBlocking {
        repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 18000L))
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_cleanup BEFORE DELETE ON recurrence_exceptions BEGIN SELECT RAISE(ABORT, 'test'); END")
        try { repo.updateRecurrenceFromPeriod(id,sep,RecurrenceChanges(amountCents = 15000L)); fail() }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertEquals(1,db.recurrenceDao().versions(id).size); assertEquals(18000L,amount(9))
    }
    @Test fun stopAtStartRollsBackIfExceptionInsertFails() = runBlocking {
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_exception BEFORE INSERT ON recurrence_exceptions BEGIN SELECT RAISE(ABORT, 'test'); END")
        try { repo.stopRecurrence(id,sep.withMonth(1),false); fail() }
        catch (_: android.database.sqlite.SQLiteException) { }
        assertNull(db.recurrenceDao().findById(id)!!.endPeriod)
    }
    @Test fun existingAnalysisFlowReactsToEveryOperation() = runBlocking {
        val channel = Channel<MonthlyAnalysis>(Channel.UNLIMITED)
        val job = launch { MonthlyAnalysisRepository(db).observeMonthlyAnalysis(sep).collect { channel.send(it) } }
        try { withTimeout(10000) {
            assertEquals(10000L,channel.receive().forecastExpenseCents)
            repo.updateRecurrenceForPeriod(id,sep,RecurrenceChanges(amountCents = 15000L))
            assertEquals(15000L,channel.receive().forecastExpenseCents)
            repo.updateRecurrenceFromPeriod(id,sep,RecurrenceChanges(amountCents = 18000L))
            assertEquals(18000L,channel.receive().forecastExpenseCents)
            repo.stopRecurrence(id,sep,false)
            assertEquals(0L,channel.receive().forecastExpenseCents)
            repo.save(EntryInput(MovementType.EXPENSE,"Outra",100L,ExpenseCategory.FOOD,sep.atDay(1),true))
            assertEquals(100L,channel.receive().forecastExpenseCents)
            val other = db.recurrenceDao().observeAll().first().first { it.id != id }.id
            repo.deleteRecurrence(other)
            assertEquals(0L,channel.receive().forecastExpenseCents)
        } } finally { job.cancelAndJoin(); channel.close() }
    }
}
