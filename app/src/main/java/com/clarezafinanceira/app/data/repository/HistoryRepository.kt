package com.clarezafinanceira.app.data.repository

import com.clarezafinanceira.app.domain.*
import java.time.Clock
import java.time.YearMonth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

/** At most twelve existing monthly streams; no stored summaries or financial rules. */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepository(private val source: MonthlyAnalysisSource, clock: Clock,
    private val currentPeriod: CurrentPeriod = CurrentPeriod(clock)) : HistorySource {
    override fun observeYearHistory(year: Int): Flow<List<MonthlyHistoryItem>> =
        currentPeriod.period.flatMapLatest { current -> observeYear(year, current) }

    private fun observeYear(year: Int, current: YearMonth): Flow<List<MonthlyHistoryItem>> {
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
