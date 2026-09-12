package com.clarezafinanceira.app.presentation.entry

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.clarezafinanceira.app.presentation.dashboard.DashboardTheme
import java.time.LocalDate
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntryScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun show(initial: EntryState = EntryState(date = LocalDate.of(2026, 9, 7))) {
        compose.setContent {
            var state by remember { mutableStateOf(initial) }
            DashboardTheme { EntryScreen(state, { state = it(state) }, {}, {}) }
        }
    }
    @Test fun monthlyToggleShowsOnlyRelevantFields() {
        show()
        compose.onNodeWithText("Data").assertExists()
        compose.onNodeWithText("Mês de início (MM/AAAA)").assertDoesNotExist()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Data").assertDoesNotExist()
        compose.onNodeWithText("Mês de início (MM/AAAA)").assertExists()
        compose.onNodeWithText("Dia habitual (1 a 31)").assertExists()
        compose.onNode(isToggleable()).performScrollTo().performClick()
        compose.onNodeWithText("Data").assertExists()
        compose.onNodeWithText("Dia habitual (1 a 31)").assertDoesNotExist()
    }
    @Test fun incomeHasNoCategoryAndClearTitle() {
        show(); compose.onNodeWithText("Renda").performClick()
        compose.onNodeWithText("Nova renda").assertExists()
        compose.onNodeWithText("Origem / nome da renda").assertExists()
        compose.onNodeWithText("Categoria").assertDoesNotExist()
    }
    @Test fun categoryCanBeChosen() {
        show(); compose.onNodeWithText("Escolher categoria").performClick()
        compose.onNodeWithText("Alimentação").performClick()
        compose.onNodeWithText("Alimentação").assertExists()
    }
    @Test fun saveRemainsReachableOnNarrowScreen() {
        show(); compose.onNodeWithText("Salvar").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }
    @Test fun errorsAreVisibleAndSavingDisabled() {
        show(EntryState(date = LocalDate.of(2026, 9, 7), saving = true,
            errors = mapOf("name" to "Informe um nome.")))
        compose.onNodeWithText("Informe um nome.").assertExists()
        compose.onNodeWithText("Salvando…").performScrollTo().assertIsNotEnabled()
    }
    @Test fun editDoesNotOfferConversionToMonthly() {
        show(EntryState(date = LocalDate.of(2026, 9, 7), editing = true))
        compose.onNodeWithText("Editar despesa").assertExists()
        compose.onNodeWithText("Acontece todo mês?").assertDoesNotExist()
    }
}
