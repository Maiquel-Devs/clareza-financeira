package com.clarezafinanceira.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "movements", indices = [Index(value = ["date"])])
data class MovementEntity(
    @PrimaryKey val id: String,
    val type: MovementType,
    val name: String,
    val amountCents: Long,
    val category: ExpenseCategory?,
    val date: LocalDate,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank())
        require(name.isNotBlank())
        require(amountCents > 0)
        validateCategory(type, category)
    }
}
