package com.clarezafinanceira.app.presentation.dashboard

import java.math.BigDecimal
import java.time.YearMonth
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardFormattingTest {
    @Test fun moneyUsesBrazilianGroupingAndDecimalSeparator() {
        assertEquals("R$ 10.000,00", DashboardFormatting.money(1000000))
    }

    @Test fun centsAndZeroAreExact() {
        assertEquals("R$ 0,01", DashboardFormatting.money(1))
        assertEquals("R$ 0,00", DashboardFormatting.money(0))
        assertEquals("-R$ 12,34", DashboardFormatting.money(-1234))
    }

    @Test fun largeMoneyDoesNotLoseCentsToFloatingPoint() {
        assertEquals("R$ 92.233.720.368.547.758,07", DashboardFormatting.money(Long.MAX_VALUE))
    }

    @Test fun monthAndYearArePortugueseRegardlessOfDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            assertEquals("Setembro de 2026", DashboardFormatting.period(YearMonth.of(2026, 9)))
            assertEquals("Janeiro de 2031", DashboardFormatting.period(YearMonth.of(2031, 1)))
            assertEquals("R$ 1.234,56", DashboardFormatting.money(123456))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test fun percentageIsNotMultipliedOrCapped() {
        assertEquals("150%", DashboardFormatting.percentage(BigDecimal("150.0000")))
        assertEquals("33,3%", DashboardFormatting.percentage(BigDecimal("33.3333")))
        assertEquals("0%", DashboardFormatting.percentage(BigDecimal.ZERO))
    }
}
