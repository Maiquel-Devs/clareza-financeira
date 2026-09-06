package com.clarezafinanceira.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class MonthlyAnalysisEngineTest {
    private val month = YearMonth.of(2026, 9)
    private val engine = MonthlyAnalysisEngine()
    private fun income(amount: Long = 300000, period: YearMonth = month) =
        Movement("income", MovementType.INCOME, "Salary", amount, null, period.atDay(15))
    private fun expense(amount: Long = 100000, category: ExpenseCategory = ExpenseCategory.FOOD,
        id: String = "expense", period: YearMonth = month) =
        Movement(id, MovementType.EXPENSE, "Expense", amount, category, period.atDay(15))
    private fun recurrence(start: YearMonth = month, end: YearMonth? = null,
        type: MovementType = MovementType.EXPENSE) = Recurrence("r", type, start, end)
    private fun version(amount: Long = 10000, from: YearMonth = month,
        id: String = "v", category: ExpenseCategory? = ExpenseCategory.HOUSING, day: Int = 15) =
        RecurrenceVersion(id, "r", from, "Rent", amount, category, day)
    private fun analyzeRecurring(period: YearMonth = month, r: Recurrence = recurrence(),
        versions: List<RecurrenceVersion> = listOf(version()),
        exceptions: List<RecurrenceException> = emptyList()) =
        engine.analyze(period, recurrences = listOf(r), versions = versions, exceptions = exceptions)
    private fun exception(period: YearMonth = month) = RecurrenceException("e", "r", period)

    @Test fun emptyMonth() {
        val result = engine.analyze(month)
        assertEquals(month, result.period)
        assertTrue(result.isEmpty)
        assertEquals(0L, result.registeredIncomeCents)
        assertEquals(0L, result.recurringIncomeCents)
        assertEquals(0L, result.consideredIncomeCents)
        assertEquals(0L, result.registeredExpenseCents)
        assertEquals(0L, result.forecastExpenseCents)
        assertNull(result.forecastRemainingCents)
        assertNull(result.expensePercentageOfIncome)
        assertTrue(result.expensesByCategory.isEmpty())
    }

    @Test fun onlyPunctualIncome() {
        val result = engine.analyze(month, listOf(income()))
        assertEquals(300000L, result.registeredIncomeCents)
        assertEquals(300000L, result.consideredIncomeCents)
        assertEquals(300000L, result.forecastRemainingCents)
        assertEquals(BigDecimal("0.0000"), result.expensePercentageOfIncome)
        assertFalse(result.isEmpty)
        assertEquals("income", result.incomeSources.single().sourceId)
    }

    @Test fun onlyPunctualExpense() {
        val result = engine.analyze(month, listOf(expense()))
        assertEquals(100000L, result.registeredExpenseCents)
        assertEquals(100000L, result.forecastExpenseCents)
        assertFalse(result.isEmpty)
    }

    @Test fun incomeAndExpenses() {
        val result = engine.analyze(month, listOf(income(300000), expense(150000)))
        assertEquals(150000L, result.forecastRemainingCents)
        assertEquals(BigDecimal("50.0000"), result.expensePercentageOfIncome)
    }

    @Test fun expensesGreaterThanIncome() {
        val result = engine.analyze(month, listOf(income(300000), expense(350000)))
        assertEquals(-50000L, result.forecastRemainingCents)
    }

    @Test fun absentIncomeHasNoArtificialRemainingOrPercentage() {
        val result = engine.analyze(month, listOf(expense()), listOf(recurrence()), listOf(version()))
        assertEquals(110000L, result.forecastExpenseCents)
        assertNull(result.forecastRemainingCents)
        assertNull(result.expensePercentageOfIncome)
    }

    @Test fun percentageCanExceedOneHundred() {
        val result = engine.analyze(month, listOf(income(100000), expense(150000)))
        assertEquals(BigDecimal("150.0000"), result.expensePercentageOfIncome)
    }

    @Test fun otherMonthMovementsIgnored() {
        val result = engine.analyze(month, listOf(income(period = month.minusMonths(1)),
            expense(period = month.plusMonths(1))))
        assertTrue(result.isEmpty)
    }

    @Test fun recurrenceBeforeStartIgnored() {
        assertTrue(analyzeRecurring(period = month.minusMonths(1)).isEmpty)
    }

    @Test fun recurrenceAfterEndIgnored() {
        assertTrue(analyzeRecurring(period = month.plusMonths(1), r = recurrence(end = month)).isEmpty)
    }

    @Test fun recurrenceActiveAtInclusiveStartAndEnd() {
        val r = recurrence(end = month.plusMonths(1))
        assertEquals(10000L, analyzeRecurring(r = r).forecastExpenseCents)
        assertEquals(10000L, analyzeRecurring(period = month.plusMonths(1), r = r).forecastExpenseCents)
    }

    @Test fun selectsLatestEffectiveVersionFromUnsortedInput() {
        val january = YearMonth.of(2026, 1)
        val versions = listOf(version(15000, month, "september"),
            version(10000, january, "january"), version(12000, YearMonth.of(2026, 5), "may"))
        val result = analyzeRecurring(YearMonth.of(2026, 7), recurrence(start = january), versions)
        assertEquals(12000L, result.forecastExpenseCents)
        assertEquals("may", result.expenseItems.single().versionId)
    }

    @Test fun futureVersionIgnored() {
        val result = analyzeRecurring(versions = listOf(version(),
            version(99999, month.plusMonths(1), "future")))
        assertEquals(10000L, result.forecastExpenseCents)
        assertEquals("v", result.expenseItems.single().versionId)
    }

    @Test fun permanentChangePreservesEarlierPeriod() {
        val versions = listOf(version(), version(20000, month.plusMonths(1), "next"))
        assertEquals(10000L, analyzeRecurring(versions = versions).forecastExpenseCents)
        assertEquals(20000L, analyzeRecurring(period = month.plusMonths(1), versions = versions).forecastExpenseCents)
        assertEquals(10000L, analyzeRecurring(versions = versions).forecastExpenseCents)
    }

    @Test fun monthlyAmountOverride() {
        val result = analyzeRecurring(exceptions = listOf(exception().copy(overrideAmountCents = 7000)))
        assertEquals(7000L, result.forecastExpenseCents)
        assertEquals("e", result.expenseItems.single().exceptionId)
    }

    @Test fun nameAndCategoryOverride() {
        val result = analyzeRecurring(exceptions = listOf(exception().copy(
            overrideName = "Holiday", overrideCategory = ExpenseCategory.LEISURE)))
        assertEquals("Holiday", result.expenseItems.single().name)
        assertEquals(mapOf(ExpenseCategory.LEISURE to 10000L), result.expensesByCategory)
    }

    @Test fun exclusionRemovesOnlyItsMonth() {
        val exceptions = listOf(exception().copy(excluded = true))
        assertTrue(analyzeRecurring(exceptions = exceptions).isEmpty)
        assertEquals(10000L, analyzeRecurring(period = month.plusMonths(1), exceptions = exceptions).forecastExpenseCents)
    }

    @Test fun overrideDoesNotAffectNextMonth() {
        val exceptions = listOf(exception().copy(overrideAmountCents = 50000))
        assertEquals(50000L, analyzeRecurring(exceptions = exceptions).forecastExpenseCents)
        assertEquals(10000L, analyzeRecurring(period = month.plusMonths(1), exceptions = exceptions).forecastExpenseCents)
    }

    @Test fun habitualDayDoesNotControlInclusionEvenInFebruary() {
        val february = YearMonth.of(2026, 2)
        val result = analyzeRecurring(february, recurrence(start = february),
            listOf(version(from = february, day = 31)))
        assertEquals(10000L, result.forecastExpenseCents)
        assertEquals(31, result.expenseItems.single().habitualDay)
        assertNull(result.expenseItems.single().date)
    }

    @Test fun groupsPunctualAndRecurringExpensesByCategory() {
        val result = engine.analyze(month, listOf(expense(500, ExpenseCategory.HOUSING),
            expense(250, ExpenseCategory.FOOD, "food1"), expense(750, ExpenseCategory.FOOD, "food2")),
            listOf(recurrence()), listOf(version()))
        assertEquals(mapOf(ExpenseCategory.FOOD to 1000L, ExpenseCategory.HOUSING to 10500L),
            result.expensesByCategory)
        assertEquals(result.forecastExpenseCents, result.expensesByCategory.values.sum())
    }

    @Test fun unusedCategoriesAbsent() {
        val result = engine.analyze(month, listOf(expense()))
        assertEquals(setOf(ExpenseCategory.FOOD), result.expensesByCategory.keys)
    }

    @Test fun recurringIncomeCountsInConsideredIncome() {
        val result = analyzeRecurring(r = recurrence(type = MovementType.INCOME),
            versions = listOf(version(50000, category = null)))
        assertEquals(0L, result.registeredIncomeCents)
        assertEquals(50000L, result.recurringIncomeCents)
        assertEquals(50000L, result.consideredIncomeCents)
        assertEquals(50000L, result.forecastRemainingCents)
        assertEquals(ItemOrigin.RECURRENCE, result.incomeSources.single().origin)
        assertNull(result.incomeSources.single().category)
    }

    @Test fun recurringExpenseCountsInForecast() {
        assertEquals(10000L, analyzeRecurring().forecastExpenseCents)
    }

    @Test fun recurringExpenseIsNotRegisteredExpense() {
        assertEquals(0L, analyzeRecurring().registeredExpenseCents)
    }

    @Test fun negativeRemainingPreservedWithRecurringExpenses() {
        val result = engine.analyze(month, listOf(income(300000), expense(340000)),
            listOf(recurrence()), listOf(version()))
        assertEquals(-50000L, result.forecastRemainingCents)
        assertEquals(340000L, result.registeredExpenseCents)
        assertEquals(350000L, result.forecastExpenseCents)
    }

    @Test fun combinedIncomeIsPercentageDenominator() {
        val result = engine.analyze(month, listOf(income(50000), expense(150000)),
            listOf(recurrence(type = MovementType.INCOME)), listOf(version(50000, category = null)))
        assertEquals(50000L, result.registeredIncomeCents)
        assertEquals(100000L, result.consideredIncomeCents)
        assertEquals(BigDecimal("150.0000"), result.expensePercentageOfIncome)
        assertEquals(-50000L, result.forecastRemainingCents)
    }

    @Test fun nullOverridesInheritAndDayOverrideIsRetained() {
        val result = analyzeRecurring(exceptions = listOf(exception().copy(overrideHabitualDay = 31)))
        val item = result.expenseItems.single()
        assertEquals("Rent", item.name)
        assertEquals(10000L, item.amountCents)
        assertEquals(ExpenseCategory.HOUSING, item.category)
        assertEquals(31, item.habitualDay)
    }

    @Test fun excludedIgnoresInvalidOverrides() {
        assertTrue(analyzeRecurring(exceptions = listOf(exception().copy(excluded = true,
            overrideAmountCents = -1, overrideHabitualDay = 99, overrideName = ""))).isEmpty)
    }

    @Test fun recurringIncomeExceptionAndExclusion() {
        val r = recurrence(type = MovementType.INCOME)
        val versions = listOf(version(category = null))
        assertEquals(500L, analyzeRecurring(r = r, versions = versions,
            exceptions = listOf(exception().copy(overrideAmountCents = 500))).consideredIncomeCents)
        assertNull(analyzeRecurring(r = r, versions = versions,
            exceptions = listOf(exception().copy(excluded = true))).forecastRemainingCents)
    }

    @Test fun noCarryOverBetweenMonths() {
        val movements = listOf(income(), expense(period = month.plusMonths(1)))
        engine.analyze(month, movements)
        val next = engine.analyze(month.plusMonths(1), movements)
        assertEquals(0L, next.consideredIncomeCents)
        assertNull(next.forecastRemainingCents)
    }

    @Test fun percentageRoundsSafelyToFourDecimalPlaces() {
        val result = engine.analyze(month, listOf(income(3), expense(1)))
        assertEquals(BigDecimal("33.3333"), result.expensePercentageOfIncome)
    }

    @Test fun longValuesDoNotUseFloatingPoint() {
        val value = 9007199254740993L
        val result = engine.analyze(month, listOf(income(value), expense(value - 1)))
        assertEquals(value, result.consideredIncomeCents)
        assertEquals(value - 1, result.forecastExpenseCents)
        assertEquals(1L, result.forecastRemainingCents)
    }

    @Test fun overflowFailsExplicitlyInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) {
            engine.analyze(month, listOf(expense(Long.MAX_VALUE), expense(1, id = "second")))
        }
        assertThrows(ArithmeticException::class.java) {
            engine.analyze(month, listOf(income(Long.MAX_VALUE)), listOf(recurrence(type = MovementType.INCOME)),
                listOf(version(1, category = null)))
        }
    }

    @Test fun missingEffectiveVersionFailsInsteadOfInventingValue() {
        assertThrows(IllegalArgumentException::class.java) {
            analyzeRecurring(versions = listOf(version(from = month.plusMonths(1))))
        }
        assertThrows(IllegalArgumentException::class.java) { analyzeRecurring(versions = emptyList()) }
    }

    @Test fun duplicateKeysFailInsteadOfDependingOnInputOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            analyzeRecurring(versions = listOf(version(), version(id = "other")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            analyzeRecurring(exceptions = listOf(exception(), exception().copy(id = "other")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            engine.analyze(month, listOf(expense(), expense()))
        }
    }

    @Test fun reorderedInputsProduceSameAnalysis() {
        val movements = listOf(expense(id = "b"), expense(id = "a"), income())
        val versions = listOf(version(), version(from = month.plusMonths(1), id = "future"))
        val a = engine.analyze(month, movements, listOf(recurrence()), versions)
        val b = engine.analyze(month, movements.reversed(), listOf(recurrence()), versions.reversed())
        assertEquals(a, b)
        assertEquals(listOf("a", "b", "r"), a.expenseItems.map { it.sourceId })
    }

    @Test fun invalidEffectiveValuesRejected() {
        assertThrows(IllegalArgumentException::class.java) { engine.analyze(month, listOf(expense(0))) }
        assertThrows(IllegalArgumentException::class.java) { engine.analyze(month, listOf(income().copy(category = ExpenseCategory.FOOD))) }
        assertThrows(IllegalArgumentException::class.java) {
            analyzeRecurring(exceptions = listOf(exception().copy(overrideHabitualDay = 32)))
        }
    }

    @Test fun exceptionsAreScopedToRecurrenceId() {
        assertEquals(10000L, analyzeRecurring(exceptions =
            listOf(exception().copy(recurrenceId = "different", excluded = true))).forecastExpenseCents)
    }
}
