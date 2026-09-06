package com.clarezafinanceira.app.data.local

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class FinancialDatabaseTest {
    private lateinit var db: FinancialDatabase
    private val now = Instant.parse("2026-09-06T12:00:00.123456789Z")
    private val period = YearMonth.of(2026, 9)
    private fun id() = UUID.randomUUID().toString()
    private fun movement() = MovementEntity(id(), MovementType.EXPENSE, "Food", 12345,
        ExpenseCategory.FOOD, LocalDate.of(2026, 9, 6), now, now)
    private fun recurrence(type: MovementType = MovementType.EXPENSE) =
        RecurrenceEntity(id(), type, period, null, now, now)
    private fun version(r: RecurrenceEntity) = RecurrenceVersionEntity(
        id(), r.id, r.startPeriod, "Rent", 100000,
        if (r.type == MovementType.EXPENSE) ExpenseCategory.HOUSING else null, 31, now)
    private fun exception(r: RecurrenceEntity) = RecurrenceExceptionEntity(
        id(), r.id, period, "Different rent", 95000, ExpenseCategory.OTHER, 15, false, now, now)

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(), FinancialDatabase::class.java,
        ).build()
    }
    @After fun close() { db.close() }

    @Test fun databaseOpensAndMovementRoundTripsThroughFlow() = runBlocking {
        val movement = movement()
        db.movementDao().insert(movement)
        assertEquals(movement, db.movementDao().findById(movement.id))
        assertEquals(listOf(movement), db.movementDao()
            .observeBetween(movement.date, movement.date).first())
        assertTrue(db.movementDao().observeBetween(
            movement.date.plusDays(1), movement.date.plusDays(2)).first().isEmpty())
        val updated = movement.copy(amountCents = Long.MAX_VALUE)
        assertEquals(1, db.movementDao().update(updated))
        assertEquals(updated, db.movementDao().findById(movement.id))
        assertEquals(1, db.movementDao().deleteById(movement.id))
        assertNull(db.movementDao().findById(movement.id))
    }

    @Test fun recurrenceAndVersionsKeepHistory() = runBlocking {
        val recurrence = recurrence()
        val initial = version(recurrence)
        db.recurrenceDao().create(recurrence, initial)
        val next = initial.copy(id = id(), validFrom = period.plusMonths(1), amountCents = 110000)
        db.recurrenceDao().addVersion(next)
        assertEquals(recurrence, db.recurrenceDao().findById(recurrence.id))
        assertEquals(listOf(initial, next), db.recurrenceDao().observeVersions(recurrence.id).first())
        assertEquals(listOf(recurrence), db.recurrenceDao().observeAll().first())
    }

    @Test fun monthlyExceptionRoundTrips() = runBlocking {
        val r = recurrence()
        db.recurrenceDao().create(r, version(r))
        val exception = exception(r)
        db.recurrenceDao().addException(exception)
        assertEquals(exception, db.recurrenceDao().exceptionFor(r.id, period))
        assertEquals(listOf(exception), db.recurrenceDao().observeExceptions(r.id).first())
    }

    @Test fun excludedOverridesAreDiscardedEvenIfInvalid() = runBlocking {
        val r = recurrence(MovementType.INCOME)
        db.recurrenceDao().create(r, version(r))
        val excluded = exception(r).copy(
            excluded = true, overrideName = "", overrideAmountCents = -1, overrideHabitualDay = 99)
        db.recurrenceDao().addException(excluded)
        assertEquals(excluded.normalized(), db.recurrenceDao().exceptionFor(r.id, period))
    }

    @Test fun uniqueVersionPeriodRejectsDuplicateWithoutReplacingHistory() = runBlocking {
        val r = recurrence()
        val first = version(r)
        db.recurrenceDao().create(r, first)
        expectFailure { db.recurrenceDao().addVersion(first.copy(id = id(), amountCents = 1)) }
        assertEquals(listOf(first), db.recurrenceDao().versions(r.id))
    }

    @Test fun uniqueExceptionPeriodRejectsDuplicate() = runBlocking {
        val r = recurrence()
        db.recurrenceDao().create(r, version(r))
        val exception = exception(r)
        db.recurrenceDao().addException(exception)
        expectFailure { db.recurrenceDao().addException(exception.copy(id = id())) }
        assertEquals(exception, db.recurrenceDao().exceptionFor(r.id, period))
    }

    @Test fun creationIsAtomicWhenVersionPrimaryKeyConflicts() = runBlocking {
        val first = recurrence()
        val firstVersion = version(first)
        db.recurrenceDao().create(first, firstVersion)
        val second = recurrence()
        expectFailure { db.recurrenceDao().create(second, version(second).copy(id = firstVersion.id)) }
        assertNull(db.recurrenceDao().findById(second.id))
    }

    @Test fun invalidFirstOrEarlierVersionIsRejected() = runBlocking {
        val r = recurrence()
        expectFailure { db.recurrenceDao().create(r, version(r).copy(validFrom = period.plusMonths(1))) }
        assertNull(db.recurrenceDao().findById(r.id))
        db.recurrenceDao().create(r, version(r))
        expectFailure { db.recurrenceDao().addVersion(version(r).copy(validFrom = period.minusMonths(1))) }
        expectFailure { db.recurrenceDao().addVersion(version(r).copy(recurrenceId = id())) }
    }

    @Test fun categoriesFollowRecurrenceType() = runBlocking {
        val r = recurrence(MovementType.INCOME)
        expectFailure { db.recurrenceDao().create(r, version(r).copy(category = ExpenseCategory.OTHER)) }
        db.recurrenceDao().create(r, version(r))
        expectFailure { db.recurrenceDao().addException(exception(r)) }
        expectFailure { db.recurrenceDao().addVersion(
            version(r).copy(validFrom = period.plusMonths(1), category = ExpenseCategory.FOOD)) }
        val expense = recurrence()
        expectFailure { db.recurrenceDao().create(expense, version(expense).copy(category = null)) }
    }

    @Test fun stopKeepsHistoryAndPhysicalDeletionCascades() = runBlocking {
        val r = recurrence()
        val version = version(r)
        db.recurrenceDao().create(r, version)
        db.recurrenceDao().addException(exception(r))
        expectFailure { db.recurrenceDao().stop(r.id, period.minusMonths(1), now) }
        db.recurrenceDao().stop(r.id, period.plusMonths(1), now.plusSeconds(1))
        assertEquals(r.copy(endPeriod = period.plusMonths(1), updatedAt = now.plusSeconds(1)),
            db.recurrenceDao().findById(r.id))
        assertEquals(listOf(version), db.recurrenceDao().versions(r.id))
        assertNotNull(db.recurrenceDao().exceptionFor(r.id, period))
        db.recurrenceDao().deletePermanently(r.id)
        assertTrue(db.recurrenceDao().versions(r.id).isEmpty())
        assertNull(db.recurrenceDao().exceptionFor(r.id, period))
    }

    @Test fun foreignKeysRejectOrphansInSqlite() {
        val sql = db.openHelper.writableDatabase
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            sql.execSQL("""INSERT INTO recurrence_versions
                (id, recurrenceId, validFrom, name, amountCents, category, habitualDay, createdAt)
                VALUES ('v', 'missing', 24320, 'Rent', 100, 'HOUSING', 1, '2026-09-06T00:00:00Z')""")
        }
        assertThrows(android.database.sqlite.SQLiteConstraintException::class.java) {
            sql.execSQL("""INSERT INTO recurrence_exceptions
                (id, recurrenceId, period, excluded, createdAt, updatedAt)
                VALUES ('e', 'missing', 24320, 1, '2026-09-06T00:00:00Z', '2026-09-06T00:00:00Z')""")
        }
    }

    @Test fun entityValidationRejectsInvalidValues() {
        assertThrows(IllegalArgumentException::class.java) { movement().copy(amountCents = 0) }
        assertThrows(IllegalArgumentException::class.java) { movement().copy(amountCents = -1) }
        assertThrows(IllegalArgumentException::class.java) { movement().copy(category = null) }
        assertThrows(IllegalArgumentException::class.java) { movement().copy(type = MovementType.INCOME) }
        val r = recurrence()
        assertThrows(IllegalArgumentException::class.java) { r.copy(endPeriod = period.minusMonths(1)) }
        assertThrows(IllegalArgumentException::class.java) { version(r).copy(name = " ") }
        assertThrows(IllegalArgumentException::class.java) { version(r).copy(amountCents = 0) }
        listOf(0, 32).forEach { day ->
            assertThrows(IllegalArgumentException::class.java) { version(r).copy(habitualDay = day) }
            assertThrows(IllegalArgumentException::class.java) { exception(r).copy(overrideHabitualDay = day) }
        }
        assertThrows(IllegalArgumentException::class.java) { exception(r).copy(overrideName = " ") }
        assertThrows(IllegalArgumentException::class.java) { exception(r).copy(overrideAmountCents = 0) }
    }

    @Test fun fileDatabaseSurvivesReopening() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "persistence-test.db"
        context.deleteDatabase(name)
        val movement = movement()
        try {
            val first = Room.databaseBuilder(context, FinancialDatabase::class.java, name).build()
            try { first.movementDao().insert(movement) } finally { first.close() }
            val reopened = Room.databaseBuilder(context, FinancialDatabase::class.java, name).build()
            try { assertEquals(movement, reopened.movementDao().findById(movement.id)) }
            finally { reopened.close() }
        } finally { context.deleteDatabase(name) }
    }

    private suspend fun expectFailure(block: suspend () -> Unit) {
        try { block() } catch (_: IllegalArgumentException) { return }
        catch (_: android.database.sqlite.SQLiteConstraintException) { return }
        fail("Expected validation or SQLite constraint failure")
    }
}
