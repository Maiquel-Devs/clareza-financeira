package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import com.clarezafinanceira.app.presentation.history.*
import com.clarezafinanceira.app.presentation.entry.*
import java.time.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinancialChartsTest {
    @get:Rule val compose = createComposeRule()
    private val period = YearMonth.of(2026,9)
    private fun analysis(month: YearMonth = period, rent: Long = 30000) = MonthlyAnalysisEngine().analyze(month,
        movements = listOf(Movement("food",MovementType.EXPENSE,"Mercado",10000,ExpenseCategory.FOOD,month.atDay(1)),
            Movement("rent",MovementType.EXPENSE,"Aluguel",rent,ExpenseCategory.HOUSING,month.atDay(1))))
    private fun scroll(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun open(text: String) { scroll(text); compose.onNodeWithText(text).performClick() }

    @Test fun expenseChartStartsClosedAndCanOpenAndClose() {
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,analysis())) } }
        compose.onNodeWithText("R$ 300,00 · 75%").assertDoesNotExist()
        open("Ver gráfico das despesas")
        scroll("R$ 300,00 · 75%"); compose.onNodeWithText("R$ 300,00 · 75%").assertIsDisplayed()
        open("Ocultar gráfico")
        compose.onNodeWithText("R$ 300,00 · 75%").assertDoesNotExist()
        compose.onNodeWithText("Ver gráfico das despesas").assertIsDisplayed()
    }
    @Test fun openChartReactsToNewAnalysisWithoutManualRefresh() {
        val current = mutableStateOf(analysis())
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,current.value)) } }
        open("Ver gráfico das despesas")
        compose.runOnIdle { current.value = analysis(rent = 10000) }
        for (category in listOf("Alimentação", "Moradia")) {
            val row = hasText(category) and hasText("R$ 100,00 · 50%")
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(row)
            compose.onNode(row).assertIsDisplayed()
        }
        compose.onNodeWithText("R$ 300,00 · 75%").assertDoesNotExist()
    }
    @Test fun newPeriodResetsChartAndOpeningUsesSelectedHistoricalPeriod() {
        val current = mutableStateOf(analysis())
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(current.value.period,current.value),onBack = {}) } }
        open("Ver gráfico das despesas")
        compose.runOnIdle { current.value = analysis(period.minusMonths(1),10000) }
        open("Ver gráfico das despesas")
        scroll("Distribuição da previsão de despesas · Agosto de 2026")
        compose.onNodeWithText("Distribuição da previsão de despesas · Agosto de 2026").assertIsDisplayed()
        compose.onNodeWithText("+ Adicionar").assertDoesNotExist()
    }
    @Test fun emptyDashboardDoesNotOfferArtificialGraph() {
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,MonthlyAnalysisEngine().analyze(period))) } }
        compose.onNodeWithText("Nenhuma informação neste período.").assertIsDisplayed()
        compose.onNodeWithText("Ver gráfico das despesas").assertDoesNotExist()
    }
    private fun history(year: Int) = HistoryUiState(year,2026,listOf(
        MonthlyHistoryItem(YearMonth.of(year,1),10000,10000,0,false),
        MonthlyHistoryItem(YearMonth.of(year,2),10000,20000,-10000,false)))
    @Test fun annualChartStartsClosedAndRespondsToYearSelectionWhileOpen() {
        compose.setContent {
            var year by remember { mutableStateOf(2026) }
            DashboardTheme { HistoryScreen(history(year),{ year = it },{}, {}) }
        }
        compose.onNodeWithText("Previsão de despesas em 2026").assertDoesNotExist()
        open("Ver evolução do ano")
        compose.onNodeWithText("Previsão de despesas em 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ano anterior").performClick()
        compose.onNodeWithText("Previsão de despesas em 2025").assertIsDisplayed()
        compose.onNodeWithText("Previsão de despesas em 2026").assertDoesNotExist()
        open("Ocultar gráfico")
        compose.onNodeWithText("Previsão de despesas em 2025").assertDoesNotExist()
    }
    @Test fun insufficientHistoryExplainsInsteadOfDrawingEmptyChart() {
        compose.setContent { DashboardTheme { HistoryScreen(history(2026).copy(items = history(2026).items.take(1)),{}, {}, {}) } }
        open("Ver evolução do ano")
        compose.onNodeWithText("A evolução precisa de pelo menos dois meses com informação e alguma despesa prevista.").assertIsDisplayed()
    }
    @Test fun emptyYearRetainsExistingMessageWithoutGraph() {
        compose.setContent { DashboardTheme { HistoryScreen(HistoryUiState(2025,2026),{}, {}, {}) } }
        compose.onNodeWithText("Nenhuma informação financeira em 2025.").assertIsDisplayed()
        compose.onNodeWithText("Ver evolução do ano").assertDoesNotExist()
    }
    @Test fun chartAmountsAndCloseRemainReachableAtLargeFont() {
        compose.setContent { CompositionLocalProvider(LocalDensity provides Density(1f,1.6f)) {
            DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,analysis(rent = 99999999900))) }
        } }
        open("Ver gráfico das despesas")
        scroll("R$ 999.999.999,00 · 100%")
        compose.onNodeWithText("R$ 999.999.999,00 · 100%").assertIsDisplayed()
        open("Ocultar gráfico")
        compose.onNodeWithText("Ver gráfico das despesas").assertIsDisplayed()
    }
    @Test fun monthInputHasHumanPresentationWithoutChangingStoredInput() {
        var current = EntryState(date = LocalDate.of(2026,9,7),monthly = true)
        compose.setContent {
            var state by remember { mutableStateOf(current) }
            DashboardTheme { EntryScreen(state,{ state = it(state); current = state },{}, {}) }
        }
        compose.onNodeWithText("Setembro de 2026").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Mês de início (MM/AAAA)").performTextReplacement("08/2026")
        compose.onNodeWithText("Agosto de 2026").performScrollTo().assertIsDisplayed()
        assertEquals("08/2026",current.startMonth)
    }
}
