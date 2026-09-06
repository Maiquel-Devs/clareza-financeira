package com.clarezafinanceira.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import java.time.LocalDate
import java.time.YearMonth

data class MonthlyAnalysisSnapshot(
    val movements: List<MovementEntity>,
    val recurrences: List<RecurrenceEntity>,
    val versions: List<RecurrenceVersionEntity>,
    val exceptions: List<RecurrenceExceptionEntity>,
)

/** Candidate filtering only; effective versions and financial rules belong to the engine. */
@Dao
abstract class MonthlyAnalysisDao {
    @Query("SELECT * FROM movements WHERE date BETWEEN :from AND :through ORDER BY date, id")
    protected abstract suspend fun movements(from: LocalDate, through: LocalDate): List<MovementEntity>

    @Query("""
        SELECT * FROM recurrences
        WHERE startPeriod <= :period AND (endPeriod IS NULL OR endPeriod >= :period)
        ORDER BY id
    """)
    protected abstract suspend fun recurrences(period: YearMonth): List<RecurrenceEntity>

    @Query("""
        SELECT v.* FROM recurrence_versions v
        INNER JOIN recurrences r ON r.id = v.recurrenceId
        WHERE r.startPeriod <= :period AND (r.endPeriod IS NULL OR r.endPeriod >= :period)
          AND v.validFrom <= :period
        ORDER BY v.recurrenceId, v.validFrom
    """)
    protected abstract suspend fun versions(period: YearMonth): List<RecurrenceVersionEntity>

    @Query("""
        SELECT e.* FROM recurrence_exceptions e
        INNER JOIN recurrences r ON r.id = e.recurrenceId
        WHERE r.startPeriod <= :period AND (r.endPeriod IS NULL OR r.endPeriod >= :period)
          AND e.period = :period
        ORDER BY e.recurrenceId
    """)
    protected abstract suspend fun exceptions(period: YearMonth): List<RecurrenceExceptionEntity>

    // The four reads must share one snapshot, including during concurrent writes.
    @Transaction
    open suspend fun snapshot(period: YearMonth): MonthlyAnalysisSnapshot = MonthlyAnalysisSnapshot(
        movements = movements(period.atDay(1), period.atEndOfMonth()),
        recurrences = recurrences(period),
        versions = versions(period),
        exceptions = exceptions(period),
    )
}
