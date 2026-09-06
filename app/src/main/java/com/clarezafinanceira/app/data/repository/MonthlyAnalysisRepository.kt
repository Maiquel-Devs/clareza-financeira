package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.data.local.FinancialDatabase
import com.clarezafinanceira.app.data.local.toDomain
import com.clarezafinanceira.app.domain.MonthlyAnalysis
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MonthlyAnalysisRepository(
    private val database: FinancialDatabase,
    private val engine: MonthlyAnalysisEngine = MonthlyAnalysisEngine(),
) {
    /** Read facts atomically, then calculate outside the Room transaction and UI thread. */
    suspend fun getMonthlyAnalysis(period: YearMonth): MonthlyAnalysis = withContext(Dispatchers.Default) {
        val snapshot = database.monthlyAnalysisDao().snapshot(period)
        engine.analyze(
            period = period,
            movements = snapshot.movements.map { it.toDomain() },
            recurrences = snapshot.recurrences.map { it.toDomain() },
            versions = snapshot.versions.map { it.toDomain() },
            exceptions = snapshot.exceptions.map { it.toDomain() },
        )
    }

    /** Cold flow: an initial snapshot followed by database invalidations, without polling. */
    fun observeMonthlyAnalysis(period: YearMonth): Flow<MonthlyAnalysis> =
        database.invalidationTracker.createFlow(
            "movements", "recurrences", "recurrence_versions", "recurrence_exceptions",
        ).map { getMonthlyAnalysis(period) }.distinctUntilChanged()
}
