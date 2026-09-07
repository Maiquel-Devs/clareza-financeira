package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.data.local.*
import com.clarezafinanceira.app.domain.EntryInput
import com.clarezafinanceira.app.domain.FinancialEntryStore
import java.time.Clock
import java.util.UUID

class FinancialEntryRepository(
    private val database: FinancialDatabase,
    private val clock: Clock,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : FinancialEntryStore {
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
