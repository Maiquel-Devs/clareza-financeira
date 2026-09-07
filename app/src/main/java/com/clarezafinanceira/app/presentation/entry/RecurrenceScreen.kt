package com.clarezafinanceira.app.presentation.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.YearMonth

@Composable
fun RecurrenceRoute(viewModel: RecurrenceViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    BackHandler(enabled = state.saving) { }
    EntryScreen(state, viewModel::change, viewModel::save, onBack, viewModel.period,
        viewModel::requestStop, viewModel::requestDelete)
    RecurrenceDialogContent(dialog, viewModel.period, viewModel::dismiss,
        viewModel::applyScope, viewModel::stop, viewModel::delete)
}

@Composable
fun RecurrenceDialogContent(dialog: RecurrenceDialog?, period: YearMonth, onDismiss: () -> Unit,
    onScope: (Boolean) -> Unit, onStop: (Boolean) -> Unit, onDelete: () -> Unit) {
    if (dialog == null) return
    val month = period.portuguesePeriod()
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(when (dialog) {
            RecurrenceDialog.SCOPE -> "Como deseja aplicar esta alteração?"
            RecurrenceDialog.STOP -> "Parar recorrência?"
            RecurrenceDialog.DELETE -> "Excluir recorrência?"
        }) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when (dialog) {
                    RecurrenceDialog.SCOPE -> {
                        TextButton(onClick = { onScope(false) }) { Text("Somente em $month") }
                        TextButton(onClick = { onScope(true) }) { Text("A partir de $month") }
                        Text("Alterações já programadas para períodos posteriores serão preservadas.")
                    }
                    RecurrenceDialog.STOP -> {
                        Text("Parar preserva o histórico anterior a $month.")
                        TextButton(onClick = { onStop(true) }) {
                            Text("Manter em $month e parar a partir de ${period.plusMonths(1).portuguesePeriod()}")
                        }
                        TextButton(onClick = { onStop(false) }) { Text("Remover de $month e parar a partir de $month") }
                    }
                    RecurrenceDialog.DELETE -> Text("Esta recorrência será removida de todos os períodos, inclusive dos meses anteriores em que fazia parte da sua análise. Esta ação não pode ser desfeita.")
                }
            }
        }, confirmButton = {
            if (dialog == RecurrenceDialog.DELETE) TextButton(onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Excluir de todos os períodos") }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } })
}
