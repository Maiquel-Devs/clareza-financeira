package com.clarezafinanceira.app.presentation.entry

import org.junit.Assert.*
import org.junit.Test

class MoneyInputTest {
    @Test fun integer() { assertEquals(1000L, MoneyInput.parse("10").cents) }
    @Test fun comma() { assertEquals(1050L, MoneyInput.parse("10,50").cents) }
    @Test fun grouped() { assertEquals(100000L, MoneyInput.parse("1.000,00").cents) }
    @Test fun empty() { assertEquals("Informe um valor.", MoneyInput.parse("  ").error) }
    @Test fun zero() { assertEquals("O valor deve ser maior que zero.", MoneyInput.parse("0,00").error) }
    @Test fun invalid() {
        listOf("abc", "-1", "1.50", "1,234", "1e3", "1,2,3", "NaN").forEach {
            assertNotNull(it, MoneyInput.parse(it).error)
        }
    }
    @Test fun overflow() { assertNotNull(MoneyInput.parse("92233720368547758,08").error) }
    @Test fun fullLongPrecisionRoundTrip() {
        assertEquals(Long.MAX_VALUE, MoneyInput.parse(MoneyInput.format(Long.MAX_VALUE)).cents)
    }
}
