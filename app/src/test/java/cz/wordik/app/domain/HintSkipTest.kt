package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HintSkipTest {
    private fun round(foreign: String, evaluation: Evaluation = Evaluation.PER_LETTER, extra: Int = 0, pool: List<Char> = emptyList()) =
        AssemblyRound(Word("cz", foreign), evaluation, extra, pool, Random(1))

    private fun AssemblyRound.press(c: Char) = press(tiles.first { it.char == c && !isUsed(it) }.id)

    @Test
    fun `napoveda doplni dalsi pismeno a spocita jednu chybu`() {
        val r = round("book")
        assertTrue(r.hint())
        assertEquals(1, r.filledCount)
        assertEquals(0, r.correctUnits)
        assertEquals(1, r.wrongUnits)
        assertFalse(r.isDone)
        // další očekávané písmeno je 'o'
        assertEquals(PressResult.PLACED, r.press('o'))
        assertEquals(1, r.correctUnits)
    }

    @Test
    fun `napoveda posledniho pismena dokonci slovo s chybou`() {
        val r = round("it")
        r.press('i')
        r.hint()
        assertTrue(r.isDone)
        assertTrue(r.needsManualNext)
        assertEquals(1, r.correctUnits)
        assertEquals(1, r.wrongUnits)
    }

    @Test
    fun `presko cene slovo ma tolik chyb kolik zbyva pismen a zobrazi reseni`() {
        val r = round("book", extra = 3, pool = "xyz".toList())
        r.press('b')
        r.press('x') // chyba
        assertTrue(r.giveUp())
        assertTrue(r.isDone && r.gaveUp && r.needsManualNext)
        assertEquals(1, r.correctUnits)
        assertEquals(1 + 3, r.wrongUnits)
        assertEquals("book", r.slotTiles.joinToString("") { it!!.char.toString() })
    }

    @Test
    fun `presko cene slovo bez pokusu ma tolik chyb kolik ma pismen`() {
        val r = round("ice cream")
        r.giveUp()
        assertEquals(9, r.wrongUnits)
        assertEquals(0, r.correctUnits)
    }

    @Test
    fun `napoveda a preskoceni nefunguji pri hodnoceni celeho slova ani po vyhodnoceni`() {
        val whole = round("cat", Evaluation.WHOLE_WORD)
        assertFalse(whole.hint())
        assertFalse(whole.giveUp())
        val done = round("it")
        done.press('i'); done.press('t')
        assertFalse(done.hint())
        assertFalse(done.giveUp())
    }

    @Test
    fun `preskoceni se zapise do statistiky testu`() {
        val words = listOf(Word("a", "dog"), Word("b", "cat"))
        val session = TestSession(words, 1, 0, Random(1)) { 0 }
        val test = AssemblyTest(session, Evaluation.PER_LETTER, 0, emptyList(), Random(1))
        test.hint()
        test.giveUp()
        assertEquals(0, session.totalCorrect)
        assertEquals(3, session.totalWrong)
        test.next()
        assertEquals(1, session.wordsDoneInPass)
    }

    @Test
    fun `preskoceni slova s velkymi pismeny pouzije spravne klavesy`() {
        val r = AssemblyRound(Word("pondělí", "Monday", caseSensitive = true), Evaluation.PER_LETTER, 0, emptyList(), Random(2))
        r.giveUp()
        assertEquals("Monday", r.slotTiles.joinToString("") { it!!.char.toString() })
    }
}
