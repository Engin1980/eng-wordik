package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TestSessionTest {
    private fun words(n: Int) = List(n) { Word("cz$it", "en$it") }

    private var now = 0L

    private fun session(
        words: List<Word>,
        minPasses: Int = 2,
        percent: Int = 50,
        seed: Int = 1,
    ) = TestSession(words, minPasses, percent, Random(seed)) { now }

    /** Zodpoví aktuální slovíčko (správně/špatně) a uzavře ho; vrací zkoušené slovíčko. */
    private fun TestSession.answer(correct: Boolean): Word {
        val word = current!!
        if (correct) addResult(1, 0) else addResult(0, 1)
        completeWord()
        return word
    }

    @Test
    fun `kazde slovicko prave jednou v kazdem pruchodu`() {
        val all = words(6)
        val s = session(all, minPasses = 3, percent = 100)
        repeat(3) {
            val asked = List(all.size) { s.answer(true) }
            assertEquals(all.toSet(), asked.toSet())
            assertEquals(all.size, asked.size)
        }
        assertTrue(s.isFinished)
        assertEquals(3, s.completedPasses)
    }

    @Test
    fun `pri stejnem seedu je poradi stejne`() {
        val all = words(8)
        fun order(seed: Int): List<Word> {
            val s = session(all, minPasses = 2, percent = 100, seed = seed)
            return List(16) { s.answer(true) }
        }
        assertEquals(order(7), order(7))
    }

    @Test
    fun `poradi se mezi pruchody lisi a prvni slovicko se neopakuje`() {
        val all = words(5)
        repeat(50) { seed ->
            val s = session(all, minPasses = 10, percent = 100, seed = seed)
            val order = List(50) { s.answer(true) }
            order.zipWithNext().forEach { (a, b) -> assertTrue(a != b) }
        }
    }

    @Test
    fun `jedno slovicko se neopakuje na hranici pruchodu protoze nema na vyber`() {
        val s = session(words(1), minPasses = 3, percent = 100)
        repeat(3) { s.answer(true) }
        assertTrue(s.isFinished)
    }

    @Test
    fun `konci po poslednim pruchodu pokud je uspesnost splnena`() {
        val s = session(words(3), minPasses = 2, percent = 100)
        repeat(5) {
            s.answer(true)
            assertFalse(s.isFinished)
        }
        s.answer(true)
        assertTrue(s.isFinished)
        assertFalse(s.wasAborted)
        assertNull(s.current)
        assertEquals(2, s.completedPasses)
    }

    @Test
    fun `procento dosazene driv nez pruchody test neukonci`() {
        val s = session(words(3), minPasses = 2, percent = 50)
        repeat(3) { s.answer(true) }
        assertFalse(s.isFinished)
        assertEquals(1, s.completedPasses)
        assertNotNull(s.current)
    }

    @Test
    fun `procento nedosazeno po poslednim pruchodu znamena dalsi pruchod`() {
        val s = session(words(3), minPasses = 1, percent = 70)
        repeat(3) { s.answer(false) }
        assertFalse(s.isFinished)
        assertEquals(1, s.completedPasses)
        repeat(3) { s.answer(true) }
        // 3 správné z 6 = 50 %
        assertFalse(s.isFinished)
        repeat(3) { s.answer(true) }
        // 6 z 9 = 66,7 %
        assertFalse(s.isFinished)
        s.answer(true)
        // 7 z 10 = 70 %, konec po prvním slovíčku čtvrtého průchodu
        assertTrue(s.isFinished)
        assertEquals(3, s.completedPasses)
    }

    @Test
    fun `test skonci uprostred pruchodu kdyz se splni obe podminky`() {
        val s = session(words(4), minPasses = 2, percent = 50)
        // Průchod 1: 4 chyby. Průchod 2: 2 chyby, 2 správně => 2/8 = 25 %.
        listOf(false, false, false, false, false, false, true, true).forEach { s.answer(it) }
        assertFalse(s.isFinished)
        assertEquals(2, s.completedPasses)
        // Průchod 3: po jedné správné 3/9 = 33 %, po druhé 4/10 = 40 %, po třetí 5/11 = 45 %, po čtvrté 6/12 = 50 %.
        s.answer(true)
        s.answer(true)
        s.answer(true)
        assertFalse(s.isFinished)
        s.answer(true)
        assertTrue(s.isFinished)
        assertEquals(3, s.completedPasses)
    }

    @Test
    fun `konec uprostred pruchodu pri peti slovech`() {
        val s = session(words(5), minPasses = 2, percent = 40)
        // Průchod 1 a 2: 10 chyb. 3. průchod: po k správných musí platit k*100 >= 40*(10+k), tj. k >= 7 (60k >= 400).
        repeat(10) { s.answer(false) }
        assertFalse(s.isFinished)
        assertEquals(2, s.completedPasses)
        repeat(5) { s.answer(true) }
        assertFalse(s.isFinished)
        // 4. průchod: 6. a 7. správná odpověď => konec po druhém slovíčku 4. průchodu.
        s.answer(true)
        assertFalse(s.isFinished)
        s.answer(true)
        assertTrue(s.isFinished)
        assertEquals(3, s.completedPasses)
        assertEquals(3, s.wordsLeftInPass)
    }

    @Test
    fun `predcasne ukonceni`() {
        now = 1000
        val s = session(words(3))
        s.answer(true)
        now = 5000
        s.abort()
        assertTrue(s.isFinished)
        assertTrue(s.wasAborted)
        assertNull(s.current)
        val result = s.result()
        assertFalse(result.completed)
        assertEquals(1, result.correct)
        assertEquals(4000L, result.durationMillis)
    }

    @Test
    fun `po ukonceni uz nejde zapisovat`() {
        val s = session(words(3))
        s.abort()
        try {
            s.addResult(1, 0)
            fail()
        } catch (_: IllegalStateException) {
        }
        try {
            s.completeWord()
            fail()
        } catch (_: IllegalStateException) {
        }
    }

    @Test
    fun `abort po konci nic nemeni`() {
        val s = session(words(1), minPasses = 1, percent = 100)
        s.answer(true)
        assertTrue(s.isFinished)
        s.abort()
        assertFalse(s.wasAborted)
        assertTrue(s.result().completed)
    }

    @Test
    fun `doba trvani se zastavi po konci testu`() {
        now = 0
        val s = session(words(1), minPasses = 1, percent = 100)
        now = 2500
        s.answer(true)
        now = 99_000
        assertEquals(2500L, s.result().durationMillis)
    }

    @Test
    fun `doba trvani bezi dokud test bezi`() {
        now = 100
        val s = session(words(3))
        now = 600
        assertEquals(500L, s.result().durationMillis)
    }

    @Test
    fun `pocty a uspesnost`() {
        val s = session(words(4), minPasses = 5, percent = 100)
        s.answer(true)
        s.answer(true)
        s.answer(false)
        assertEquals(2, s.totalCorrect)
        assertEquals(1, s.totalWrong)
        assertEquals(2.0 / 3.0, s.successRate, 1e-9)
        val r = s.result()
        assertEquals(2, r.correct)
        assertEquals(1, r.wrong)
        assertEquals(3, r.total)
        assertEquals(2.0 / 3.0, r.successRate, 1e-9)
    }

    @Test
    fun `pismena se pocitaji jako samostatne jednotky`() {
        val s = session(words(2), minPasses = 1, percent = 80)
        // 1. slovo: 4 správná písmena a 1 chyba
        s.addResult(0, 1)
        s.addResult(4, 0)
        s.completeWord()
        assertFalse(s.isFinished)
        // 80 % = 4/5 po prvním slovu, ale průchod není hotový; 2. slovo: 3 správná => 7/8
        s.addResult(3, 0)
        s.completeWord()
        assertTrue(s.isFinished)
        assertEquals(7, s.totalCorrect)
        assertEquals(1, s.totalWrong)
    }

    @Test
    fun `vice zapisu k jednomu slovicku se secte`() {
        val all = words(2)
        val s = session(all, minPasses = 1, percent = 100)
        val first = s.current!!
        s.addResult(1, 0)
        s.addResult(0, 2)
        s.addResult(2, 0)
        s.completeWord()
        val entry = s.result().words.single()
        assertEquals(first, entry.word)
        assertEquals(3, entry.correct)
        assertEquals(2, entry.wrong)
    }

    @Test
    fun `chybna slovicka jsou ta s alespon jednou chybou`() {
        val all = listOf(Word("a", "a"), Word("b", "b"), Word("c", "c"))
        val s = session(all, minPasses = 1, percent = 100)
        val order = mutableListOf<Word>()
        order += s.answer(true)
        order += s.answer(false)
        order += s.answer(true)
        val result = s.result()
        assertEquals(listOf(order[1]), result.wrongWords)
    }

    @Test
    fun `vysledky jsou razene podle uspesnosti vzestupne a pri shode podle abecedy`() {
        val all = listOf(Word("žába", "frog"), Word("auto", "car"), Word("čaj", "tea"), Word("dům", "house"))
        val s = session(all, minPasses = 1, percent = 0)
        // Zodpovíme v pořadí, které vybere seed; úspěšnost přiřadíme podle slovíčka.
        val rates = mapOf(
            all[0] to (1 to 1),   // 50 %
            all[1] to (2 to 0),   // 100 %
            all[2] to (1 to 1),   // 50 %
            all[3] to (0 to 2),   // 0 %
        )
        repeat(4) {
            val (c, w) = rates.getValue(s.current!!)
            s.addResult(c, w)
            s.completeWord()
        }
        val order = s.result().words.map { it.word.czech }
        // 0 % (dům), pak 50 % abecedně (čaj před žába; česká kolace), pak 100 % (auto)
        assertEquals(listOf("dům", "čaj", "žába", "auto"), order)
    }

    @Test
    fun `razeni porovnava zlomky presne`() {
        val a = Word("a", "a")
        val b = Word("b", "b")
        val c = Word("c", "c")
        val s = session(listOf(a, b, c), minPasses = 1, percent = 0)
        val data = mapOf(a to (1 to 2), b to (2 to 4), c to (1 to 3)) // 33,3 %, 33,3 %, 25 %
        repeat(3) {
            val (cor, wr) = data.getValue(s.current!!)
            s.addResult(cor, wr)
            s.completeWord()
        }
        assertEquals(listOf("c", "a", "b"), s.result().words.map { it.word.czech })
    }

    @Test
    fun `nezkousena slovicka nejsou ve vysledcich`() {
        val s = session(words(5), minPasses = 1, percent = 100)
        s.answer(true)
        s.abort()
        assertEquals(1, s.result().words.size)
    }

    @Test
    fun `vice sad se sloucenim zkouseji dohromady`() {
        val setA = WordSet("A", listOf(Word("pes", "dog"), Word("kočka", "cat")))
        val setB = WordSet("B", listOf(Word("kůň", "horse")))
        val merged = listOf(setA, setB).flatMap { it.words }
        val s = session(merged, minPasses = 1, percent = 100)
        val asked = List(3) { s.answer(true) }
        assertEquals(merged.toSet(), asked.toSet())
        assertTrue(s.isFinished)
    }

    @Test
    fun `nulovy pocet jednotek test neukonci`() {
        val s = session(words(2), minPasses = 1, percent = 0)
        s.completeWord()
        s.completeWord()
        // 0 jednotek: cíl úspěšnosti se nepovažuje za dosažený, dokud nic není zodpovězeno
        assertFalse(s.isFinished)
    }

    @Test
    fun `neplatne parametry`() {
        listOf(
            { TestSession(emptyList(), 1, 50) },
            { TestSession(words(1), 0, 50) },
            { TestSession(words(1), 1, 101) },
        ).forEach {
            try {
                it()
                fail()
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
