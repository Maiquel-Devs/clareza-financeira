package com.clarezafinanceira.app.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarezafinanceira.app.domain.*
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryUiState(val period: YearMonth, val category: ExpenseCategory,
    val detail: CategoryDetail? = null, val failed: Boolean = false)

class CategoryViewModel(source: FinancialDetailSource, period: YearMonth, category: ExpenseCategory) : ViewModel() {
    val uiState = source.observeCategory(period, category)
        .map { CategoryUiState(period, category, it) }
        .catch { emit(CategoryUiState(period, category, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CategoryUiState(period, category))
}

data class FinancialDetailUiState(val reference: FinancialItemReference,
    val item: AnalysisItem? = null, val loading: Boolean = true, val failed: Boolean = false)

enum class DeletionState { IDLE, CONFIRMING, DELETING, DELETED, FAILED }

class FinancialDetailViewModel(source: FinancialDetailSource,
    private val deletionStore: MovementDeletionStore,
    val reference: FinancialItemReference) : ViewModel() {
    val uiState = source.observeItem(reference)
        .map { FinancialDetailUiState(reference, it, loading = false) }
        .catch { emit(FinancialDetailUiState(reference, loading = false, failed = true)) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, FinancialDetailUiState(reference))
    private val deletion = MutableStateFlow(DeletionState.IDLE)
    val deletionState = deletion.asStateFlow()

    fun requestDeletion() {
        if (reference.origin == ItemOrigin.MOVEMENT && uiState.value.item != null &&
            deletion.value in listOf(DeletionState.IDLE, DeletionState.FAILED))
            deletion.value = DeletionState.CONFIRMING
    }

    fun cancelDeletion() {
        if (deletion.value == DeletionState.CONFIRMING) deletion.value = DeletionState.IDLE
    }

    fun confirmDeletion() {
        if (reference.origin != ItemOrigin.MOVEMENT || deletion.value != DeletionState.CONFIRMING) return
        // Set synchronously before launching: repeated taps cannot issue another deletion.
        deletion.value = DeletionState.DELETING
        viewModelScope.launch {
            try {
                deletionStore.deleteMovement(reference.sourceId)
                deletion.value = DeletionState.DELETED
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                deletion.value = DeletionState.FAILED
            }
        }
    }
}
