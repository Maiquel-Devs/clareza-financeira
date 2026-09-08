package com.clarezafinanceira.app.presentation.charts

import com.clarezafinanceira.app.domain.ExpenseCategory
import com.clarezafinanceira.app.domain.MonthlyHistoryItem
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.YearMonth

data class ExpenseBar(val category: ExpenseCategory, val amountCents: Long,
    val fraction: Float, val percentage: BigDecimal)
data class YearExpenseBar(val period: YearMonth, val amountCents: Long, val fraction: Float)

/** Display geometry only. All financial amounts are supplied by the existing monthly analysis. */
object ChartData {
    fun expenses(amounts: Map<ExpenseCategory, Long>): List<ExpenseBar> {
        val entries = amounts.entries.filter { it.value > 0 }.sortedWith(
            compareByDescending<Map.Entry<ExpenseCategory, Long>> { it.value }.thenBy { it.key.ordinal })
        if (entries.isEmpty()) return emptyList()
        val total = entries.fold(BigInteger.ZERO) { sum, entry -> sum + entry.value.toBigInteger() }
        // Largest remainders distribute tenths of a percent, so displayed shares sum to 100%.
        val portions = entries.map { (it.value.toBigInteger() * BigInteger.valueOf(1000)).divideAndRemainder(total) }
        val tenths = portions.map { it[0].toInt() }.toMutableList()
        val remainderOrder = entries.indices.sortedByDescending { portions[it][1] }
        repeat(1000 - tenths.sum()) { tenths[remainderOrder[it]]++ }
        return entries.mapIndexed { index, entry ->
            ExpenseBar(entry.key, entry.value, ratio(entry.value.toBigInteger(), total),
                BigDecimal.valueOf(tenths[index].toLong(), 1))
        }
    }

    fun year(items: List<MonthlyHistoryItem>): List<YearExpenseBar> {
        if (items.size < 2) return emptyList()
        val maximum = items.maxOf { it.forecastExpensesCents }
        if (maximum <= 0) return emptyList()
        return items.sortedBy { it.period }.map {
            YearExpenseBar(it.period, it.forecastExpensesCents,
                ratio(it.forecastExpensesCents.coerceAtLeast(0).toBigInteger(), maximum.toBigInteger()))
        }
    }

    private fun ratio(value: BigInteger, total: BigInteger): Float =
        value.toBigDecimal().divide(total.toBigDecimal(), 12, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
}
