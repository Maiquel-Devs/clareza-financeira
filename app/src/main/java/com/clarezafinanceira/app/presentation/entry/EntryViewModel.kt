package com.clarezafinanceira.app.presentation.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EntryState(
    val type: MovementType = MovementType.EXPENSE,
    val name: String = "",
    val category: ExpenseCategory? = null,
    val amount: String = "",
    val monthly: Boolean = false,
    val date: LocalDate,
    val startMonth: String = String.format(java.util.Locale.ROOT, "%02d/%04d", date.monthValue, date.year),
    val habitualDay: String = date.dayOfMonth.toString(),
    val editing: Boolean = false,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val loadFailed: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class EntryViewModel(
    private val store: FinancialEntryStore,
    clock: Clock,
    private val movementId: String? = null,
) : ViewModel() {
    private val mutable = MutableStateFlow(EntryState(date = LocalDate.now(clock),
        editing = movementId != null, loading = movementId != null))
    val state = mutable.asStateFlow()

    init {
        if (movementId != null) viewModelScope.launch {
            try {
                val movement = requireNotNull(store.findMovement(movementId))
                mutable.value = mutable.value.copy(type = movement.type, name = movement.name,
                    category = movement.category, amount = MoneyInput.format(movement.amountCents),
                    date = movement.date, loading = false)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false, loadFailed = true,
                    error = "Não foi possível abrir esta movimentação. Volte e tente novamente.")
            }
        }
    }

    fun change(update: (EntryState) -> EntryState) {
        val old = mutable.value
        if (old.saving || old.saved || old.loading || old.loadFailed) return
        val next = update(old)
        mutable.value = next.copy(
            type = if (old.editing) old.type else next.type,
            monthly = !old.editing && next.monthly,
            errors = emptyMap(), error = null,
        )
    }

    fun save() {
        val value = mutable.value
        if (value.saving || value.saved || value.loading || value.loadFailed) return
        val money = MoneyInput.parse(value.amount)
        val period = parseMonth(value.startMonth)
        val day = value.habitualDay.toIntOrNull()
        val errors = buildMap {
            if (value.name.isBlank()) put("name", "Informe um nome.")
            if (value.type == MovementType.EXPENSE && value.category == null)
                put("category", "Escolha uma categoria.")
            money.error?.let { put("amount", it) }
            if (value.monthly && period == null) put("month", "Informe o mês e o ano, como 09/2026.")
            if (value.monthly && (day == null || day !in 1..31)) put("day", "Informe um dia de 1 a 31.")
        }
        if (errors.isNotEmpty()) { mutable.value = value.copy(errors = errors); return }
        mutable.value = value.copy(saving = true, error = null)
        viewModelScope.launch {
            try {
                store.save(EntryInput(value.type, value.name.trim(), requireNotNull(money.cents),
                    if (value.type == MovementType.EXPENSE) value.category else null,
                    value.date, value.monthly, period ?: YearMonth.from(value.date), day ?: 1), movementId)
                mutable.value = mutable.value.copy(saving = false, saved = true)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(saving = false,
                    error = "Não foi possível salvar. Seus dados continuam aqui. Tente novamente.")
            }
        }
    }

    companion object {
        fun parseMonth(text: String): YearMonth? = try {
            if (!Regex("[0-9]{2}/[0-9]{4}").matches(text)) null else {
                val month = YearMonth.of(text.substring(3).toInt(), text.take(2).toInt())
                month.takeIf { it.year in 1..9999 }
            }
        } catch (_: Exception) { null }
    }
}
