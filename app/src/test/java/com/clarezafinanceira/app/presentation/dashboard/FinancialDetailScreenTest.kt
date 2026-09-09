package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.clarezafinanceira.app.domain.*
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import com.clarezafinanceira.app.presentation.detail.*
import java.time.YearMonth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FinancialDetailScreenTest {
    @get:Rule val compose = createComposeRule()
    private val period = YearMonth.of(2026,8)
    private fun item(type: MovementType = MovementType.EXPENSE, origin: ItemOrigin = ItemOrigin.MOVEMENT) =
        AnalysisItem("id",origin,type,"Nome efetivo",170000,if(type == MovementType.EXPENSE) ExpenseCategory.HOUSING else null,
            date = if(origin == ItemOrigin.MOVEMENT) period.atDay(7) else null,
            habitualDay = if(origin == ItemOrigin.RECURRENCE) 9 else null)
    private fun show(item: AnalysisItem?, deletion: DeletionState = DeletionState.IDLE,
        edit: () -> Unit = {}, delete: () -> Unit = {}, cancel: () -> Unit = {}, confirm: () -> Unit = {}) {
        compose.setContent { DashboardTheme { FinancialDetailScreen(FinancialDetailUiState(
            FinancialItemReference(item?.origin ?: ItemOrigin.MOVEMENT,"id",period),item,false),deletion,{},edit,delete,cancel,confirm) } }
    }
    private fun visible(text: String) {
        compose.onNodeWithTag("financial-detail").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }
    @Test fun punctualExpenseShowsCategoryDateAndEffectiveValue() {
        show(item())
        compose.onNodeWithText("Detalhes da despesa").assertExists()
        compose.onNodeWithText("R$ 1.700,00").assertExists()
        visible("Moradia"); visible("Pontual"); visible("7 de agosto de 2026"); visible("Agosto de 2026")
    }
    @Test fun punctualIncomeShowsDateWithoutCategory() {
        show(item(MovementType.INCOME))
        compose.onNodeWithText("Detalhes da renda").assertExists()
        visible("7 de agosto de 2026"); visible("Agosto de 2026")
        compose.onNodeWithText("Categoria").assertDoesNotExist()
    }
    @Test fun recurringExpenseShowsDayAndHistoricalPeriod() {
        show(item(origin = ItemOrigin.RECURRENCE))
        visible("Moradia"); visible("Mensal"); visible("9"); visible("Agosto de 2026")
        compose.onNodeWithText("Data").assertDoesNotExist()
    }
    @Test fun recurringIncomeShowsDayWithoutCategoryOrDuplicateActions() {
        show(item(MovementType.INCOME,ItemOrigin.RECURRENCE))
        visible("9"); visible("Editar")
        compose.onNodeWithText("Categoria").assertDoesNotExist()
        compose.onNodeWithText("Excluir movimentação").assertDoesNotExist()
        compose.onNodeWithText("Parar recorrência").assertDoesNotExist()
        compose.onNodeWithText("Excluir recorrência").assertDoesNotExist()
    }
    @Test fun editIsAccessible() {
        var edited = false
        show(item(),edit = { edited = true }); visible("Editar")
        compose.onNodeWithText("Editar").performClick(); assertTrue(edited)
    }
    @Test fun deleteActionRequestsConfirmation() {
        var requested = false
        show(item(),delete = { requested = true }); visible("Excluir movimentação")
        compose.onNodeWithText("Excluir movimentação").performClick(); assertTrue(requested)
    }
    @Test fun confirmationNamesHistoricalMonthAndCanCancel() {
        var cancelled = false
        show(item(),DeletionState.CONFIRMING,cancel = { cancelled = true })
        compose.onNodeWithText("Esta movimentação será removida de agosto de 2026.").assertIsDisplayed()
        compose.onNodeWithText("Cancelar").performClick(); assertTrue(cancelled)
    }
    @Test fun confirmationInvokesDelete() {
        var deleted = false
        show(item(),DeletionState.CONFIRMING,confirm = { deleted = true })
        compose.onNodeWithText("Excluir").performClick(); assertTrue(deleted)
    }
    @Test fun unavailableItemShowsBackAndNoEdit() {
        show(null)
        compose.onNodeWithText("Esta movimentação não está disponível neste período.").assertIsDisplayed()
        compose.onNodeWithText("Voltar").assertIsDisplayed()
        compose.onNodeWithText("Editar").assertDoesNotExist()
    }
    private fun category(count: Int) {
        compose.setContent { DashboardTheme { CategoryScreen(CategoryUiState(period,ExpenseCategory.HOUSING,
            CategoryDetail(period,ExpenseCategory.HOUSING,count*170000L,(1..count).map { item().copy(sourceId="$it") })),{}, {}) } }
    }
    @Test fun categoryUsesSingularCount() { category(1); compose.onNodeWithText("1 gasto").assertExists() }
    @Test fun categoryUsesPluralCountAndTotal() {
        category(2); compose.onNodeWithText("2 gastos").assertExists(); compose.onNodeWithText("R$ 3.400,00").assertExists()
    }
    @Test fun dashboardCategoryClickKeepsPeriodAndRemovesTemporaryEditing() {
        var selected: Pair<ExpenseCategory,YearMonth>? = null
        val analysis = MonthlyAnalysisEngine().analyze(period,movements = listOf(
            Movement("m",MovementType.EXPENSE,"Gasto",10000,ExpenseCategory.HOUSING,period.atDay(1))))
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,analysis),
            onCategory = { category, month -> selected = category to month }) } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Moradia"))
        compose.onNodeWithText("Moradia").assertHasClickAction().performClick()
        assertEquals(ExpenseCategory.HOUSING to period,selected)
        compose.onNodeWithText("Editar movimentações").assertDoesNotExist()
    }
    @Test fun dashboardIncomeClickSendsStableReferenceDirectly() {
        var selected: FinancialItemReference? = null
        val analysis = MonthlyAnalysisEngine().analyze(period,movements = listOf(
            Movement("salary-id",MovementType.INCOME,"Salário",10000,null,period.atDay(1))))
        compose.setContent { DashboardTheme { DashboardScreen(MonthlyAnalysisUiState.Success(period,analysis),onItem = { selected = it }) } }
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Salário"))
        compose.onNodeWithText("Salário").assertHasClickAction().performClick()
        assertEquals(FinancialItemReference(ItemOrigin.MOVEMENT,"salary-id",period),selected)
    }
}
