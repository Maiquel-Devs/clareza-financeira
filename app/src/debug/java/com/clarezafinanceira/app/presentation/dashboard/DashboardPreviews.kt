package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.clarezafinanceira.app.domain.ExpenseCategory
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import com.clarezafinanceira.app.domain.Movement
import com.clarezafinanceira.app.domain.MovementType
import com.clarezafinanceira.app.domain.Recurrence
import com.clarezafinanceira.app.domain.RecurrenceVersion
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import java.time.YearMonth

// Debug-only fixtures: no repository, database, or persistence writes.
private val previewPeriod = YearMonth.of(2026, 9)

private fun previewAnalysis(withIncome: Boolean = true, manySources: Boolean = false) = MonthlyAnalysisEngine().analyze(
    period = previewPeriod,
    movements = buildList {
        add(Movement("market", MovementType.EXPENSE, "Mercado", 35000, ExpenseCategory.FOOD, previewPeriod.atDay(3)))
        add(Movement("lunch", MovementType.EXPENSE, "Almoço", 5000, ExpenseCategory.FOOD, previewPeriod.atDay(4)))
        add(Movement("bus", MovementType.EXPENSE, "Transporte", 15000, ExpenseCategory.TRANSPORT, previewPeriod.atDay(5)))
        if (withIncome) {
            add(Movement("extra", MovementType.INCOME, "Trabalho extra", 80000, null, previewPeriod.atDay(7)))
            if (manySources) repeat(12) {
                add(Movement("extra-$it", MovementType.INCOME, "Projeto independente ${it + 1}",
                    10000L, null, previewPeriod.atDay(8)))
            }
        }
        if (manySources) {
            add(Movement("leisure", MovementType.EXPENSE, "Cinema", 8000, ExpenseCategory.LEISURE, previewPeriod.atDay(9)))
            add(Movement("health", MovementType.EXPENSE, "Consulta", 25000, ExpenseCategory.HEALTH, previewPeriod.atDay(9)))
            add(Movement("other", MovementType.EXPENSE, "Presente", 10000, ExpenseCategory.OTHER, previewPeriod.atDay(9)))
        }
    },
    recurrences = buildList {
        add(Recurrence("rent", MovementType.EXPENSE, previewPeriod))
        if (withIncome) add(Recurrence("salary", MovementType.INCOME, previewPeriod))
    },
    versions = buildList {
        add(RecurrenceVersion("rent-v1", "rent", previewPeriod, "Aluguel", 180000, ExpenseCategory.HOUSING, 10))
        if (withIncome) add(RecurrenceVersion("salary-v1", "salary", previewPeriod, "Salário", 700000, null, 5))
    },
)

@Preview(name = "Análise mensal", showBackground = true, widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Preview(name = "Tela estreita e fonte ampliada", widthDp = 320, heightDp = 720, fontScale = 1.6f, locale = "pt-rBR")
@Composable
fun NormalDashboardPreview() {
    DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(previewPeriod, previewAnalysis())) }
}

@Preview(name = "Sem renda", widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Composable
fun NoIncomeDashboardPreview() {
    DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(previewPeriod, previewAnalysis(withIncome = false))) }
}

@Preview(name = "Mês vazio", widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Composable
fun EmptyDashboardPreview() {
    DashboardTheme {
        DashboardScreen(MonthlyAnalysisUiState.Success(previewPeriod, MonthlyAnalysisEngine().analyze(previewPeriod)))
    }
}

@Preview(name = "Erro", widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Composable
fun ErrorDashboardPreview() {
    DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Error(previewPeriod, IllegalStateException("Preview"))) }
}

@Preview(name = "Carregando", widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Composable
fun LoadingDashboardPreview() {
    DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Loading(previewPeriod)) }
}

@Preview(name = "Muitas fontes e todas as categorias", widthDp = 411, heightDp = 891, locale = "pt-rBR")
@Composable
fun ManySourcesDashboardPreview() {
    DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(previewPeriod, previewAnalysis(manySources = true))) }
}
