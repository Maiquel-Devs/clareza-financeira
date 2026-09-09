package com.clarezafinanceira.app.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarezafinanceira.app.R
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.dashboard.*

@Composable
fun CategoryRoute(viewModel: CategoryViewModel, onBack: () -> Unit, onItem: (FinancialItemReference) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoryScreen(state, onBack, onItem)
}

@Composable
fun CategoryScreen(state: CategoryUiState, onBack: () -> Unit, onItem: (FinancialItemReference) -> Unit) {
    DetailLayout("category-list") {
        item("header") {
            TextButton(onClick = onBack) { Text("Voltar") }
            Text(stringResource(state.category.labelResource()), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() })
            Text(DashboardFormatting.period(state.period), style = MaterialTheme.typography.titleMedium)
        }
        val detail = state.detail
        when {
            state.failed -> item { Text("Não foi possível carregar os gastos.") }
            detail == null -> item { CircularProgressIndicator() }
            else -> {
                item("summary") {
                    Text(pluralStringResource(R.plurals.dashboard_expense_count, detail.items.size, detail.items.size))
                    Text(DashboardFormatting.money(detail.totalCents), style = MaterialTheme.typography.headlineSmall)
                    if (detail.items.isEmpty()) Text("Nenhum gasto nesta categoria neste período.")
                }
                items(detail.items, key = { "${it.origin}-${it.sourceId}" }) { item ->
                    DetailCard(item.name, if (item.origin == ItemOrigin.RECURRENCE) "Mensal"
                        else item.date?.let(DashboardFormatting::date) ?: "Pontual", item.amountCents,
                        onClick = { onItem(FinancialItemReference(item.origin, item.sourceId, state.period)) })
                }
            }
        }
    }
}

@Composable
fun FinancialDetailRoute(viewModel: FinancialDetailViewModel, onBack: () -> Unit,
    onEdit: (FinancialItemReference) -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    LaunchedEffect(deletion) { if (deletion == DeletionState.DELETED) onBack() }
    FinancialDetailScreen(state, deletion, onBack, { onEdit(state.reference) },
        viewModel::requestDeletion, viewModel::cancelDeletion, viewModel::confirmDeletion)
}

@Composable
fun FinancialDetailScreen(state: FinancialDetailUiState, deletion: DeletionState,
    onBack: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit,
    onCancelDelete: () -> Unit, onConfirmDelete: () -> Unit) {
    DetailLayout("financial-detail") {
        item("back") { TextButton(onClick = onBack, enabled = deletion != DeletionState.DELETING) { Text("Voltar") } }
        val item = state.item
        when {
            state.loading -> item { CircularProgressIndicator() }
            state.failed -> item { Text("Não foi possível carregar a movimentação.") }
            item == null -> item { Text("Esta movimentação não está disponível neste período.") }
            else -> {
                item("title") {
                    Text(if (item.type == MovementType.INCOME) "Detalhes da renda" else "Detalhes da despesa",
                        style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                }
                item("name") { Text(item.name, style = MaterialTheme.typography.titleLarge) }
                item("amount") { Text(DashboardFormatting.money(item.amountCents), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary) }
                if (item.type == MovementType.EXPENSE) item("category") {
                    DetailField("Categoria", item.category?.let { stringResource(it.labelResource()) } ?: "Não informada")
                }
                item("type") { DetailField("Tipo", if (item.origin == ItemOrigin.RECURRENCE) "Mensal" else "Pontual") }
                if (item.origin == ItemOrigin.RECURRENCE) {
                    item.habitualDay?.let { day -> item("day") { DetailField("Dia habitual", day.toString()) } }
                } else item.date?.let { date -> item("date") { DetailField("Data", DashboardFormatting.date(date)) } }
                item("period") { DetailField("Período visualizado", DashboardFormatting.period(state.reference.period)) }
                item("edit") { Button(onClick = onEdit, enabled = deletion != DeletionState.DELETING,
                    modifier = Modifier.fillMaxWidth()) { Text("Editar") } }
                if (item.origin == ItemOrigin.MOVEMENT) item("delete") {
                    TextButton(onClick = onDelete, enabled = deletion != DeletionState.DELETING,
                        modifier = Modifier.fillMaxWidth()) { Text("Excluir movimentação", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (deletion == DeletionState.FAILED) item { Text("Não foi possível excluir. Tente novamente.", color = MaterialTheme.colorScheme.error) }
        if (deletion == DeletionState.DELETING) item { CircularProgressIndicator() }
    }
    if (deletion == DeletionState.CONFIRMING) AlertDialog(
        onDismissRequest = onCancelDelete,
        title = { Text("Excluir movimentação?") },
        text = { Text("Esta movimentação será removida de ${DashboardFormatting.period(state.reference.period).replaceFirstChar { it.lowercase() }}.") },
        dismissButton = { TextButton(onClick = onCancelDelete) { Text("Cancelar") } },
        confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Excluir") } },
    )
}

@Composable
private fun DetailField(label: String, value: String) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun DetailLayout(tag: String, content: LazyListScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
            // Saved by the navigation entry; every new entry starts with its own state at the top.
            LazyColumn(state = rememberLazyListState(), modifier = Modifier.widthIn(max = 600.dp).fillMaxSize().testTag(tag),
                contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}
