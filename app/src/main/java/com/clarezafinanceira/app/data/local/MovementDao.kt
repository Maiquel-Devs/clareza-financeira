package com.clarezafinanceira.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MovementDao {
    @Insert suspend fun insert(movement: MovementEntity)
    @Update suspend fun update(movement: MovementEntity): Int
    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun findById(id: String): MovementEntity?
    @Query("SELECT * FROM movements WHERE date >= :from AND date <= :through ORDER BY date, id")
    fun observeBetween(from: LocalDate, through: LocalDate): Flow<List<MovementEntity>>
    @Query("DELETE FROM movements WHERE id = :id")
    suspend fun deleteById(id: String): Int
}
