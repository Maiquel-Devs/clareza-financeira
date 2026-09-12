package com.clarezafinanceira.app.presentation.entry

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarezafinanceira.app.domain.ExpenseCategory
import com.clarezafinanceira.app.domain.MovementType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun EntryRoute(viewModel: EntryViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    BackHandler(enabled = state.saving) { /* Await the write before allowing navigation. */ }
    EntryScreen(state, viewModel::change, viewModel::save, onBack)
}

@Composable
fun EntryScreen(state: EntryState, onChange: ((EntryState) -> EntryState) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit,
    recurrencePeriod: java.time.YearMonth? = null,
    onStop: () -> Unit = {}, onDelete: () -> Unit = {}) {
    val context = LocalContext.current
    var choosingCategory by remember { mutableStateOf(false) }
    val enabled = !state.saving && !state.loading && !state.loadFailed && !state.saved
    val income = state.type == MovementType.INCOME
    val title = if (state.editing) { if (income) "Editar renda" else "Editar despesa" }
        else { if (income) "Nova renda" else "Nova despesa" }
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().imePadding().fillMaxSize()
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack, enabled = !state.saving) { Text("Voltar") }
            Text(title + if (recurrencePeriod != null) " mensal" else "", style = MaterialTheme.typography.headlineMedium)
            recurrencePeriod?.let { Text("Período: ${it.portuguesePeriod()}") }
            if (state.loading) CircularProgressIndicator()
            if (!state.editing) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !income, enabled = enabled,
                    onClick = { onChange { it.copy(type = MovementType.EXPENSE) } }, label = { Text("Despesa") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer))
                FilterChip(selected = income, enabled = enabled,
                    onClick = { onChange { it.copy(type = MovementType.INCOME, category = null) } }, label = { Text("Renda") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer))
            }
            EntryField(state.name, if (income) "Origem / nome da renda" else "Descrição",
                state.errors["name"], enabled) { value -> onChange { it.copy(name = value) } }
            if (!income) {
                Column {
                    Text("Categoria", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { choosingCategory = true }, enabled = enabled,
                        modifier = Modifier.fillMaxWidth()) { Text(state.category?.label() ?: "Escolher categoria") }
                    state.errors["category"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            EntryField(state.amount, "Valor (R$)", state.errors["amount"], enabled,
                KeyboardType.Decimal) { value -> onChange { it.copy(amount = value) } }
            if (!state.editing) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .toggleable(value = state.monthly, enabled = enabled, role = Role.Switch,
                    onValueChange = { checked -> onChange { it.copy(monthly = checked) } }),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Acontece todo mês?", Modifier.weight(1f))
                Switch(checked = state.monthly, enabled = enabled, onCheckedChange = null)
            }
            if (state.monthly) {
                if (recurrencePeriod == null) EntryField(state.startMonth, "Mês de início (MM/AAAA)", state.errors["month"], enabled,
                    KeyboardType.Number) { value ->
                        val digits = value.filter { it in '0'..'9' }.take(6)
                        val month = if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits
                        onChange { it.copy(startMonth = month) }
                    }
                if (recurrencePeriod == null) EntryViewModel.parseMonth(state.startMonth)?.let {
                    Text(com.clarezafinanceira.app.presentation.dashboard.DashboardFormatting.period(it),
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
                EntryField(state.habitualDay, "Dia habitual (1 a 31)", state.errors["day"], enabled,
                    KeyboardType.Number) { value -> onChange { it.copy(habitualDay = value) } }
                Text(if (recurrencePeriod == null) "Esse valor será considerado a cada mês. O dia é apenas quando costuma acontecer."
                    else "Ao salvar, você poderá escolher em quais períodos aplicar a alteração. O dia é apenas quando costuma acontecer.",
                    style = MaterialTheme.typography.bodyMedium)
            } else {
                Column {
                    Text("Data", style = MaterialTheme.typography.labelLarge)
                    TextButton(enabled = enabled, onClick = {
                        DatePickerDialog(context, { _, year, month, day ->
                            onChange { it.copy(date = LocalDate.of(year, month + 1, day)) }
                        }, state.date.year, state.date.monthValue - 1, state.date.dayOfMonth).show()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(state.date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = onSave, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(if (state.saving) "Salvando…" else if (recurrencePeriod != null) "Salvar alteração" else "Salvar")
            }
            if (recurrencePeriod != null) {
                OutlinedButton(onClick = onStop, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Parar recorrência") }
                TextButton(onClick = onDelete, enabled = enabled, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Excluir recorrência") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (choosingCategory) AlertDialog(onDismissRequest = { choosingCategory = false },
        title = { Text("Categoria") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ExpenseCategory.entries.forEach { category ->
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        onChange { it.copy(category = category) }; choosingCategory = false
                    }) { Text(category.label()) }
                }
            }
        }, confirmButton = { TextButton(onClick = { choosingCategory = false }) { Text("Voltar") } })
}

@Composable
private fun EntryField(value: String, label: String, error: String?, enabled: Boolean,
    keyboard: KeyboardType = KeyboardType.Text, onValue: (String) -> Unit) {
    TextField(value = value, onValueChange = onValue, label = { Text(label) },
        modifier = Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        isError = error != null, supportingText = error?.let { { Text(it) } },
        colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard))
}

private fun ExpenseCategory.label() = when (this) {
    ExpenseCategory.FOOD -> "Alimentação"
    ExpenseCategory.HOUSING -> "Moradia"
    ExpenseCategory.TRANSPORT -> "Transporte"
    ExpenseCategory.LEISURE -> "Lazer"
    ExpenseCategory.HEALTH -> "Saúde"
    ExpenseCategory.OTHER -> "Outros"
}
