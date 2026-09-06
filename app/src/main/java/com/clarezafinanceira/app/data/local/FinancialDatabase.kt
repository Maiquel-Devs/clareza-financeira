package com.clarezafinanceira.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MovementEntity::class, RecurrenceEntity::class,
        RecurrenceVersionEntity::class, RecurrenceExceptionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(FinancialConverters::class)
abstract class FinancialDatabase : RoomDatabase() {
    abstract fun movementDao(): MovementDao
    abstract fun recurrenceDao(): RecurrenceDao
    abstract fun monthlyAnalysisDao(): MonthlyAnalysisDao

    companion object {
        @Volatile private var instance: FinancialDatabase? = null

        fun getInstance(context: Context): FinancialDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, FinancialDatabase::class.java, "clareza-financeira.db",
                ).build().also { instance = it }
            }
    }
}
