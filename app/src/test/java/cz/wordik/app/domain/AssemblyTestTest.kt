package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AssemblyTestTest {
    private fun round(
        foreign: String,
        evaluation: Evaluation,
        caseSensitive: Boolean = false,
        extra: Int = 0,
        pool: List<Char> = emptyList(),
        seed: Int = 1,
    ) = AssemblyRound(Word("cz", foreign, caseSensitive), evaluation, extra, pool, Random(seed))

    /** Najde dosud nepoužitou klávesu s daným písmenem (přesně). */
    private fun AssemblyRound.free(c: Char) = tiles.first { it.char == c && !isUsed(it) }

    private fun AssemblyRound.pressChar(c: Char) = press(free(c).id)

    private fun AssemblyRound.typeAll(text: String) = text.forEach { pressChar(it) }

    // --- nabídka písmen ---

    @Test
    fun `nabidka obsahuje vsechny znaky vcetne opakovanych`() {
        val r = round("book", Evaluation.PER_LETTER)
        assertEquals("bkoo".toList(), r.tiles.map { it.char }.sorted())
        assertEquals(2, r.tiles.count { it.char == 'o' })
    }

    @Test
    fun `nabidka obsahuje mezeru apostrof a pomlcku`() {
        val r = round("I'm a well-known", Evaluation.PER_LETTER)
        assertEquals("I'm a well-known".toList().sorted(), r.tiles.map { it.char }.sorted())
        assertTrue(r.tiles.any { it.char == ' ' })
        assertTrue(r.tiles.any { it.char == '\'' })
        assertTrue(r.tiles.any { it.char == '-' })
    }

    @Test
    fun `nabidka ma unikatni id a je nahodne zamichana`() {
        val orders = (0 until 30).map { round("abcdefgh", Evaluation.PER_LETTER, seed = it).tiles.map { t -> t.char } }
        assertEquals(setOf(8), (0 until 30).map { round("abcdefgh", Evaluation.PER_LETTER, seed = it).tiles.map { t -> t.id }.toSet().size }.toSet())
        assertTrue(orders.toSet().size > 1)
    }

    @Test
    fun `nabidka zobrazuje pismena tak jak jsou zapsana`() {
        val r = round("Monday", Evaluation.PER_LETTER)
        assertTrue(r.tiles.any { it.char == 'M' })
        assertFalse(r.tiles.any { it.char == 'm' })
    }

    // --- písmena navíc ---

    @Test
    fun `pismena navic jsou jen z povolene mnoziny`() {
        val pool = listOf('x', 'y', 'z')
        repeat(100) { seed ->
            val r = round("ab", Evaluation.PER_LETTER, extra = 5, pool = pool, seed = seed)
            assertEquals(7, r.tiles.size)
            val extras = r.tiles.map { it.char }.toMutableList().also { list -> "ab".forEach { list.remove(it) } }
            assertEquals(5, extras.size)
            assertTrue(extras.all { it in pool })
        }
    }

    @Test
    fun `pismeno navic se muze shodovat s pismenem ve slove`() {
        var seen = false
        repeat(100) { seed ->
            val r = round("a", Evaluation.PER_LETTER, extra = 3, pool = listOf('a'), seed = seed)
            if (r.tiles.count { it.char == 'a' } > 1) seen = true
        }
        assertTrue(seen)
    }

    @Test
    fun `nula pismen navic nic nepridava a prazdna mnozina take`() {
        assertEquals(3, round("abc", Evaluation.PER_LETTER, extra = 0, pool = listOf('x')).tiles.size)
        assertEquals(3, round("abc", Evaluation.PER_LETTER, extra = 4, pool = emptyList()).tiles.size)
    }

    @Test
    fun `mnozina pismen je z cizich slovicek bez mezer a interpunkce`() {
        val pool = AssemblyRound.letterPool(
            listOf(Word("a", "ice cream"), Word("b", "I'm"), Word("c", "well-known"), Word("d", "žába")),
        )
        assertTrue(pool.none { !it.isLetter() })
        assertTrue('ž' in pool && 'I' in pool && 'k' in pool)
        assertFalse(' ' in pool)
        assertFalse('\'' in pool)
        assertFalse('-' in pool)
        assertEquals(pool.size, pool.toSet().size)
        assertFalse('q' in pool)
    }

    // --- porovnání znaků ---

    @Test
    fun `velikost pismen se nerozlisuje`() {
        val r = round("Ii", Evaluation.PER_LETTER)
        assertEquals(PressResult.PLACED, r.pressChar('i')) // klávesa "i" odpovídá očekávanému "I"
        assertEquals(PressResult.PLACED, r.pressChar('I')) // klávesa "I" odpovídá očekávanému "i"
        assertTrue(r.isDone)
        assertFalse(r.hadError)
    }

    @Test
    fun `s uvozovkami se velikost pismen rozlisuje`() {
        val r = round("Ii", Evaluation.PER_LETTER, caseSensitive = true)
        assertEquals(PressResult.WRONG, r.pressChar('i')) // očekává se "I"
        assertEquals(PressResult.PLACED, r.pressChar('I'))
        assertEquals(PressResult.PLACED, r.pressChar('i'))
        assertTrue(r.isDone)
        assertEquals(1, r.wrongUnits)
        assertEquals(2, r.correctUnits)
    }

    @Test
    fun `diakritika je jine pismeno`() {
        // Pool s jediným "c" zaručí, že všechna písmena navíc jsou "c" bez háčku.
        val perLetter = round("č", Evaluation.PER_LETTER, extra = 3, pool = listOf('c'))
        assertEquals(PressResult.WRONG, perLetter.pressChar('c'))
        assertEquals(PressResult.PLACED, perLetter.pressChar('č'))

        val whole = round("č", Evaluation.WHOLE_WORD, extra = 3, pool = listOf('c'))
        whole.pressChar('c')
        assertEquals(false, whole.confirm())
    }

    // --- hodnocení po písmenkách ---

    @Test
    fun `po pismenkach spravne pismeno se vlozi na dalsi misto`() {
        val r = round("cat", Evaluation.PER_LETTER)
        assertEquals(PressResult.PLACED, r.pressChar('c'))
        assertEquals(listOf('c'), r.slotTiles.filterNotNull().map { it.char })
        assertEquals(1, r.correctUnits)
        assertEquals(0, r.wrongUnits)
        assertFalse(r.isDone)
    }

    @Test
    fun `po pismenkach spatne pismeno se nevlozi a pocita se chyba`() {
        val r = round("cat", Evaluation.PER_LETTER)
        assertEquals(PressResult.WRONG, r.pressChar('a'))
        assertEquals(0, r.filledCount)
        assertEquals(1, r.wrongUnits)
        assertEquals(0, r.correctUnits)
        // spatna klavesa zustava dostupna
        assertFalse(r.isUsed(r.tiles.first { it.char == 'a' }))
    }

    @Test
    fun `po pismenkach slovo s chybami nemuze mit vice spravnych nez ma pismen`() {
        val r = round("cat", Evaluation.PER_LETTER)
        repeat(5) { r.pressChar('t') } // 5x špatně
        r.typeAll("cat")
        assertTrue(r.isDone)
        assertEquals(3, r.correctUnits)
        assertEquals(5, r.wrongUnits)
        assertTrue(r.hadError)
        assertTrue(r.needsManualNext)
        assertFalse(r.advancesAutomatically)
    }

    @Test
    fun `po pismenkach slovo bez chyby prejde automaticky`() {
        val r = round("cat", Evaluation.PER_LETTER)
        r.typeAll("cat")
        assertTrue(r.isDone)
        assertTrue(r.advancesAutomatically)
        assertFalse(r.needsManualNext)
    }

    @Test
    fun `po dokonceni se dalsi klepnuti ignoruje`() {
        val r = round("a", Evaluation.PER_LETTER, extra = 2, pool = listOf('b'))
        r.pressChar('a')
        assertEquals(PressResult.IGNORED, r.press(r.tiles.first { it.char == 'b' }.id))
        assertEquals(0, r.wrongUnits)
    }

    @Test
    fun `po pismenkach dve pismena o ve slove book`() {
        val r = round("book", Evaluation.PER_LETTER)
        r.typeAll("book")
        assertTrue(r.isDone)
        assertEquals(4, r.correctUnits)
        assertEquals("book", r.slotTiles.joinToString("") { it!!.char.toString() })
    }

    @Test
    fun `po pismenkach mezera je samostatna klavesa`() {
        val r = round("ice cream", Evaluation.PER_LETTER)
        r.typeAll("ice")
        assertEquals(PressResult.WRONG, r.pressChar('c')) // očekává se mezera
        assertEquals(PressResult.PLACED, r.pressChar(' '))
        r.typeAll("cream")
        assertTrue(r.isDone)
        assertEquals(9, r.correctUnits)
        assertEquals(1, r.wrongUnits)
    }

    @Test
    fun `po pismenkach nelze vratit pismeno`() {
        val r = round("ab", Evaluation.PER_LETTER)
        r.pressChar('a')
        assertFalse(r.remove(0))
        assertEquals(1, r.filledCount)
    }

    @Test
    fun `pouzite pismeno nelze pouzit znovu`() {
        val r = round("aa", Evaluation.PER_LETTER)
        val first = r.tiles[0]
        assertEquals(PressResult.PLACED, r.press(first.id))
        assertEquals(PressResult.IGNORED, r.press(first.id))
        assertEquals(0, r.wrongUnits)
    }

    @Test
    fun `neexistujici klavesa se ignoruje`() {
        val r = round("a", Evaluation.PER_LETTER)
        assertEquals(PressResult.IGNORED, r.press(99))
    }

    // --- hodnocení celého slova ---

    @Test
    fun `cele slovo spravne`() {
        val r = round("cat", Evaluation.WHOLE_WORD)
        r.typeAll("cat")
        assertFalse(r.isDone)
        assertEquals(true, r.confirm())
        assertTrue(r.isDone)
        assertEquals(1, r.correctUnits)
        assertEquals(0, r.wrongUnits)
        assertTrue(r.advancesAutomatically)
    }

    @Test
    fun `cele slovo spatne je jedna chyba a vyzaduje Dal`() {
        val r = round("cat", Evaluation.WHOLE_WORD)
        r.typeAll("act")
        assertEquals(false, r.confirm())
        assertEquals(0, r.correctUnits)
        assertEquals(1, r.wrongUnits)
        assertTrue(r.needsManualNext)
    }

    @Test
    fun `nevyplnene slovo lze potvrdit a je to chyba`() {
        val r = round("cat", Evaluation.WHOLE_WORD)
        assertEquals(false, r.confirm())
        assertEquals(1, r.wrongUnits)
        assertTrue(r.needsManualNext)
    }

    @Test
    fun `castecne vyplnene slovo je chyba`() {
        val r = round("cat", Evaluation.WHOLE_WORD)
        r.typeAll("ca")
        assertEquals(false, r.confirm())
    }

    @Test
    fun `pismena se plni od prvniho volneho mista a lze je vratit`() {
        val r = round("cat", Evaluation.WHOLE_WORD)
        r.pressChar('a')
        r.pressChar('c')
        assertEquals("ac", r.slotTiles.filterNotNull().joinToString("") { it.char.toString() })
        assertTrue(r.remove(0))
        assertEquals(1, r.filledCount)
        // uvolněné první místo se obsadí jako první
        r.pressChar('t')
        assertEquals('t', r.slotTiles[0]!!.char)
        assertEquals('c', r.slotTiles[1]!!.char)
        // vrácená písmena jsou zase v nabídce
        assertTrue(r.remove(0))
        assertTrue(r.remove(1))
        assertEquals(0, r.filledCount)
        assertFalse(r.tiles.any { r.isUsed(it) })
        r.typeAll("cat")
        assertEquals(true, r.confirm())
    }

    @Test
    fun `vraceni z prazdneho mista nebo mimo rozsah selze`() {
        val r = round("ab", Evaluation.WHOLE_WORD)
        assertFalse(r.remove(0))
        assertFalse(r.remove(5))
        assertFalse(r.remove(-1))
    }

    @Test
    fun `plne slovo nepritahne dalsi pismeno`() {
        val r = round("a", Evaluation.WHOLE_WORD, extra = 1, pool = listOf('b'))
        r.pressChar('a')
        assertEquals(PressResult.IGNORED, r.press(r.tiles.first { it.char == 'b' }.id))
    }

    @Test
    fun `po potvrzeni uz nelze menit ani znovu potvrdit`() {
        val r = round("a", Evaluation.WHOLE_WORD)
        r.pressChar('a')
        r.confirm()
        assertFalse(r.remove(0))
        assertNull(r.confirm())
        assertEquals(1, r.correctUnits)
    }

    @Test
    fun `cele slovo bez rozliseni velikosti pismen a s rozlisenim`() {
        // Pool s jediným "a" zaručí, že všechna písmena navíc jsou malé "a".
        val loose = round("Ab", Evaluation.WHOLE_WORD, extra = 3, pool = listOf('a'))
        loose.pressChar('a')
        loose.pressChar('b')
        assertEquals(true, loose.confirm())

        val strict = round("Ab", Evaluation.WHOLE_WORD, caseSensitive = true, extra = 3, pool = listOf('a'))
        strict.pressChar('a')
        strict.pressChar('b')
        assertEquals(false, strict.confirm())

        val strictOk = round("Ab", Evaluation.WHOLE_WORD, caseSensitive = true)
        strictOk.typeAll("Ab")
        assertEquals(true, strictOk.confirm())
    }

    @Test
    fun `confirm a remove v rezimu po pismenkach nic nedelaji`() {
        val r = round("ab", Evaluation.PER_LETTER)
        assertNull(r.confirm())
        assertFalse(r.isDone)
    }

    // --- propojení s TestSession ---

    private fun session(words: List<Word>, minPasses: Int = 1, percent: Int = 100) =
        TestSession(words, minPasses, percent, Random(4)) { 0 }

    private fun assemblyTest(words: List<Word>, evaluation: Evaluation, extra: Int = 0, minPasses: Int = 1, percent: Int = 100) =
        AssemblyTest(
            session(words, minPasses, percent), evaluation, extra,
            AssemblyRound.letterPool(words), Random(6),
        )

    private fun AssemblyTest.type(text: String) {
        val r = round!!
        text.forEach { c -> press(r.tiles.first { it.char == c && !r.isUsed(it) }.id) }
    }

    @Test
    fun `po pismenkach se pismena zapisuji do statistiky testu`() {
        val t = assemblyTest(listOf(Word("kočka", "cat")), Evaluation.PER_LETTER)
        val r = t.round!!
        t.press(r.tiles.first { it.char == 'a' }.id) // špatně, očekává se c
        t.type("cat")
        assertEquals(3, t.session.totalCorrect)
        assertEquals(1, t.session.totalWrong)
        assertTrue(r.needsManualNext)
    }

    @Test
    fun `cele slovo se zapisuje az po potvrzeni`() {
        val t = assemblyTest(listOf(Word("kočka", "cat")), Evaluation.WHOLE_WORD)
        t.type("cat")
        assertEquals(0, t.session.totalCorrect)
        assertEquals(true, t.confirm())
        assertEquals(1, t.session.totalCorrect)
        assertEquals(0, t.session.totalWrong)
        assertNull(t.confirm())
        assertEquals(1, t.session.totalCorrect)
    }

    @Test
    fun `Dal funguje az po vyhodnoceni slova`() {
        val words = listOf(Word("a", "ab"), Word("b", "cd"))
        val t = assemblyTest(words, Evaluation.WHOLE_WORD)
        val first = t.round!!.word
        t.next()
        assertEquals(first, t.round!!.word)
        t.confirm()
        t.next()
        assertFalse(first == t.round!!.word)
    }

    @Test
    fun `projde vsechna slovicka a skonci`() {
        val words = listOf(Word("a", "ab"), Word("b", "cd"), Word("c", "ef"))
        val t = assemblyTest(words, Evaluation.PER_LETTER)
        val asked = mutableListOf<String>()
        while (!t.isFinished) {
            val r = t.round!!
            asked += r.word.foreign
            t.type(r.word.foreign)
            t.next()
        }
        assertEquals(words.map { it.foreign }.toSet(), asked.toSet())
        assertEquals(3, asked.size)
        assertNull(t.round)
        assertTrue(t.result().completed)
        assertEquals(6, t.result().correct)
    }

    @Test
    fun `pri nedosazene uspesnosti test pokracuje dalsim pruchodem`() {
        val words = listOf(Word("a", "ab"))
        val t = assemblyTest(words, Evaluation.WHOLE_WORD, percent = 100)
        t.confirm() // nevyplněné => chyba
        t.next()
        assertFalse(t.isFinished)
        t.type("ab")
        t.confirm()
        t.next()
        // 1 z 2 = 50 % < 100 %, pokračuje se
        assertFalse(t.isFinished)
    }

    @Test
    fun `predcasne ukonceni`() {
        val t = assemblyTest(listOf(Word("a", "ab")), Evaluation.PER_LETTER)
        t.type("a")
        t.abort()
        assertTrue(t.isFinished)
        assertNull(t.round)
        assertFalse(t.result().completed)
        assertEquals(1, t.result().correct)
        assertEquals(PressResult.IGNORED, t.press(0))
    }
}
