package com.clarezafinanceira.app.presentation.dashboard

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Display conversion only: money stays exact; percentages already come in percent units. */
object DashboardFormatting {
    private val locale = Locale.forLanguageTag("pt-BR")
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM 'de' uuuu", locale)

    fun money(cents: Long): String = NumberFormat.getCurrencyInstance(locale)
        .format(BigDecimal.valueOf(cents, 2))
        .replace('\u00a0', ' ')

    fun period(period: YearMonth): String = period.format(monthFormatter)
        .replaceFirstChar { it.titlecase(locale) }

    fun percentage(value: BigDecimal): String = NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 1
        roundingMode = RoundingMode.HALF_UP
    }.format(value) + "%"
}
