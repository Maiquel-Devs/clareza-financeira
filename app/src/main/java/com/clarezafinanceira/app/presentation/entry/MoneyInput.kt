package com.clarezafinanceira.app.presentation.entry

import java.math.BigDecimal

object MoneyInput {
    data class Result(val cents: Long? = null, val error: String? = null)
    fun parse(text: String): Result {
        val value = text.trim()
        if (value.isEmpty()) return Result(error = "Informe um valor.")
        if (!Regex("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+)(?:,[0-9]{1,2})?").matches(value))
            return Result(error = "Informe um valor válido, como 10,50.")
        val cents = try {
            BigDecimal(value.replace(".", "").replace(',', '.')).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return Result(error = "Informe um valor menor.")
        }
        return if (cents > 0) Result(cents) else Result(error = "O valor deve ser maior que zero.")
    }
    fun format(cents: Long): String = BigDecimal.valueOf(cents, 2).toPlainString().replace('.', ',')
}
