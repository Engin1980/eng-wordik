package cz.wordik.app.ui

import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.Word
import cz.wordik.app.testutil.MainDispatcherRule
import cz.wordik.app.testutil.TestData
import cz.wordik.app.ui.test.AssemblyTestViewModel
import cz.wordik.app.ui.test.AssemblyUiState
import kotlin.random.Random
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AssemblyTestViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val words = listOf(Word("pes", "dog"), Word("kočka", "cat"), Word("ryba", "fish"))
    private var now = 0L
    private val scheduler get() = mainRule.dispatcher.scheduler

    private fun vm(
        evaluation: Evaluation,
        minPasses: Int = 1,
        percent: Int = 100,
        extra: Int = 0,
        list: List<Word> = words,
    ) = AssemblyTestViewModel(
        list,
        TestData.defaults.copy(evaluation = evaluation, minPasses = minPasses, minSuccessPercent = percent, extraLetters = extra),
        TestData.config,
        Random(5),
    ) { now }

    /** Zadání (české slovo) → cizí znění. */
    private fun AssemblyTestViewModel.target() = words.first { it.czech == state.value.prompt }.foreign

    private fun AssemblyTestViewModel.tapChar(c: Char) {
        val tile = state.value.tiles.first { it.char == c && !it.used }
        onTileClick(tile.id)
    }

    private fun AssemblyTestViewModel.type(text: String) = text.forEach { tapChar(it) }

    private val AssemblyUiState.filled get() = slots.count { it.char != null }

    // --- společné ---

    @Test
    fun `zacina zadanim a prazdnymi misty podle delky slova`() {
        val vm = vm(Evaluation.PER_LETTER)
        val s = vm.state.value
        val target = vm.target()
        assertEquals(target.length, s.slots.size)
        assertEquals(target.length, s.tiles.size)
        assertTrue(s.slots.all { it.char == null })
        assertTrue(s.tiles.none { it.used })
        assertFalse(s.done)
        assertNull(s.result)
    }

    @Test
    fun `pismena navic se pridaji do nabidky`() {
        val vm = vm(Evaluation.PER_LETTER, extra = 3)
        assertEquals(vm.target().length + 3, vm.state.value.tiles.size)
        assertEquals(vm.target().length, vm.state.value.slots.size)
    }

    // --- hodnocení po písmenkách ---

    @Test
    fun `spravne pismeno zaplni misto a zesedne v nabidce`() {
        val vm = vm(Evaluation.PER_LETTER)
        val target = vm.target()
        vm.tapChar(target[0])
        val s = vm.state.value
        assertEquals(target[0], s.slots[0].char)
        assertEquals(1, s.tiles.count { it.used })
        assertFalse(s.letterError)
    }

    @Test
    fun `spatne pismeno vyvola chybu a nevlozi se`() {
        val vm = vm(Evaluation.PER_LETTER, extra = 5)
        val target = vm.target()
        val wrong = vm.state.value.tiles.first { !it.used && it.char != target[0] }
        vm.onTileClick(wrong.id)
        val s = vm.state.value
        assertTrue(s.letterError)
        assertEquals(0, s.filled)
        assertFalse(s.tiles.first { it.id == wrong.id }.used)

        vm.tapChar(target[0])
        assertFalse(vm.state.value.letterError) // chyba zmizí po správném písmenu
    }

    @Test
    fun `slovo bez chyby se po intervalu prejde automaticky`() = runTest {
        val vm = vm(Evaluation.PER_LETTER, minPasses = 2)
        val first = vm.state.value.prompt
        vm.type(vm.target())
        assertTrue(vm.state.value.done)
        assertFalse(vm.state.value.needsManualNext)

        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis - 1)
        assertEquals(first, vm.state.value.prompt)
        scheduler.advanceTimeBy(2)
        scheduler.runCurrent()
        assertTrue(vm.state.value.prompt != first)
        assertFalse(vm.state.value.done)
    }

    @Test
    fun `slovo s chybou vyzaduje Dal a nepokracuje automaticky`() = runTest {
        val vm = vm(Evaluation.PER_LETTER, extra = 5, percent = 1)
        val target = vm.target()
        val first = vm.state.value.prompt
        vm.onTileClick(vm.state.value.tiles.first { !it.used && it.char != target[0] }.id)
        vm.type(target)
        val s = vm.state.value
        assertTrue(s.done)
        assertTrue(s.needsManualNext)
        assertEquals(target, s.correctAnswer)

        scheduler.advanceTimeBy(60_000)
        scheduler.runCurrent()
        assertEquals(first, vm.state.value.prompt)

        vm.onNext()
        assertTrue(vm.state.value.prompt != first)
        assertNull(vm.state.value.correctAnswer)
    }

    @Test
    fun `Dal pred intervalem prejde hned a automaticky prechod se nezdvoji`() = runTest {
        val vm = vm(Evaluation.PER_LETTER, minPasses = 2)
        vm.type(vm.target())
        vm.onNext()
        val second = vm.state.value.prompt
        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis * 3)
        scheduler.runCurrent()
        assertEquals(second, vm.state.value.prompt)
        assertEquals(0, vm.state.value.filled)
    }

    @Test
    fun `po pismenkach nelze vratit pismeno`() {
        val vm = vm(Evaluation.PER_LETTER)
        vm.tapChar(vm.target()[0])
        vm.onSlotClick(0)
        assertEquals(1, vm.state.value.filled)
    }

    @Test
    fun `po pismenkach test skonci po splneni podminek`() = runTest {
        val vm = vm(Evaluation.PER_LETTER)
        repeat(words.size) {
            vm.type(vm.target())
            vm.onNext()
        }
        val result = vm.state.value.result
        assertNotNull(result)
        assertTrue(result!!.completed)
        assertEquals(words.sumOf { it.foreign.length }, result.correct)
        assertEquals(0, result.wrong)
    }

    // --- hodnocení celého slova ---

    @Test
    fun `cele slovo se vyhodnoti az po potvrzeni`() {
        val vm = vm(Evaluation.WHOLE_WORD)
        vm.type(vm.target())
        assertFalse(vm.state.value.done)
        assertEquals(vm.target().length, vm.state.value.filled)
        vm.onConfirm()
        assertTrue(vm.state.value.done)
        assertFalse(vm.state.value.hadError)
    }

    @Test
    fun `klepnutim na vlozene pismeno se vrati do nabidky`() {
        val vm = vm(Evaluation.WHOLE_WORD)
        val target = vm.target()
        vm.tapChar(target[0])
        assertEquals(1, vm.state.value.tiles.count { it.used })
        vm.onSlotClick(0)
        assertEquals(0, vm.state.value.filled)
        assertEquals(0, vm.state.value.tiles.count { it.used })
    }

    @Test
    fun `potvrzeni nevyplneneho slova je chyba s povinnym Dal`() = runTest {
        val vm = vm(Evaluation.WHOLE_WORD, percent = 1)
        val first = vm.state.value.prompt
        val target = vm.target()
        vm.onConfirm()
        val s = vm.state.value
        assertTrue(s.done)
        assertTrue(s.hadError)
        assertEquals(target, s.correctAnswer)

        scheduler.advanceTimeBy(60_000)
        scheduler.runCurrent()
        assertEquals(first, vm.state.value.prompt)
        vm.onNext()
        assertTrue(vm.state.value.prompt != first)
    }

    @Test
    fun `spatne slozene slovo je chyba`() {
        val vm = vm(Evaluation.WHOLE_WORD)
        val target = vm.target()
        vm.type(target.reversed().let { if (it == target) target.drop(1) else it })
        vm.onConfirm()
        assertTrue(vm.state.value.hadError)
        assertEquals(target, vm.state.value.correctAnswer)
    }

    @Test
    fun `spravne slovo po potvrzeni prejde automaticky`() = runTest {
        val vm = vm(Evaluation.WHOLE_WORD, minPasses = 2)
        val first = vm.state.value.prompt
        vm.type(vm.target())
        vm.onConfirm()
        assertFalse(vm.state.value.needsManualNext)
        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis + 1)
        scheduler.runCurrent()
        assertTrue(vm.state.value.prompt != first)
    }

    @Test
    fun `druhe potvrzeni ani dalsi klepnuti po vyhodnoceni nic nezmeni`() {
        val vm = vm(Evaluation.WHOLE_WORD)
        vm.type(vm.target())
        vm.onConfirm()
        val before = vm.state.value
        vm.onConfirm()
        vm.onSlotClick(0)
        assertEquals(before, vm.state.value)
    }

    // --- ukončení ---

    @Test
    fun `predcasne ukonceni vrati nedokonceny vysledek`() = runTest {
        now = 100
        val vm = vm(Evaluation.PER_LETTER)
        vm.tapChar(vm.target()[0])
        now = 1_100
        vm.onQuit()
        val result = vm.state.value.result
        assertNotNull(result)
        assertFalse(result!!.completed)
        assertEquals(1, result.correct)
        assertEquals(1_000L, result.durationMillis)
        scheduler.advanceUntilIdle()
    }

    @Test
    fun `ukonceni zrusi cekajici automaticky prechod`() = runTest {
        val vm = vm(Evaluation.PER_LETTER, minPasses = 2)
        vm.type(vm.target())
        vm.onQuit()
        scheduler.advanceUntilIdle()
        assertNotNull(vm.state.value.result)
        assertFalse(vm.state.value.result!!.completed)
    }

    @Test
    fun `fraze s mezerou ma mezeru jako samostatnou klavesu`() {
        val vm = vm(Evaluation.PER_LETTER, list = listOf(Word("zmrzlina", "ice cream")))
        assertEquals(9, vm.state.value.slots.size)
        assertTrue(vm.state.value.tiles.any { it.char == ' ' })
        vm.type("ice cream")
        assertTrue(vm.state.value.done)
    }
}
