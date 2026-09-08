package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.domain.*
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.*

/** At most twelve existing monthly streams; no stored summaries or financial rules. */
class HistoryRepository(private val source: MonthlyAnalysisSource, private val clock: Clock) : HistorySource {
    override fun observeYearHistory(year: Int): Flow<List<MonthlyHistoryItem>> {
        val current = YearMonth.now(clock)
        val periods = (12 downTo 1).map { YearMonth.of(year, it) }.filter { it <= current }
        if (periods.isEmpty()) return flowOf(emptyList())
        return combine(periods.map(source::observeMonthlyAnalysis)) { analyses ->
            analyses.filterNot { it.isEmpty }.map {
                MonthlyHistoryItem(it.period, it.consideredIncomeCents, it.forecastExpenseCents,
                    it.forecastRemainingCents, it.period == current)
            }
        }.distinctUntilChanged()
    }
}
