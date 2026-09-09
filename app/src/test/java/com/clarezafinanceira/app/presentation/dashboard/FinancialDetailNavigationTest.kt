package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.AppContainer
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.entry.EntryNavigation
import java.time.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinancialDetailNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val container = AppContainer(RuntimeEnvironment.getApplication())
    private val period = YearMonth.now(container.clock)
    private val db = com.clarezafinanceira.app.data.local.FinancialDatabase.getInstance(RuntimeEnvironment.getApplication())
    private lateinit var vm: MonthlyAnalysisViewModel
    private fun save(name: String, type: MovementType = MovementType.EXPENSE, monthly: Boolean = false,
        month: YearMonth = period) = runBlocking {
        container.financialEntryRepository.save(EntryInput(type,name,10000,
            if (type == MovementType.EXPENSE) ExpenseCategory.HOUSING else null,month.atDay(1),monthly,month,7))
    }
    private fun start() {
        vm = MonthlyAnalysisViewModel(container.monthlyAnalysisRepository,container.clock)
        compose.setContent { DashboardTheme { EntryNavigation(container,vm) } }
        compose.waitUntil(10000) { compose.onAllNodesWithText("Renda").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun cleanup() { if (::vm.isInitialized) compose.runOnUiThread { vm.viewModelScope.cancel() } }
    private fun scroll(text: String) { compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text)) }
    private fun open(text: String) { scroll(text); compose.onNodeWithText(text).performClick() }
    private fun back() { scroll("Voltar"); compose.onNodeWithText("Voltar").performClick() }
    private fun position() = compose.onNode(hasScrollToNodeAction()).fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange].value()
    private fun waitFor(text: String) { compose.waitUntil(10000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }

    @Test fun categoryAndDashboardRestoreScrollAndForwardStartsAtTop() {
        for (i in 1..12) save("Gasto $i")
        start(); scroll("Moradia"); val dashboardScroll = position()
        compose.onNodeWithText("Moradia").performClick(); waitFor("12 gastos")
        compose.onNodeWithText("Voltar").assertIsDisplayed()
        scroll("Gasto 8"); val listScroll = position()
        compose.onNodeWithText("Gasto 8").performClick(); waitFor("Detalhes da despesa")
        compose.onNodeWithText("Voltar").assertIsDisplayed()
        back(); assertEquals(listScroll,position())
        back()
        assertEquals(dashboardScroll,position())
        compose.onNodeWithText("Moradia").assertIsDisplayed()
    }

    @Test fun incomeGoesDirectlyToDetailThenExistingPunctualEditor() {
        save("Freelance",MovementType.INCOME)
        start(); open("Freelance"); waitFor("Detalhes da renda")
        compose.onNodeWithTag("category-list").assertDoesNotExist()
        open("Editar"); waitFor("Editar renda")
        compose.onNodeWithText("Freelance").assertExists()
        compose.onNodeWithText("Origem / nome da renda").assertExists()
    }

    @Test fun deletingPunctualReturnsToCategoryAndCancelPreservesDatabase() {
        save("Excluir teste")
        start(); open("Moradia"); waitFor("1 gasto"); open("Excluir teste"); waitFor("Detalhes da despesa")
        open("Excluir movimentação"); compose.onNodeWithText("Cancelar").performClick()
        assertEquals(1,runBlocking { container.monthlyAnalysisRepository.getMonthlyAnalysis(period).expenseItems.size })
        compose.onNodeWithText("Excluir movimentação").performClick(); compose.onNodeWithText("Excluir").performClick()
        waitFor("Nenhum gasto nesta categoria neste período.")
        compose.onNodeWithTag("financial-detail").assertDoesNotExist()
        assertTrue(runBlocking { container.monthlyAnalysisRepository.getMonthlyAnalysis(period).isEmpty })
    }

    @Test fun historicalRecurrenceEditKeepsMonthAndUpdatesDetailThenHistory() {
        val past = YearMonth.of(period.year - 1,4)
        save("Aluguel histórico",monthly = true,month = past)
        start(); compose.onNodeWithText("Histórico").performClick()
        compose.onNodeWithContentDescription("Ano anterior").performClick()
        waitFor("Dezembro")
        scroll("Abril"); val historyScroll = position()
        compose.onNodeWithText("Abril").performClick()
        waitFor("Renda"); compose.onNodeWithText("+ Adicionar").assertDoesNotExist()
        open("Moradia"); waitFor("1 gasto"); open("Aluguel histórico"); waitFor("Detalhes da despesa")
        scroll(DashboardFormatting.period(past)); compose.onNodeWithText(DashboardFormatting.period(past)).assertIsDisplayed()
        open("Editar"); waitFor("Valor (R$)")
        compose.onNodeWithText("Valor (R$)").performTextReplacement("170,00")
        compose.onNodeWithText("Salvar alteração").performScrollTo().performClick()
        val label = DashboardFormatting.period(past).replaceFirstChar { it.lowercase() }
        compose.onNodeWithText("Somente em $label").assertIsDisplayed().performClick()
        waitFor("Detalhes da despesa"); scroll("R$ 170,00")
        compose.onNodeWithText("R$ 170,00").assertIsDisplayed()
        assertEquals(10000L,runBlocking { container.monthlyAnalysisRepository.getMonthlyAnalysis(past.plusMonths(1)).forecastExpenseCents })
        back(); back(); back()
        assertEquals(historyScroll,position())
        compose.onNodeWithText("Histórico").assertIsDisplayed()
    }
}
