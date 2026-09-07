package com.clarezafinanceira.app.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun YearMonth.portuguesePeriod(): String = format(DateTimeFormatter.ofPattern("MMMM 'de' uuuu", Locale.forLanguageTag("pt-BR")))

enum class RecurrenceDialog { SCOPE, STOP, DELETE }

class RecurrenceViewModel(private val store: RecurrenceStore, private val id: String,
    val period: YearMonth) : ViewModel() {
    private val mutable = MutableStateFlow(EntryState(date = period.atDay(1), editing = true,
        monthly = true, loading = true))
    val state = mutable.asStateFlow()
    private val mutableDialog = MutableStateFlow<RecurrenceDialog?>(null)
    val dialog = mutableDialog.asStateFlow()
    private var original: AnalysisItem? = null
    private var pending = RecurrenceChanges()
    init {
        viewModelScope.launch {
            try {
                val item = requireNotNull(store.findRecurrence(id, period))
                original = item
                mutable.value = mutable.value.copy(type = item.type, name = item.name,
                    category = item.category, amount = MoneyInput.format(item.amountCents),
                    habitualDay = item.habitualDay.toString(), loading = false)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false, loadFailed = true,
                    error = "Não foi possível abrir esta recorrência. Volte e tente novamente.")
            }
        }
    }
    private fun available() = state.value.let { !it.loading && !it.loadFailed && !it.saving && !it.saved }
    fun change(update: (EntryState) -> EntryState) {
        if (!available() || dialog.value != null) return
        val old = state.value
        mutable.value = update(old).copy(type = old.type, monthly = true, editing = true,
            errors = emptyMap(), error = null)
    }
    fun save() {
        if (!available() || dialog.value != null) return
        val value = state.value
        val amount = MoneyInput.parse(value.amount)
        val day = value.habitualDay.toIntOrNull()
        val errors = buildMap {
            if (value.name.isBlank()) put("name", "Informe um nome.")
            amount.error?.let { put("amount", it) }
            if (day == null || day !in 1..31) put("day", "Informe um dia de 1 a 31.")
            if (value.type == MovementType.EXPENSE && value.category == null) put("category", "Escolha uma categoria.")
        }
        if (errors.isNotEmpty()) { mutable.value = value.copy(errors = errors); return }
        val old = requireNotNull(original)
        pending = RecurrenceChanges(value.name.trim().takeIf { it != old.name },
            amount.cents?.takeIf { it != old.amountCents },
            value.category?.takeIf { value.type == MovementType.EXPENSE && it != old.category },
            day?.takeIf { it != old.habitualDay })
        if (pending.isEmpty) mutable.value = value.copy(saved = true)
        else mutableDialog.value = RecurrenceDialog.SCOPE
    }
    fun requestStop() { if (available() && dialog.value == null) mutableDialog.value = RecurrenceDialog.STOP }
    fun requestDelete() { if (available() && dialog.value == null) mutableDialog.value = RecurrenceDialog.DELETE }
    fun dismiss() { if (!state.value.saving) mutableDialog.value = null }
    fun applyScope(permanent: Boolean) = execute(RecurrenceDialog.SCOPE) {
        if (permanent) store.updateRecurrenceFromPeriod(id, period, pending)
        else store.updateRecurrenceForPeriod(id, period, pending)
    }
    fun stop(keepPeriod: Boolean) = execute(RecurrenceDialog.STOP) { store.stopRecurrence(id, period, keepPeriod) }
    fun delete() = execute(RecurrenceDialog.DELETE) { store.deleteRecurrence(id) }
    private fun execute(expected: RecurrenceDialog, action: suspend () -> Unit) {
        if (!available() || dialog.value != expected) return
        mutableDialog.value = null
        mutable.value = state.value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                action()
                mutable.value = state.value.copy(saving = false, saved = true)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutable.value = state.value.copy(saving = false,
                    error = "Não foi possível concluir a alteração. Seus dados continuam aqui. Tente novamente.")
            }
        }
    }
}
