package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.clarezafinanceira.app.AppContainer
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.entry.EntryNavigation
import com.clarezafinanceira.app.presentation.history.*
import java.time.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val period = YearMonth.of(2026, 9)
    private val state = MonthlyAnalysisUiState.Success(period, MonthlyAnalysisEngine().analyze(period))

    @Test fun currentDashboardOffersAddAndHistory() {
        var opened = false
        compose.setContent { DashboardTheme { DashboardScreen(state, onAdd = {}, onHistory = { opened = true }) } }
        compose.onNodeWithText("+ Adicionar").assertIsDisplayed()
        compose.onNodeWithText("Histórico").performClick()
        assertTrue(opened)
        compose.onNodeWithText("Voltar").assertDoesNotExist()
    }

    @Test fun historicalDashboardHidesAddAndHistoryAndHasBack() {
        var returned = false
        compose.setContent { DashboardTheme { DashboardScreen(state, onAdd = {}, onHistory = {}, onBack = { returned = true }) } }
        compose.onNodeWithText("+ Adicionar").assertDoesNotExist()
        compose.onNodeWithText("Histórico").assertDoesNotExist()
        compose.onNodeWithText("Voltar").performClick()
        assertTrue(returned)
    }

    @Test fun currentYearDisablesFutureNavigation() {
        compose.setContent { DashboardTheme { HistoryScreen(HistoryUiState(2026,2026), {}, {}, {}) } }
        compose.onNodeWithContentDescription("Próximo ano").assertIsNotEnabled()
        compose.onNodeWithText("Nenhuma informação financeira em 2026.").assertIsDisplayed()
    }

    @Test fun previousYearCanNavigateBothWays() {
        var year = 0
        compose.setContent { DashboardTheme { HistoryScreen(HistoryUiState(2025,2026), { year = it }, {}, {}) } }
        compose.onNodeWithContentDescription("Ano anterior").performClick()
        assertEquals(2024,year)
        compose.onNodeWithContentDescription("Próximo ano").performClick()
        assertEquals(2026,year)
    }

    @Test fun cardOpensExactPeriodAndShowsNoIncomeSemantics() {
        var selected: YearMonth? = null
        compose.setContent { DashboardTheme { HistoryScreen(HistoryUiState(2026,2026,
            listOf(MonthlyHistoryItem(period,0,10000,null,true))), {}, {}, { selected = it }) } }
        compose.onNodeWithText("ATUAL").assertExists()
        compose.onNodeWithText("Nenhuma renda informada").assertExists()
        compose.onNodeWithText("Não disponível").assertExists()
        compose.onNodeWithText("Setembro").performClick()
        assertEquals(period,selected)
    }

    @Test fun actualNavigationReusesDashboardAndRestoresHistoryScroll() {
        val container = AppContainer(RuntimeEnvironment.getApplication())
        val now = YearMonth.now(container.clock)
        val year = now.year - 1
        val db = com.clarezafinanceira.app.data.local.FinancialDatabase.getInstance(RuntimeEnvironment.getApplication())
        kotlinx.coroutines.runBlocking {
            for (month in 1..12) {
                db.movementDao().insert(com.clarezafinanceira.app.data.local.MovementEntity(
                    "history-$month", com.clarezafinanceira.app.data.local.MovementType.INCOME,
                    "Renda teste", 10000,null,YearMonth.of(year,month).atDay(1),Instant.now(),Instant.now()))
            }
        }
        val vm = MonthlyAnalysisViewModel(container.monthlyAnalysisRepository, container.clock)
        compose.setContent { DashboardTheme { EntryNavigation(container,vm) } }
        compose.onNodeWithText("Histórico").performClick()
        compose.onNodeWithContentDescription("Ano anterior").performClick()
        val title = "Abril"
        compose.waitUntil(10000) { compose.onAllNodesWithText("Dezembro").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("history-months").performScrollToNode(hasText(title))
        val previousScroll = compose.onNodeWithTag("history-months").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        compose.onNodeWithText(title).assertIsDisplayed().performClick()
        compose.onNodeWithText(DashboardFormatting.period(YearMonth.of(year,4))).assertIsDisplayed()
        compose.onNodeWithText("+ Adicionar").assertDoesNotExist()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("Histórico").assertIsDisplayed()
        compose.onNodeWithText(title).assertIsDisplayed()
        val restoredScroll = compose.onNodeWithTag("history-months").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(previousScroll, restoredScroll)
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("+ Adicionar").assertIsDisplayed()
        compose.onNodeWithText(DashboardFormatting.period(now)).assertIsDisplayed()
    }
}
