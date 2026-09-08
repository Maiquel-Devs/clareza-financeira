package com.clarezafinanceira.app.presentation

import com.clarezafinanceira.app.presentation.charts.ChartData
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.data.repository.HistoryRepository
import java.time.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*

class ChartDataTest {
    private val period = YearMonth.of(2026,9)
    private fun month(month: Int, amount: Long) = MonthlyHistoryItem(YearMonth.of(2026,month),10000,amount,0,month == 9)

    @Test fun expensesExcludeZeroAndSortByAmount() {
        val rows = ChartData.expenses(mapOf(ExpenseCategory.FOOD to 2500,ExpenseCategory.HOUSING to 7500,ExpenseCategory.OTHER to 0))
        assertEquals(listOf(ExpenseCategory.HOUSING,ExpenseCategory.FOOD),rows.map { it.category })
        assertEquals(listOf("75.0","25.0"),rows.map { it.percentage.toPlainString() })
        assertEquals(0.75f,rows.first().fraction)
    }
    @Test fun roundingAddsToOneHundredWithDeterministicTies() {
        val rows = ChartData.expenses(mapOf(ExpenseCategory.FOOD to 1,ExpenseCategory.HOUSING to 1,ExpenseCategory.OTHER to 1))
        assertEquals(listOf("33.4","33.3","33.3"),rows.map { it.percentage.toPlainString() })
        assertEquals("100.0",rows.sumOf { it.percentage }.toPlainString())
    }
    @Test fun zeroTotalHasNoBars() { assertTrue(ChartData.expenses(mapOf(ExpenseCategory.FOOD to 0)).isEmpty()) }
    @Test fun largeAmountsCannotOverflowOrProduceInvalidWidths() {
        val rows = ChartData.expenses(mapOf(ExpenseCategory.FOOD to Long.MAX_VALUE,ExpenseCategory.HOUSING to Long.MAX_VALUE))
        assertTrue(rows.all { it.fraction == 0.5f && it.fraction.isFinite() })
        assertEquals(listOf("50.0","50.0"),rows.map { it.percentage.toPlainString() })
    }
    @Test fun expenseSharesUseForecastInsteadOfIncomeOrRegisteredOnly() {
        val analysis = MonthlyAnalysisEngine().analyze(period,
            movements = listOf(Movement("m",MovementType.EXPENSE,"Mercado",10000,ExpenseCategory.FOOD,period.atDay(1))),
            recurrences = listOf(Recurrence("r",MovementType.EXPENSE,period.minusMonths(1))),
            versions = listOf(RecurrenceVersion("v1","r",period.minusMonths(1),"Aluguel",20000,ExpenseCategory.HOUSING,7),
                RecurrenceVersion("v2","r",period,"Aluguel",30000,ExpenseCategory.HOUSING,7)),
            exceptions = listOf(RecurrenceException("e","r",period,overrideAmountCents = 40000)))
        val rows = ChartData.expenses(analysis.expensesByCategory)
        assertEquals(analysis.forecastExpenseCents,rows.sumOf { it.amountCents })
        assertEquals(40000L,rows.first().amountCents)
        assertEquals("80.0",rows.first().percentage.toPlainString())
    }
    @Test fun annualBarsUseCommonScaleAndChronologicalOrder() {
        val rows = ChartData.year(listOf(month(9,20000),month(1,10000),month(4,0)))
        assertEquals(listOf(1,4,9),rows.map { it.period.monthValue })
        assertEquals(listOf(0.5f,0f,1f),rows.map { it.fraction })
    }
    @Test fun insufficientAnnualDataDoesNotCreateArtificialChart() {
        assertTrue(ChartData.year(emptyList()).isEmpty())
        assertTrue(ChartData.year(listOf(month(9,10000))).isEmpty())
        assertTrue(ChartData.year(listOf(month(8,0),month(9,0))).isEmpty())
    }
    @Test fun annualScaleHandlesLongLimit() {
        val rows = ChartData.year(listOf(month(8,1),month(9,Long.MAX_VALUE)))
        assertTrue(rows.all { it.fraction.isFinite() && it.fraction in 0f..1f })
        assertEquals(1f,rows.last().fraction)
    }
    @Test fun historyProjectionKeepsRelevantPeriodsAndExcludesFuture() = runBlocking {
        val source = MonthlyAnalysisSource { month -> flowOf(MonthlyAnalysisEngine().analyze(month,
            recurrences = listOf(Recurrence("r",MovementType.EXPENSE,YearMonth.of(2026,7))),
            versions = listOf(RecurrenceVersion("v","r",YearMonth.of(2026,7),"Aluguel",10000,ExpenseCategory.HOUSING,7)),
            exceptions = listOf(RecurrenceException("e","r",YearMonth.of(2026,8),excluded = true)))) }
        val history = HistoryRepository(source,Clock.fixed(Instant.parse("2026-09-08T12:00:00Z"),ZoneOffset.UTC))
        assertEquals(listOf(7,9),ChartData.year(history.observeYearHistory(2026).first()).map { it.period.monthValue })
    }
}
