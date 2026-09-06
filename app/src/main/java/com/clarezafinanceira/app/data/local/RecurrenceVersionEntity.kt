package com.clarezafinanceira.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.YearMonth

@Entity(
    tableName = "recurrence_versions",
    foreignKeys = [ForeignKey(
        entity = RecurrenceEntity::class, parentColumns = ["id"],
        childColumns = ["recurrenceId"], onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["recurrenceId", "validFrom"], unique = true)],
)
data class RecurrenceVersionEntity(
    @PrimaryKey val id: String,
    val recurrenceId: String,
    val validFrom: YearMonth,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val habitualDay: Int,
    val createdAt: Instant,
) {
    init {
        require(id.isNotBlank() && recurrenceId.isNotBlank())
        require(name.isNotBlank())
        require(amountCents > 0)
        require(habitualDay in 1..31)
    }
}
