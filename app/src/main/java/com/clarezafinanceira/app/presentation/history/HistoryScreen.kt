package com.clarezafinanceira.app.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clipToBounds
import com.clarezafinanceira.app.presentation.charts.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarezafinanceira.app.presentation.dashboard.DashboardFormatting
import java.time.YearMonth

@Composable
fun HistoryRoute(viewModel: HistoryViewModel, onBack: () -> Unit, onPeriod: (YearMonth) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(state, viewModel::selectYear, onBack, onPeriod)
}

@Composable
fun HistoryScreen(state: HistoryUiState, onYear: (Int) -> Unit, onBack: () -> Unit,
    onPeriod: (YearMonth) -> Unit) {
    var chartExpanded by rememberSaveable { mutableStateOf(false) }
    val chartRows = if (chartExpanded) remember(state.items) { ChartData.year(state.items) } else emptyList()
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().widthIn(max = 600.dp).padding(horizontal = 20.dp)) {
            TextButton(onClick = onBack) { Text("← Voltar") }
            Text("Histórico", style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() })
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onYear(state.year - 1) }, enabled = state.year > 1,
                    modifier = Modifier.semantics { contentDescription = "Ano anterior" }) { Text("‹") }
                Text(state.year.toString(), style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onYear(state.year + 1) }, enabled = state.year < state.currentYear,
                    modifier = Modifier.semantics { contentDescription = "Próximo ano" }) { Text("›") }
            }
            // NavHost saves this saveable LazyListState on forward navigation. A new year starts at top.
            key(state.year) {
                val scroll = rememberLazyListState()
                LazyColumn(state = scroll, modifier = Modifier.weight(1f).clipToBounds().testTag("history-months"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!state.loading && !state.failed && state.items.isNotEmpty())
                        yearChart(chartRows, chartExpanded, { chartExpanded = !chartExpanded }, state.year)
                    when {
                        state.loading -> item { CircularProgressIndicator() }
                        state.failed -> item { Text("Não foi possível carregar o histórico.") }
                        state.items.isEmpty() -> item { Text("Nenhuma informação financeira em ${state.year}.") }
                        else -> items(state.items, key = { it.period.toString() }) { month ->
                            Surface(onClick = { onPeriod(month.period) }, shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(DashboardFormatting.period(month.period), style = MaterialTheme.typography.titleLarge)
                                    if (month.isCurrentPeriod) Text("ATUAL", color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.labelSmall)
                                    Text("Renda", style = MaterialTheme.typography.labelLarge)
                                    Text(if (month.incomeCents == 0L) "Nenhuma renda informada" else DashboardFormatting.money(month.incomeCents))
                                    Text("Previsão de despesas", style = MaterialTheme.typography.labelLarge)
                                    Text(DashboardFormatting.money(month.forecastExpensesCents))
                                    Text("Sobra prevista", style = MaterialTheme.typography.labelLarge)
                                    Text(month.forecastLeftoverCents?.let(DashboardFormatting::money) ?: "Não disponível")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
