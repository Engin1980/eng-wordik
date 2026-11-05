package cz.wordik.app.ui

import app.cash.turbine.test
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.domain.TestResult
import cz.wordik.app.testutil.FakeSettingsStore
import cz.wordik.app.testutil.MainDispatcherRule
import cz.wordik.app.testutil.TestData
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val sets = listOf("A", "B")
    private val settings = TestData.defaults
    private val result = TestResult(completed = true, correct = 1, wrong = 0, durationMillis = 1, words = emptyList())

    private fun viewModel() = AppViewModel(FakeSettingsStore())

    @Test
    fun `zacina vyberem sad a nelze z nej zpet`() {
        val vm = viewModel()
        assertEquals(listOf<Screen>(Screen.SetSelection), vm.stack.value)
        assertFalse(vm.back())
        assertEquals(1, vm.stack.value.size)
    }

    @Test
    fun `navigace vyber nastaveni test vysledky`() {
        val vm = viewModel()
        vm.open(Screen.TestSetup(sets))
        vm.startTest(sets, settings)
        assertTrue(vm.current is Screen.Test)
        vm.showResults(sets, settings, result)
        assertEquals(
            listOf(Screen.SetSelection, Screen.TestSetup(sets), Screen.Results(sets, settings, result)),
            vm.stack.value,
        )
    }

    @Test
    fun `zpet z vysledku vede na nastaveni testu`() {
        val vm = viewModel()
        vm.open(Screen.TestSetup(sets))
        vm.startTest(sets, settings)
        vm.showResults(sets, settings, result)
        assertTrue(vm.back())
        assertEquals(Screen.TestSetup(sets), vm.current)
    }

    @Test
    fun `navrat na vyber sad vymaze zasobnik`() {
        val vm = viewModel()
        vm.open(Screen.TestSetup(sets))
        vm.startTest(sets, settings)
        vm.showResults(sets, settings, result)
        vm.backToSelection()
        assertEquals(listOf<Screen>(Screen.SetSelection), vm.stack.value)
    }

    @Test
    fun `opakovani testu dostane novy runId a nahradi vysledky`() {
        val vm = viewModel()
        vm.open(Screen.TestSetup(sets))
        vm.startTest(sets, settings)
        val first = vm.current as Screen.Test
        vm.showResults(sets, settings, result)
        vm.repeatTest(sets, settings)
        val second = vm.current as Screen.Test
        assertEquals(settings, second.settings)
        assertTrue(second.runId != first.runId)
        assertEquals(3, vm.stack.value.size)
    }

    @Test
    fun `nastaveni aplikace se otevre a zavre`() {
        val vm = viewModel()
        vm.open(Screen.AppSettings)
        assertEquals(Screen.AppSettings, vm.current)
        vm.back()
        assertEquals(Screen.SetSelection, vm.current)
    }

    @Test
    fun `velikost se uklada a propisuje`() = runTest {
        val store = FakeSettingsStore()
        val vm = AppViewModel(store)
        vm.sizeLevel.test {
            assertEquals(SizeLevel.NORMAL, awaitItem())
            vm.setSizeLevel(SizeLevel.LARGE)
            mainRule.dispatcher.scheduler.advanceUntilIdle()
            assertEquals(SizeLevel.LARGE, awaitItem())
        }
    }
}
