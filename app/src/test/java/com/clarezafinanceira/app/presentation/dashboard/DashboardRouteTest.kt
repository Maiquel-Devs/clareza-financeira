package com.clarezafinanceira.app.presentation.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.clarezafinanceira.app.domain.MonthlyAnalysis
import com.clarezafinanceira.app.domain.MonthlyAnalysisEngine
import com.clarezafinanceira.app.domain.MonthlyAnalysisSource
import com.clarezafinanceira.app.domain.Movement
import com.clarezafinanceira.app.domain.MovementType
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModel
import com.clarezafinanceira.app.presentation.MonthlyAnalysisViewModelFactory
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "pt-rBR-w411dp-h891dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardRouteTest {
    @get:Rule val compose = createComposeRule()
    private val store = ViewModelStore()

    @After fun clearViewModel() { compose.runOnUiThread { store.clear() } }

    @Test fun routeRendersReactiveViewModelUpdatesAndPeriodChanges() {
        val period = YearMonth.of(2026, 9)
        val nextPeriod = period.plusMonths(1)
        val currentFlow = MutableSharedFlow<MonthlyAnalysis>(replay = 1)
        val nextFlow = MutableSharedFlow<MonthlyAnalysis>(replay = 1)
        val source = MonthlyAnalysisSource { if (it == period) currentFlow else nextFlow }
        lateinit var vm: MonthlyAnalysisViewModel
        compose.runOnUiThread {
            vm = ViewModelProvider.create(store, MonthlyAnalysisViewModelFactory(
                source, Clock.fixed(Instant.parse("2026-09-10T12:00:00Z"), ZoneOffset.UTC),
            ))[MonthlyAnalysisViewModel::class.java]
        }
        compose.setContent { DashboardTheme { DashboardRoute(vm) } }
        compose.onNodeWithText("Carregando sua análise…").assertIsDisplayed()

        val engine = MonthlyAnalysisEngine()
        compose.runOnIdle { currentFlow.tryEmit(engine.analyze(period)) }
        compose.onNodeWithText("Nenhuma informação neste período.").assertIsDisplayed()

        compose.runOnIdle {
            currentFlow.tryEmit(engine.analyze(period, movements = listOf(
                Movement("income", MovementType.INCOME, "Pagamento recebido", 123400, null, period.atDay(1)),
            )))
        }
        compose.onNodeWithText("Renda").assertIsDisplayed()
        compose.onNodeWithText("Nenhuma informação neste período.").assertDoesNotExist()

        compose.runOnIdle { vm.selectPeriod(nextPeriod) }
        compose.onNodeWithText("Outubro de 2026").assertIsDisplayed()
        compose.onNodeWithText("Carregando sua análise…").assertIsDisplayed()
        compose.onNodeWithText("Renda").assertDoesNotExist()

        compose.runOnIdle { nextFlow.tryEmit(engine.analyze(nextPeriod)) }
        compose.onNodeWithText("Nenhuma informação neste período.").assertIsDisplayed()
    }
}
