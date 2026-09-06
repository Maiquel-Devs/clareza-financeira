package com.clarezafinanceira.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.YearMonth

@Entity(tableName = "recurrences")
data class RecurrenceEntity(
    @PrimaryKey val id: String,
    val type: MovementType,
    val startPeriod: YearMonth,
    val endPeriod: YearMonth?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank())
        require(endPeriod == null || endPeriod >= startPeriod)
    }
}
