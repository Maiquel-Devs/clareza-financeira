package com.clarezafinanceira.app.presentation.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarezafinanceira.app.R
import com.clarezafinanceira.app.domain.ExpenseCategory
import com.clarezafinanceira.app.domain.ItemOrigin
import com.clarezafinanceira.app.domain.MonthlyAnalysis
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel

@Composable
fun DashboardRoute(viewModel: MonthlyAnalysisViewModel, onAdd: (() -> Unit)? = null,
    onEdit: ((String) -> Unit)? = null) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(state, onAdd = onAdd, onEdit = onEdit)
}

/** Stateless UI: only domain analysis and explicit loading/error states enter this screen. */
@Composable
fun DashboardScreen(state: MonthlyAnalysisUiState, modifier: Modifier = Modifier,
    onAdd: (() -> Unit)? = null, onEdit: ((String) -> Unit)? = null) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            // A new period starts at the top; its title and content always use the same state.
            key(state.period) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.app_name),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary)
                            Text(
                                DashboardFormatting.period(state.period),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.semantics { heading() },
                            )
                            Text(stringResource(R.string.dashboard_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp))
                        }
                    }
                    when (state) {
                        is MonthlyAnalysisUiState.Loading -> item(key = "loading") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(36.dp))
                                Text(stringResource(R.string.dashboard_loading))
                            }
                        }
                        is MonthlyAnalysisUiState.Error -> item(key = "error") {
                            MessageCard(
                                title = stringResource(R.string.dashboard_error_title),
                                message = stringResource(R.string.dashboard_error_message),
                                background = MaterialTheme.colorScheme.errorContainer,
                                foreground = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        is MonthlyAnalysisUiState.Success -> {
                            if (state.analysis.isEmpty) {
                                item(key = "empty") {
                                    MessageCard(
                                        title = stringResource(R.string.dashboard_empty_title),
                                        message = stringResource(R.string.dashboard_empty_message),
                                    )
                                }
                            } else {
                                analysisContent(state.analysis)
                                if (onEdit != null) {
                                    item { Text("Editar movimentações pontuais", style = MaterialTheme.typography.titleMedium) }
                                    items((state.analysis.incomeSources + state.analysis.expenseItems)
                                        .filter { it.origin == ItemOrigin.MOVEMENT }, key = { "edit-${it.sourceId}" }) { entry ->
                                        androidx.compose.material3.TextButton(onClick = { onEdit(entry.sourceId) },
                                            modifier = Modifier.fillMaxWidth()) {
                                            Text("Editar ${entry.name} · ${DashboardFormatting.money(entry.amountCents)}")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (onAdd != null) androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Text("+ Adicionar") }
        }
    }
}

private fun LazyListScope.analysisContent(analysis: MonthlyAnalysis) {
    item(key = "summary") { Summary(analysis) }
    item(key = "interpretation") {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.dashboard_forecast_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(
                    text = analysis.expensePercentageOfIncome?.let {
                        stringResource(R.string.dashboard_percentage, DashboardFormatting.percentage(it))
                    } ?: stringResource(R.string.dashboard_no_income),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
    item(key = "analysis-title") {
        Text(stringResource(R.string.dashboard_analysis_title),
            Modifier.padding(top = 12.dp, bottom = 4.dp).semantics { heading() },
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
    item(key = "income-title") {
        SectionTitle(R.string.dashboard_income_section, R.string.dashboard_income_description)
    }
    if (analysis.incomeSources.isEmpty()) {
        item(key = "no-income-sources") { Text(stringResource(R.string.dashboard_no_income_sources)) }
    }
    items(analysis.incomeSources, key = { "income-${it.origin}-${it.sourceId}" }) { source ->
        DetailCard(
            title = source.name,
            detail = stringResource(
                if (source.origin == ItemOrigin.RECURRENCE) R.string.dashboard_monthly
                else R.string.dashboard_one_off,
            ),
            amountCents = source.amountCents,
            amountColor = IncomeInk,
        )
    }
    item(key = "expense-title") {
        SectionTitle(R.string.dashboard_expense_section, R.string.dashboard_expense_description)
    }
    val categories = ExpenseCategory.entries.filter { (analysis.expensesByCategory[it] ?: 0L) > 0L }
    if (categories.isEmpty()) {
        item(key = "no-expenses") { Text(stringResource(R.string.dashboard_no_expenses)) }
    }
    items(categories, key = { "category-$it" }) { category ->
        // Count display items only; monetary distribution is already supplied by the engine.
        val count = analysis.expenseItems.count { it.category == category }
        DetailCard(
            title = stringResource(category.labelResource()),
            detail = pluralStringResource(R.plurals.dashboard_expense_count, count, count),
            amountCents = analysis.expensesByCategory.getValue(category),
        )
    }
}

@Composable
private fun Summary(analysis: MonthlyAnalysis) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints {
            val stacked = maxWidth < 340.dp || LocalDensity.current.fontScale > 1.2f
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(R.string.dashboard_income, analysis.consideredIncomeCents, IncomeSurface)
                    SummaryCard(R.string.dashboard_registered, analysis.registeredExpenseCents, RegisteredSurface)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(R.string.dashboard_income, analysis.consideredIncomeCents, IncomeSurface,
                        Modifier.weight(1f))
                    SummaryCard(R.string.dashboard_registered, analysis.registeredExpenseCents, RegisteredSurface,
                        Modifier.weight(1f))
                }
            }
        }
        SummaryCard(R.string.dashboard_forecast, analysis.forecastExpenseCents, ForecastSurface)
        val remaining = analysis.forecastRemainingCents
        SummaryCard(
            R.string.dashboard_remaining, remaining,
            if (remaining != null && remaining < 0) MaterialTheme.colorScheme.errorContainer else IncomeSurface,
            explanation = when {
                remaining == null -> stringResource(R.string.dashboard_remaining_unavailable)
                remaining < 0 -> stringResource(R.string.dashboard_over_income)
                else -> null
            },
        )
    }
}

@Composable
private fun SummaryCard(
    @StringRes label: Int,
    amountCents: Long?,
    background: Color,
    modifier: Modifier = Modifier,
    explanation: String? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        color = background,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
            Text(amountCents?.let(DashboardFormatting::money) ?: stringResource(R.string.dashboard_unavailable),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            explanation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int, @StringRes description: Int) {
    Column(
        Modifier.padding(top = 12.dp, bottom = 4.dp).semantics(mergeDescendants = true) { heading() },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(title), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(description), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DetailCard(
    title: String,
    detail: String,
    amountCents: Long,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        // Vertical flow leaves room for long names, large amounts and Android font scaling.
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(DashboardFormatting.money(amountCents), Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = amountColor)
        }
    }
}

@Composable
private fun MessageCard(
    title: String,
    message: String,
    background: Color = MaterialTheme.colorScheme.surface,
    foreground: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() })
            Text(message, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@StringRes
private fun ExpenseCategory.labelResource(): Int = when (this) {
    ExpenseCategory.FOOD -> R.string.category_food
    ExpenseCategory.HOUSING -> R.string.category_housing
    ExpenseCategory.TRANSPORT -> R.string.category_transport
    ExpenseCategory.LEISURE -> R.string.category_leisure
    ExpenseCategory.HEALTH -> R.string.category_health
    ExpenseCategory.OTHER -> R.string.category_other
}
