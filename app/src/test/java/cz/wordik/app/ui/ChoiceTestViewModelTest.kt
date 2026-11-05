package cz.wordik.app.ui

import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.Word
import cz.wordik.app.testutil.MainDispatcherRule
import cz.wordik.app.testutil.TestData
import cz.wordik.app.ui.test.ChoiceTestViewModel
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

class ChoiceTestViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val words = listOf(
        Word("pes", "dog"), Word("kočka", "cat"), Word("kůň", "horse"),
        Word("ryba", "fish"), Word("myš", "mouse"),
    )

    private val settings = TestSettings(
        mode = TestMode.CHOICE,
        wrongOptions = 3,
        minPasses = 1,
        minSuccessPercent = 100,
        extraLetters = 0,
        evaluation = TestData.defaults.evaluation,
        direction = Direction.CZECH_TO_FOREIGN,
    )

    private var now = 0L

    private fun vm(
        s: TestSettings = settings,
        list: List<Word> = words,
    ) = ChoiceTestViewModel(list, s, TestData.config, Random(2)) { now }

    private val scheduler get() = mainRule.dispatcher.scheduler

    private fun ChoiceTestViewModel.answerCorrect() {
        onOptionClick(state.value.options.indexOf(correctTextOf(this)))
    }

    /** Správná odpověď podle zadání (směr česky → cizí). */
    private fun correctTextOf(vm: ChoiceTestViewModel): String =
        words.first { it.czech == vm.state.value.prompt }.foreign

    private fun ChoiceTestViewModel.answerWrong() {
        val correct = correctTextOf(this)
        onOptionClick(state.value.options.indexOfFirst { it != correct })
    }

    @Test
    fun `zacina otazkou s N plus 1 moznostmi bez odhalene odpovedi`() {
        val vm = vm()
        val s = vm.state.value
        assertEquals(4, s.options.size)
        assertNull(s.answeredIndex)
        assertNull(s.correctIndex)
        assertNull(s.isCorrect)
        assertNull(s.result)
        assertEquals(1, s.pass)
    }

    @Test
    fun `po spravne odpovedi se po intervalu prejde automaticky`() = runTest {
        val vm = vm()
        val first = vm.state.value.prompt
        vm.answerCorrect()
        assertEquals(true, vm.state.value.isCorrect)
        assertNotNull(vm.state.value.correctIndex)

        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis - 1)
        assertEquals(first, vm.state.value.prompt) // ještě neuplynul interval
        assertNotNull(vm.state.value.answeredIndex)

        scheduler.advanceTimeBy(2)
        scheduler.runCurrent()
        assertTrue(vm.state.value.prompt != first)
        assertNull(vm.state.value.answeredIndex)
    }

    @Test
    fun `tlacitko Dal prejde driv a automaticky prechod se nezdvoji`() = runTest {
        val vm = vm()
        val first = vm.state.value.prompt
        vm.answerCorrect()
        vm.onNext()
        val second = vm.state.value.prompt
        assertTrue(second != first)

        // Po uplynutí intervalu se nesmí přeskočit i druhá otázka.
        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis * 3)
        scheduler.runCurrent()
        assertEquals(second, vm.state.value.prompt)
        assertNull(vm.state.value.answeredIndex)
    }

    @Test
    fun `po chybne odpovedi se zvyrazni spravna a nepokracuje se automaticky`() = runTest {
        val vm = vm()
        val prompt = vm.state.value.prompt
        vm.answerWrong()
        val s = vm.state.value
        assertEquals(false, s.isCorrect)
        assertNotNull(s.correctIndex)
        assertTrue(s.answeredIndex != s.correctIndex)
        assertEquals(correctTextOf(vm), s.options[s.correctIndex!!])

        scheduler.advanceTimeBy(60_000)
        scheduler.runCurrent()
        assertEquals(prompt, vm.state.value.prompt)
        assertNotNull(vm.state.value.answeredIndex)

        vm.onNext()
        assertTrue(vm.state.value.prompt != prompt)
    }

    @Test
    fun `nelze odpovedet znovu`() = runTest {
        val vm = vm()
        vm.answerWrong()
        val before = vm.state.value
        vm.answerCorrect()
        assertEquals(before, vm.state.value)
    }

    @Test
    fun `Dal bez odpovedi nic nedela`() {
        val vm = vm()
        val before = vm.state.value
        vm.onNext()
        assertEquals(before, vm.state.value)
    }

    @Test
    fun `test skonci po splneni podminek a vysledek je dokonceny`() = runTest {
        val vm = vm()
        repeat(words.size) {
            assertNull(vm.state.value.result)
            vm.answerCorrect()
            vm.onNext()
        }
        val result = vm.state.value.result
        assertNotNull(result)
        assertTrue(result!!.completed)
        assertEquals(words.size, result.correct)
        assertEquals(0, result.wrong)
    }

    @Test
    fun `automaticky prechod po posledni odpovedi ukonci test`() = runTest {
        val vm = vm(settings.copy(minPasses = 1))
        repeat(words.size - 1) {
            vm.answerCorrect()
            vm.onNext()
        }
        vm.answerCorrect()
        assertNull(vm.state.value.result)
        scheduler.advanceTimeBy(TestData.config.autoAdvanceMillis + 1)
        scheduler.runCurrent()
        assertNotNull(vm.state.value.result)
    }

    @Test
    fun `pri nizke uspesnosti test pokracuje druhym pruchodem`() = runTest {
        val vm = vm()
        repeat(words.size) {
            vm.answerWrong()
            vm.onNext()
        }
        assertNull(vm.state.value.result)
        assertEquals(2, vm.state.value.pass)
    }

    @Test
    fun `predcasne ukonceni vrati nedokonceny vysledek`() = runTest {
        val vm = vm()
        vm.answerCorrect()
        vm.onNext()
        vm.onQuit()
        val result = vm.state.value.result
        assertNotNull(result)
        assertFalse(result!!.completed)
        assertEquals(1, result.correct)
    }

    @Test
    fun `ukonceni zrusi cekajici automaticky prechod`() = runTest {
        val vm = vm()
        vm.answerCorrect()
        vm.onQuit()
        scheduler.advanceUntilIdle()
        assertNotNull(vm.state.value.result)
        assertEquals(1, vm.state.value.result!!.words.size)
    }

    @Test
    fun `doba trvani se pocita z hodin`() = runTest {
        now = 1_000
        val vm = vm()
        now = 8_000
        vm.onQuit()
        assertEquals(7_000L, vm.state.value.result!!.durationMillis)
    }

    @Test
    fun `malo ruznych moznosti je chyba`() {
        val vm = ChoiceTestViewModel(words.take(2), settings, TestData.config, Random(1)) { now }
        assertTrue(vm.state.value.error)
        vm.onOptionClick(0)
        vm.onNext()
        vm.onQuit()
        assertTrue(vm.state.value.error)
    }
}
