package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Režim "Regresivní": opakování jen slovíček s chybou, dokud nejsou všechna napsána bez chyby. */
class RegressiveTest {
    private val words = listOf(Word("a", "ab"), Word("b", "cd"), Word("c", "ef"))

    private fun test() = AssemblyTest(
        TestSession(words, 1, 100, Random(3), regressive = true) { 0 },
        Evaluation.PER_LETTER, 0, emptyList(), Random(3),
    )

    private fun AssemblyTest.write(correct: Boolean) {
        val r = round!!
        if (!correct) r.tiles.first { it.char != r.target[0] }.let { press(it.id) }
        if (correct) r.target.forEach { c -> press(r.tiles.first { t -> t.char == c && !r.isUsed(t) }.id) } else giveUp()
        next()
    }

    @Test
    fun `po bezchybnem pruchodu test konci`() {
        val t = test()
        repeat(3) { t.write(true) }
        assertTrue(t.isFinished)
        assertTrue(t.result().completed)
    }

    @Test
    fun `dalsi kolo obsahuje jen slovicka s chybou`() {
        val t = test()
        t.write(true)
        t.write(false)
        t.write(true)
        assertFalse(t.isFinished)
        assertEquals(1, t.session.wordCount)
        assertEquals(2, t.session.completedPasses + 1)
        t.write(true)
        assertTrue(t.isFinished)
    }

    @Test
    fun `slovicko s chybou se opakuje dokud neni napsano bez chyby`() {
        val t = test()
        repeat(3) { t.write(false) }
        assertEquals(3, t.session.wordCount)
        repeat(3) { t.write(false) }
        assertFalse(t.isFinished)
        repeat(3) { t.write(true) }
        assertTrue(t.isFinished)
    }

    @Test
    fun `validator v regresivnim rezimu nekontroluje pruchody ani uspesnost`() {
        val ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10)
        val s = TestSettings(TestMode.REGRESSIVE, 4, 99, 999, 2, Evaluation.PER_LETTER, Direction.MIX)
        assertTrue(SettingsValidator.validate(s, ranges, listOf(WordSet("x", words))).isEmpty())
        assertEquals(1, SettingsValidator.validate(s.copy(extraLetters = 99), ranges, listOf(WordSet("x", words))).size)
    }
}
