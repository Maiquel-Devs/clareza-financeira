package com.clarezafinanceira.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.YearMonth

@Entity(
    tableName = "recurrence_exceptions",
    foreignKeys = [ForeignKey(
        entity = RecurrenceEntity::class, parentColumns = ["id"],
        childColumns = ["recurrenceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["recurrenceId", "period"], unique = true)],
)
data class RecurrenceExceptionEntity(
    @PrimaryKey val id: String,
    val recurrenceId: String,
    val period: YearMonth,
    val overrideName: String?,
    val overrideAmountCents: Long?,
    val overrideCategory: ExpenseCategory?,
    val overrideHabitualDay: Int?,
    val excluded: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank() && recurrenceId.isNotBlank())
        if (!excluded) {
            require(overrideName == null || overrideName.isNotBlank())
            require(overrideAmountCents == null || overrideAmountCents > 0)
            require(overrideHabitualDay == null || overrideHabitualDay in 1..31)
        }
    }

    // Exclusion is stored without overrides, so every future reader sees the same meaning.
    internal fun normalized(): RecurrenceExceptionEntity =
        if (excluded) copy(
            overrideName = null, overrideAmountCents = null,
            overrideCategory = null, overrideHabitualDay = null,
        ) else this
}
