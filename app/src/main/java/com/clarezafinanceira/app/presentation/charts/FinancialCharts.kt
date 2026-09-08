package com.clarezafinanceira.app.presentation.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.clarezafinanceira.app.presentation.dashboard.DashboardFormatting
import com.clarezafinanceira.app.presentation.dashboard.labelResource
import java.time.YearMonth

fun LazyListScope.expenseChart(rows: List<ExpenseBar>, expanded: Boolean, onToggle: () -> Unit, period: YearMonth) {
    item("expense-chart-toggle") { ChartToggle(expanded, "Ver gráfico das despesas", onToggle) }
    if (expanded) {
        item("expense-chart-title") {
            Text("Distribuição da previsão de despesas · ${DashboardFormatting.period(period)}",
                style = MaterialTheme.typography.bodyMedium)
        }
        items(rows, key = { "expense-chart-${it.category}" }) { row ->
            ChartBar(stringResource(row.category.labelResource()), row.amountCents, row.fraction,
                DashboardFormatting.percentage(row.percentage))
        }
    }
}

fun LazyListScope.yearChart(rows: List<YearExpenseBar>, expanded: Boolean, onToggle: () -> Unit, year: Int) {
    item("year-chart-toggle") { ChartToggle(expanded, "Ver evolução do ano", onToggle) }
    if (expanded) {
        item("year-chart-title") {
            Text("Previsão de despesas em $year", style = MaterialTheme.typography.titleMedium)
            if (rows.isEmpty()) Text("A evolução precisa de pelo menos dois meses com informação e alguma despesa prevista.")
        }
        items(rows, key = { "year-chart-${it.period}" }) { row ->
            ChartBar(DashboardFormatting.period(row.period), row.amountCents, row.fraction)
        }
    }
}

@Composable
private fun ChartToggle(expanded: Boolean, label: String, onToggle: () -> Unit) {
    TextButton(onClick = onToggle, modifier = Modifier.fillMaxWidth().semantics {
        stateDescription = if (expanded) "Expandido" else "Recolhido"
    }) { Text(if (expanded) "Ocultar gráfico" else label) }
}

@Composable
private fun ChartBar(label: String, amountCents: Long, fraction: Float, percentage: String? = null) {
    Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(DashboardFormatting.money(amountCents) + (percentage?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium)
            // Decorative geometry; the full label, exact amount and share above are accessible text.
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.primaryContainer).clearAndSetSemantics {}) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
            }
        }
    }
}
