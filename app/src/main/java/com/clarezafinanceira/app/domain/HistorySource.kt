package com.clarezafinanceira.app.domain

import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

data class MonthlyHistoryItem(
    val period: YearMonth,
    val incomeCents: Long,
    val forecastExpensesCents: Long,
    val forecastLeftoverCents: Long?,
    val isCurrentPeriod: Boolean,
)

fun interface HistorySource {
    fun observeYearHistory(year: Int): Flow<List<MonthlyHistoryItem>>
}
