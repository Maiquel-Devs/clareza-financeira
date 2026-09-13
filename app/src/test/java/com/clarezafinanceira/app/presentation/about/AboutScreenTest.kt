package com.clarezafinanceira.app.presentation.about

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import com.clarezafinanceira.app.AppContainer
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import com.clarezafinanceira.app.domain.MonthlyAnalysisSource
import com.clarezafinanceira.app.presentation.MonthlyAnalysisUiState
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.dashboard.DashboardScreen
import com.clarezafinanceira.app.presentation.dashboard.DashboardTheme
import com.clarezafinanceira.app.presentation.entry.EntryNavigation
import kotlinx.coroutines.flow.flowOf
import java.time.YearMonth
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.*
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AboutScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dashboardOverflowOpensAboutAndBackRestoresDashboardWithMenuClosed() {
        val container = AppContainer(RuntimeEnvironment.getApplication())
        val vm = MonthlyAnalysisViewModel(MonthlyAnalysisSource { flowOf(MonthlyAnalysisEngine().analyze(it)) },
            container.clock, container.currentPeriod)
        compose.setContent { DashboardTheme { EntryNavigation(container, vm) } }
        compose.onNodeWithText("Sobre").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mais opções").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sobre").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sobre o projeto").assertIsDisplayed()
        compose.onNodeWithText("+ Adicionar").assertDoesNotExist()
        compose.onNodeWithText("Voltar").performClick()
        compose.onNodeWithText("+ Adicionar").assertIsDisplayed()
        compose.onNodeWithText("Sobre").assertDoesNotExist()
        compose.onNodeWithContentDescription("Mais opções").assertIsDisplayed().performClick()
        compose.onNodeWithText("Sobre").assertIsDisplayed()
    }

    @Test fun historicalDashboardDoesNotGainInstitutionalNavigation() {
        compose.setContent { DashboardTheme {
            DashboardScreen(MonthlyAnalysisUiState.Loading(YearMonth.of(2026, 8)), onBack = {}, onAbout = {})
        } }
        compose.onNodeWithContentDescription("Mais opções").assertDoesNotExist()
        compose.onNodeWithText("Voltar").assertIsDisplayed()
    }

    @Test fun essentialContentAndProjectActionRemainReachableWithLargeText() {
        var opened = 0
        var returned = false
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                DashboardTheme { AboutScreen(onBack = { returned = true }, onProjectPage = { opened++; true }) }
            }
        }
        listOf("Clareza Financeira", "Organize sua vida financeira sem complicação.", "Sobre o projeto",
            "O Clareza Financeira foi criado para ajudar pessoas a entenderem sua situação financeira de forma simples, rápida e clara.",
            "Desenvolvedor", "Maiquel", "Projeto open source", "Licença MIT").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Página do projeto").performScrollTo().performClick()
        assertEquals(1, opened)
        compose.onNodeWithText("Voltar").performScrollTo().performClick()
        assertTrue(returned)
    }

    @Test fun projectButtonUsesExternalIntentAndMissingBrowserShowsRecoverableMessage() {
        var unavailable = true
        var launched: Intent? = null
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun startActivity(intent: Intent) {
                if (unavailable) throw ActivityNotFoundException("No browser")
                launched = intent
            }
        }
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                DashboardTheme { AboutRoute(onBack = {}) }
            }
        }
        compose.onNodeWithText("Página do projeto").performScrollTo().performClick()
        val error = "Não foi possível abrir a página do projeto. Verifique se há um navegador disponível e tente novamente."
        compose.onNodeWithText(error).performScrollTo().assertIsDisplayed()
        assertNull(launched)
        unavailable = false
        compose.onNodeWithText("Página do projeto").performScrollTo().performClick()
        compose.onNodeWithText(error).assertDoesNotExist()
        assertEquals(Intent.ACTION_VIEW, launched?.action)
        assertEquals("https://maiquel-devs.github.io/clareza-financeira-landing-page/", launched?.dataString)
    }
}
