package com.clarezafinanceira.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.YearMonth

@Dao
abstract class RecurrenceDao {
    @Upsert abstract suspend fun putVersion(value: RecurrenceVersionEntity)
    @Upsert abstract suspend fun putException(value: RecurrenceExceptionEntity)
    @Query("DELETE FROM recurrence_exceptions WHERE recurrenceId = :id AND period = :period")
    abstract suspend fun removeException(id: String, period: YearMonth)
    @Insert protected abstract suspend fun insertRecurrence(value: RecurrenceEntity)
    @Insert protected abstract suspend fun insertVersion(value: RecurrenceVersionEntity)
    @Insert protected abstract suspend fun insertException(value: RecurrenceExceptionEntity)

    @Query("SELECT * FROM recurrences WHERE id = :id")
    abstract suspend fun findById(id: String): RecurrenceEntity?
    @Query("SELECT * FROM recurrences ORDER BY startPeriod, id")
    abstract fun observeAll(): Flow<List<RecurrenceEntity>>
    @Query("SELECT * FROM recurrence_versions WHERE recurrenceId = :recurrenceId ORDER BY validFrom")
    abstract suspend fun versions(recurrenceId: String): List<RecurrenceVersionEntity>
    @Query("SELECT * FROM recurrence_versions WHERE recurrenceId = :recurrenceId ORDER BY validFrom")
    abstract fun observeVersions(recurrenceId: String): Flow<List<RecurrenceVersionEntity>>
    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceId = :recurrenceId ORDER BY period")
    abstract fun observeExceptions(recurrenceId: String): Flow<List<RecurrenceExceptionEntity>>
    @Query("SELECT * FROM recurrence_exceptions WHERE recurrenceId = :recurrenceId AND period = :period")
    abstract suspend fun exceptionFor(recurrenceId: String, period: YearMonth): RecurrenceExceptionEntity?

    @Transaction
    open suspend fun create(recurrence: RecurrenceEntity, firstVersion: RecurrenceVersionEntity) {
        require(firstVersion.recurrenceId == recurrence.id)
        require(firstVersion.validFrom == recurrence.startPeriod)
        validateCategory(recurrence.type, firstVersion.category)
        insertRecurrence(recurrence)
        insertVersion(firstVersion)
    }

    // Inserts a new milestone; scoped edits preserve its ID through putVersion.
    @Transaction
    open suspend fun addVersion(version: RecurrenceVersionEntity) {
        val recurrence = requireNotNull(findById(version.recurrenceId))
        require(version.validFrom >= recurrence.startPeriod)
        validateCategory(recurrence.type, version.category)
        insertVersion(version)
    }

    @Transaction
    open suspend fun addException(exception: RecurrenceExceptionEntity) {
        val recurrence = requireNotNull(findById(exception.recurrenceId))
        val normalized = exception.normalized()
        require(recurrence.type != MovementType.INCOME || normalized.overrideCategory == null)
        insertException(normalized)
    }

    @Query("UPDATE recurrences SET endPeriod = :endPeriod, updatedAt = :updatedAt WHERE id = :id")
    protected abstract suspend fun setEndPeriod(id: String, endPeriod: YearMonth, updatedAt: Instant)

    /** endPeriod is inclusive. Stopping never removes versions or exceptions. */
    @Transaction
    open suspend fun stop(id: String, endPeriod: YearMonth, updatedAt: Instant) {
        val recurrence = requireNotNull(findById(id))
        require(endPeriod >= recurrence.startPeriod)
        setEndPeriod(id, endPeriod, updatedAt)
    }

    /** Explicit irreversible physical deletion, distinct from stopping. */
    @Query("DELETE FROM recurrences WHERE id = :id")
    abstract suspend fun deletePermanently(id: String): Int
}
