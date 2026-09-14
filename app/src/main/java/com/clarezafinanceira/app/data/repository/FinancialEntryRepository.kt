package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.data.local.*
import com.clarezafinanceira.app.domain.EntryInput
import com.clarezafinanceira.app.domain.FinancialEntryStore
import com.clarezafinanceira.app.domain.RecurrenceStore
import com.clarezafinanceira.app.domain.RecurrenceChanges
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import androidx.room.withTransaction
import java.time.YearMonth
import java.time.Clock
import java.util.UUID

class FinancialEntryRepository(
    private val database: FinancialDatabase,
    private val clock: Clock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : FinancialEntryStore, RecurrenceStore, com.clarezafinanceira.app.domain.MovementDeletionStore {
    override suspend fun deleteMovement(id: String) {
        database.movementDao().deleteById(id)
    }

    override suspend fun findRecurrence(id: String, period: YearMonth) = database.withTransaction {
        val dao = database.recurrenceDao()
        val parent = dao.findById(id) ?: return@withTransaction null
        val result = MonthlyAnalysisEngine().analyze(period,
            recurrences = listOf(parent.toDomain()),
            versions = dao.versions(id).map { it.toDomain() },
            exceptions = listOfNotNull(dao.exceptionFor(id, period)?.toDomain()))
        (result.incomeSources + result.expenseItems).singleOrNull()
    }

    override suspend fun updateRecurrenceForPeriod(id: String, period: YearMonth, changes: RecurrenceChanges) {
        updateRecurrence(id, period, changes, permanent = false)
    }

    override suspend fun updateRecurrenceFromPeriod(id: String, period: YearMonth, changes: RecurrenceChanges) {
        updateRecurrence(id, period, changes, permanent = true)
    }

    private suspend fun updateRecurrence(id: String, period: YearMonth, changes: RecurrenceChanges,
        permanent: Boolean) = database.withTransaction {
        val dao = database.recurrenceDao()
        val parent = requireNotNull(dao.findById(id))
        require(period >= parent.startPeriod && (parent.endPeriod == null || period <= parent.endPeriod))
        require(parent.type != MovementType.INCOME || changes.category == null)
        val effective = requireNotNull(findRecurrence(id, period))
        // Also guard callers outside the form against redundant writes.
        val patch = RecurrenceChanges(
            changes.name?.trim()?.takeIf { it != effective.name },
            changes.amountCents?.takeIf { it != effective.amountCents },
            changes.category?.takeIf { it != effective.category },
            changes.habitualDay?.takeIf { it != effective.habitualDay })
        if (patch.isEmpty) return@withTransaction
        val base = dao.versions(id).last { it.validFrom <= period }
        val old = dao.exceptionFor(id, period)
        val now = clock.instant()
        if (permanent) {
            // Start from the version active in this period, not its monthly overrides.
            // Keep later milestones: a historical edit must not replace future decisions.
            dao.putVersion(base.copy(id = if (base.validFrom == period) base.id else newId(),
                validFrom = period, name = patch.name ?: base.name,
                amountCents = patch.amountCents ?: base.amountCents,
                category = patch.category?.let { ExpenseCategory.valueOf(it.name) } ?: base.category,
                habitualDay = patch.habitualDay ?: base.habitualDay,
                createdAt = if (base.validFrom == period) base.createdAt else now))
            // Edited fields now come from the milestone; unrelated monthly overrides survive.
            if (old != null) persistException(old.copy(
                overrideName = if (patch.name != null) null else old.overrideName,
                overrideAmountCents = if (patch.amountCents != null) null else old.overrideAmountCents,
                overrideCategory = if (patch.category != null) null else old.overrideCategory,
                overrideHabitualDay = if (patch.habitualDay != null) null else old.overrideHabitualDay,
                updatedAt = now))
        } else {
            persistException(RecurrenceExceptionEntity(old?.id ?: newId(), id, period,
                (patch.name ?: old?.overrideName)?.takeIf { it != base.name },
                (patch.amountCents ?: old?.overrideAmountCents)?.takeIf { it != base.amountCents },
                (patch.category?.let { ExpenseCategory.valueOf(it.name) } ?: old?.overrideCategory)
                    ?.takeIf { it != base.category },
                (patch.habitualDay ?: old?.overrideHabitualDay)?.takeIf { it != base.habitualDay },
                false, old?.createdAt ?: now, now))
        }
    }

    private suspend fun persistException(value: RecurrenceExceptionEntity) {
        val dao = database.recurrenceDao()
        if (!value.excluded && value.overrideName == null && value.overrideAmountCents == null &&
            value.overrideCategory == null && value.overrideHabitualDay == null)
            dao.removeException(value.recurrenceId, value.period)
        else dao.putException(value)
    }

    override suspend fun stopRecurrence(id: String, period: YearMonth, keepPeriod: Boolean) = database.withTransaction {
        val dao = database.recurrenceDao()
        val parent = requireNotNull(dao.findById(id))
        require(period >= parent.startPeriod)
        val end = if (keepPeriod) period else maxOf(parent.startPeriod, period.minusMonths(1))
        // Repeating a stop must never extend an already closed lifecycle.
        dao.stop(id, minOf(parent.endPeriod ?: end, end), clock.instant())
        if (!keepPeriod && period == parent.startPeriod) {
            // The inclusive end cannot precede the start; exclude the first occurrence
            // explicitly so stopping here keeps a valid interval without including this month.
            val old = dao.exceptionFor(id, period)
            val now = clock.instant()
            dao.putException(RecurrenceExceptionEntity(old?.id ?: newId(), id, period,
                null, null, null, null, true, old?.createdAt ?: now, now))
        }
    }

    override suspend fun deleteRecurrence(id: String) {
        database.withTransaction { database.recurrenceDao().deletePermanently(id) }
    }
    override suspend fun findMovement(id: String) = database.movementDao().findById(id)?.toDomain()

    override suspend fun save(input: EntryInput, movementId: String?) {
        val now = clock.instant()
        val type = MovementType.valueOf(input.type.name)
        val category = if (type == MovementType.INCOME) null
            else ExpenseCategory.valueOf(requireNotNull(input.category).name)
        val name = input.name.trim()
        require(name.isNotEmpty() && input.amountCents > 0)
        if (movementId != null) {
            require(!input.monthly)
            val previous = requireNotNull(database.movementDao().findById(movementId))
            require(previous.type == type)
            check(database.movementDao().update(previous.copy(
                name = name, amountCents = input.amountCents, category = category,
                date = input.date, updatedAt = now,
            )) == 1)
        } else if (input.monthly) {
            val id = newId()
            database.recurrenceDao().create(
                RecurrenceEntity(id, type, input.startPeriod, null, now, now),
                RecurrenceVersionEntity(newId(), id, input.startPeriod, name,
                    input.amountCents, category, input.habitualDay, now),
            )
        } else {
            database.movementDao().insert(MovementEntity(newId(), type, name,
                input.amountCents, category, input.date, now, now))
        }
    }
}
