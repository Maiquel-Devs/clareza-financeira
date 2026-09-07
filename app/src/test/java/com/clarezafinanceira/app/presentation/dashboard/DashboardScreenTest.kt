package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import java.math.BigDecimal
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardScreenTest {
    @get:Rule val compose = createComposeRule()
    private val period = YearMonth.of(2026, 9)

    private fun analysis(withIncome: Boolean = true): MonthlyAnalysis = MonthlyAnalysisEngine().analyze(
        period,
        movements = buildList {
            if (withIncome) add(Movement("extra", MovementType.INCOME, "Trabalho extra", 100000, null, period.atDay(1)))
            add(Movement("food", MovementType.EXPENSE, "Mercado", 20000, ExpenseCategory.FOOD, period.atDay(2)))
            add(Movement("meal", MovementType.EXPENSE, "Almoço", 5000, ExpenseCategory.FOOD, period.atDay(3)))
        },
        recurrences = buildList {
            add(Recurrence("rent", MovementType.EXPENSE, period))
            if (withIncome) add(Recurrence("salary", MovementType.INCOME, period))
        },
        versions = buildList {
            add(RecurrenceVersion("rent-v1", "rent", period, "Aluguel", 150000, ExpenseCategory.HOUSING, 10))
            if (withIncome) add(RecurrenceVersion("salary-v1", "salary", period, "Salário", 900000, null, 5))
        },
    )

    private fun show(state: MonthlyAnalysisUiState) {
        compose.setContent { DashboardTheme { DashboardScreen(state) } }
    }

    private fun showSuccess(value: MonthlyAnalysis = analysis()) = show(MonthlyAnalysisUiState.Success(value.period, value))

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    @Test fun loadingShowsProgressAndSelectedPeriod() {
        show(MonthlyAnalysisUiState.Loading(period))
        compose.onNodeWithText("Setembro de 2026").assertIsDisplayed()
        compose.onNodeWithText("Carregando sua análise…").assertIsDisplayed()
        compose.onNode(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate,
        )).assertIsDisplayed()
        compose.onNodeWithText("Renda").assertDoesNotExist()
    }

    @Test fun normalSuccessShowsPeriodAndAllFourDomainTotals() {
        showSuccess()
        compose.onNodeWithText("Setembro de 2026").assertIsDisplayed()
        compose.onNode(hasText("Renda") and hasText("R$ 10.000,00")).assertExists()
        compose.onNode(hasText("Despesas registradas") and hasText("R$ 250,00")).assertExists()
        compose.onNode(hasText("Previsão de despesas do mês") and hasText("R$ 1.750,00")).assertExists()
        scrollTo("Sobra prevista da renda")
        compose.onNode(hasText("Sobra prevista da renda") and hasText("R$ 8.250,00")).assertIsDisplayed()
    }

    @Test fun incomeShowsEnginePercentage() {
        showSuccess()
        scrollTo("A previsão de despesas representa 17,5% da sua renda neste mês.")
    }

    @Test fun overIncomePercentageAndNegativeRemainingAreNotHidden() {
        showSuccess(analysis().copy(expensePercentageOfIncome = BigDecimal("150"), forecastRemainingCents = -500000))
        scrollTo("Sobra prevista da renda")
        compose.onNodeWithText("-R$ 5.000,00").assertExists()
        scrollTo("As despesas previstas ultrapassam a renda deste mês.")
        scrollTo("A previsão de despesas representa 150% da sua renda neste mês.")
    }

    @Test fun noIncomeShowsExplanationWithoutPercentageOrNegativeRemaining() {
        showSuccess(analysis(withIncome = false))
        scrollTo("Sobra prevista da renda")
        compose.onNode(hasText("Sobra prevista da renda") and hasText("Não disponível")).assertExists()
        compose.onNodeWithText("-R$ 1.750,00").assertDoesNotExist()
        scrollTo("Nenhuma renda informada neste mês.")
        compose.onAllNodes(hasText("%", substring = true)).assertCountEquals(0)
    }

    @Test fun emptyMonthShowsGuidanceWithoutArtificialZeroCards() {
        showSuccess(MonthlyAnalysisEngine().analyze(period))
        compose.onNodeWithText("Nenhuma informação neste período.").assertIsDisplayed()
        compose.onNodeWithText("Adicione uma renda ou gasto para começar a análise deste mês.").assertIsDisplayed()
        listOf("Renda", "Despesas registradas", "Previsão de despesas do mês", "Sobra prevista da renda", "R$ 0,00")
            .forEach { compose.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test fun incomeSourcesShowOriginAndAmount() {
        showSuccess()
        scrollTo("Trabalho extra")
        compose.onNode(hasText("Trabalho extra") and hasText("Pontual") and hasText("R$ 1.000,00")).assertExists()
        scrollTo("Salário")
        compose.onNode(hasText("Salário") and hasText("Mensal") and hasText("R$ 9.000,00")).assertExists()
    }

    @Test fun categoryDistributionAndItemCountsComeFromAnalysis() {
        // Distinct supplied totals prove that the UI does not sum expense items again.
        showSuccess(analysis().copy(expensesByCategory = mapOf(
            ExpenseCategory.FOOD to 32100, ExpenseCategory.HOUSING to 150000, ExpenseCategory.LEISURE to 0,
        )))
        scrollTo("Alimentação")
        compose.onNode(hasText("Alimentação") and hasText("2 gastos") and hasText("R$ 321,00")).assertExists()
        scrollTo("Moradia")
        compose.onNode(hasText("Moradia") and hasText("1 gasto") and hasText("R$ 1.500,00")).assertExists()
        listOf("Transporte", "Lazer", "Saúde", "Outros").forEach { compose.onNodeWithText(it).assertDoesNotExist() }
    }

    @Test fun incomeOnlyMonthShowsNoExpensesMessage() {
        showSuccess(MonthlyAnalysisEngine().analyze(period, movements = listOf(
            Movement("income", MovementType.INCOME, "Renda pontual", 10000, null, period.atDay(1)),
        )))
        scrollTo("Nenhuma despesa prevista neste mês.")
    }

    @Test fun errorIsFriendlyAndDoesNotExposeTechnicalCause() {
        show(MonthlyAnalysisUiState.Error(period, IllegalStateException("SQLite secret stack trace")))
        compose.onNodeWithText("Não foi possível carregar a análise deste período.").assertIsDisplayed()
        compose.onNodeWithText("SQLite", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Renda").assertDoesNotExist()
    }

    @Test fun loadingForNewMonthRemovesPreviousAnalysis() {
        val state = mutableStateOf<MonthlyAnalysisUiState>(MonthlyAnalysisUiState.Success(period, analysis()))
        compose.setContent { DashboardTheme { DashboardScreen(state.value) } }
        compose.onNodeWithText("Renda").assertExists()
        compose.runOnIdle { state.value = MonthlyAnalysisUiState.Loading(period.plusMonths(1)) }
        compose.onNodeWithText("Outubro de 2026").assertIsDisplayed()
        compose.onNodeWithText("Setembro de 2026").assertDoesNotExist()
        compose.onNodeWithText("Renda").assertDoesNotExist()
    }

    @Test fun largeFontCanScrollThroughSummaryAndCategories() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period, analysis())) }
            }
        }
        scrollTo("Sobra prevista da renda")
        scrollTo("Moradia")
    }
}
