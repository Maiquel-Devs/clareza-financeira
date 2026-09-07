package com.clarezafinanceira.app.presentation.entry

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.clarezafinanceira.app.presentation.dashboard.DashboardTheme
import java.time.YearMonth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RecurrenceScreenTest {
    @get:Rule val compose = createComposeRule()
    private val period = YearMonth.of(2026,9)
    @Test fun existingRecurrenceHasMonthlyTitleAndNoConversionOrStartField() {
        compose.setContent { DashboardTheme {
            EntryScreen(EntryState(date = period.atDay(1),monthly = true,editing = true),{}, {}, {},period)
        } }
        compose.onNodeWithText("Editar despesa mensal").assertExists()
        compose.onNodeWithText("Período: setembro de 2026").assertExists()
        compose.onNodeWithText("Acontece todo mês?").assertDoesNotExist()
        compose.onNodeWithText("Mês de início (MM/AAAA)").assertDoesNotExist()
        compose.onNodeWithText("Salvar alteração").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Parar recorrência").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Excluir recorrência").performScrollTo().assertIsDisplayed()
    }
    @Test fun scopeLabelsAreExplicitAndCancelDismisses() {
        var calls = 0
        compose.setContent { DashboardTheme {
            var dialog by remember { mutableStateOf<RecurrenceDialog?>(RecurrenceDialog.SCOPE) }
            RecurrenceDialogContent(dialog,period,{dialog = null},{calls++},{calls++},{calls++})
        } }
        compose.onNodeWithText("Somente em setembro de 2026").assertExists()
        compose.onNodeWithText("A partir de setembro de 2026").assertExists()
        compose.onNodeWithText("Cancelar").performClick()
        compose.onNodeWithText("Como deseja aplicar esta alteração?").assertDoesNotExist()
        assertEquals(0,calls)
    }
    @Test fun stopExplainsBothPeriodsAndForwardsChoice() {
        var keep: Boolean? = null
        compose.setContent { DashboardTheme { RecurrenceDialogContent(RecurrenceDialog.STOP,period,{}, {},{keep=it},{}) } }
        compose.onNodeWithText("Manter em setembro de 2026 e parar a partir de outubro de 2026").assertExists()
        compose.onNodeWithText("Remover de setembro de 2026 e parar a partir de setembro de 2026").performClick()
        assertEquals(false,keep)
    }
    @Test fun deleteWarnsAboutHistoryAndRequiresStrongAction() {
        var calls = 0
        compose.setContent { DashboardTheme { RecurrenceDialogContent(RecurrenceDialog.DELETE,period,{}, {},{},{calls++}) } }
        compose.onNodeWithText("Esta recorrência será removida de todos os períodos, inclusive dos meses anteriores em que fazia parte da sua análise. Esta ação não pode ser desfeita.").assertExists()
        assertEquals(0,calls)
        compose.onNodeWithText("Excluir de todos os períodos").performClick(); assertEquals(1,calls)
    }
}
