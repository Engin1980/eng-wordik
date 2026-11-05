package cz.wordik.app.ui

import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.SettingsError
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.Word
import cz.wordik.app.domain.WordSet
import cz.wordik.app.testutil.FakeSettingsStore
import cz.wordik.app.testutil.MainDispatcherRule
import cz.wordik.app.testutil.TestData
import cz.wordik.app.ui.setup.SetSelectionViewModel
import cz.wordik.app.ui.setup.TestSetupViewModel
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupViewModelsTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun set(name: String, count: Int) = WordSet(name, List(count) { Word("cz$name$it", "en$name$it") })

    private val sets = listOf(set("A", 12), set("B", 3), set("C", 5))

    // --- výběr sad ---

    @Test
    fun `na zacatku neni vybrana zadna sada a nelze pokracovat`() {
        val vm = SetSelectionViewModel(sets)
        assertTrue(vm.selected.value.isEmpty())
        assertFalse(vm.canContinue())
        assertEquals(0, vm.selectedWordCount())
    }

    @Test
    fun `vyber vice sad najednou`() {
        val vm = SetSelectionViewModel(sets)
        vm.toggle("C")
        vm.toggle("A")
        assertTrue(vm.canContinue())
        assertEquals(listOf("A", "C"), vm.selectedNames()) // v pořadí seznamu, ne výběru
        assertEquals(17, vm.selectedWordCount())
    }

    @Test
    fun `opakovane klepnuti vyber zrusi`() {
        val vm = SetSelectionViewModel(sets)
        vm.toggle("A")
        vm.toggle("A")
        assertTrue(vm.selected.value.isEmpty())
        assertFalse(vm.canContinue())
    }

    @Test
    fun `neznama sada se ignoruje`() {
        val vm = SetSelectionViewModel(sets)
        vm.toggle("Neexistuje")
        assertTrue(vm.selected.value.isEmpty())
    }

    // --- nastavení testu ---

    private fun setup(
        names: List<String>,
        store: FakeSettingsStore = FakeSettingsStore(),
    ) = TestSetupViewModel(names, sets, TestData.config, store)

    @Test
    fun `nova kombinace dostane vychozi hodnoty`() = runTest {
        val vm = setup(listOf("A"))
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.state.value.loaded)
        assertEquals(TestData.defaults, vm.state.value.settings)
    }

    @Test
    fun `nacte ulozene nastaveni pro presnou kombinaci`() = runTest {
        val store = FakeSettingsStore()
        val saved = TestData.defaults.copy(mode = TestMode.CHOICE, wrongOptions = 2, minPasses = 4)
        store.saveTestSettings(listOf("A", "B"), saved)

        val same = setup(listOf("B", "A"), store)
        val other = setup(listOf("A"), store)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(saved, same.state.value.settings)
        assertEquals(TestData.defaults, other.state.value.settings)
    }

    @Test
    fun `hodnoty se omezuji na povolene rozsahy`() = runTest {
        val vm = setup(listOf("A"))
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setWrongOptions(99)
        vm.setMinPasses(0)
        vm.setMinSuccessPercent(500)
        vm.setExtraLetters(-3)
        val s = vm.state.value.settings
        assertEquals(8, s.wrongOptions)
        assertEquals(1, s.minPasses)
        assertEquals(100, s.minSuccessPercent)
        assertEquals(0, s.extraLetters)
    }

    @Test
    fun `zmeny nastaveni se propisuji`() = runTest {
        val vm = setup(listOf("A"))
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setMode(TestMode.CHOICE)
        vm.setDirection(Direction.FOREIGN_TO_CZECH)
        vm.setEvaluation(Evaluation.WHOLE_WORD)
        vm.setWrongOptions(6)
        val s = vm.state.value.settings
        assertEquals(TestMode.CHOICE, s.mode)
        assertEquals(Direction.FOREIGN_TO_CZECH, s.direction)
        assertEquals(Evaluation.WHOLE_WORD, s.evaluation)
        assertEquals(6, s.wrongOptions)
    }

    @Test
    fun `malo slovicek pro N plus 1 ohlasi chybu a test se nespusti`() = runTest {
        val store = FakeSettingsStore()
        val vm = setup(listOf("B"), store) // 3 slovíčka
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setMode(TestMode.CHOICE)
        vm.setWrongOptions(4)
        var started: TestSettings? = null
        vm.start { started = it }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertNull(started)
        assertEquals(listOf(SettingsError.NotEnoughWords(required = 5, available = 3)), vm.state.value.errors)
        assertTrue(store.saved.isEmpty())
    }

    @Test
    fun `chyba zmizi po uprave nastaveni`() = runTest {
        val vm = setup(listOf("B"))
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setMode(TestMode.CHOICE)
        vm.start { }
        assertFalse(vm.state.value.errors.isEmpty())
        vm.setWrongOptions(2)
        assertTrue(vm.state.value.errors.isEmpty())
        var started = false
        vm.start { started = true }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(started)
    }

    @Test
    fun `slovicka se scitaji pres vybrane sady`() = runTest {
        val vm = setup(listOf("B", "C")) // 3 + 5
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(8, vm.wordCount)
        vm.setMode(TestMode.CHOICE)
        vm.setWrongOptions(7) // potřeba 8
        var started = false
        vm.start { started = true }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(started)
    }

    @Test
    fun `start ulozi nastaveni pro kombinaci a preda ho`() = runTest {
        val store = FakeSettingsStore()
        val vm = setup(listOf("A", "C"), store)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setMode(TestMode.CHOICE)
        vm.setMinPasses(3)
        var started: TestSettings? = null
        vm.start { started = it }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(vm.state.value.settings, started)
        assertEquals(started, store.loadTestSettings(listOf("C", "A")))
        // další otevření nabídne uložené hodnoty
        val again = setup(listOf("C", "A"), store)
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertEquals(started, again.state.value.settings)
    }

    @Test
    fun `rezim 2 nepotrebuje N plus 1 slovicek`() = runTest {
        val vm = setup(listOf("B"))
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        vm.setMode(TestMode.ASSEMBLY)
        var started = false
        vm.start { started = true }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        assertTrue(started)
    }

    @Test
    fun `pred nactenim nastaveni se test nespusti`() = runTest {
        val vm = setup(listOf("A"))
        var started = false
        vm.start { started = true }
        mainRule.dispatcher.scheduler.advanceUntilIdle()
        // Načtení doběhlo, ale start byl zavolán dřív => ignorován.
        assertFalse(started)
    }
}
