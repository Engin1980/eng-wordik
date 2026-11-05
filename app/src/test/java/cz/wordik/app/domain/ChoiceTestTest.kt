package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ChoiceTestTest {
    private val words = listOf(
        Word("pes", "dog"), Word("kočka", "cat"), Word("kůň", "horse"), Word("ryba", "fish"),
        Word("myš", "mouse"), Word("sova", "owl"), Word("slon", "elephant"), Word("žába", "frog"),
    )

    private fun generator(direction: Direction, n: Int = 4, list: List<Word> = words, seed: Int = 3) =
        ChoiceQuestionGenerator(list, direction, n, Random(seed))

    @Test
    fun `pocet moznosti je N plus 1`() {
        for (n in 1..7) {
            val q = generator(Direction.MIX, n).generate(words[0])
            assertEquals(n + 1, q.options.size)
        }
    }

    @Test
    fun `spravna odpoved je vzdy mezi moznostmi na uvedenem indexu`() {
        val gen = generator(Direction.CZECH_TO_FOREIGN)
        repeat(200) {
            val w = words.random(Random(it))
            val q = gen.generate(w)
            assertEquals(w.foreign, q.options[q.correctIndex])
            assertEquals(w.foreign, q.correctText)
        }
    }

    @Test
    fun `zadne duplicity ani text shodny se spravnou odpovedi`() {
        val gen = generator(Direction.MIX)
        repeat(300) {
            val q = gen.generate(words[it % words.size])
            assertEquals(q.options.size, q.options.toSet().size)
            assertEquals(1, q.options.count { o -> o == q.correctText })
        }
    }

    @Test
    fun `smer cesky na cizi`() {
        val gen = generator(Direction.CZECH_TO_FOREIGN)
        repeat(50) {
            val q = gen.generate(words[it % words.size])
            assertEquals(Side.CZECH, q.promptSide)
            assertEquals(q.word.czech, q.prompt)
            assertTrue(q.options.all { o -> words.any { w -> w.foreign == o } })
        }
    }

    @Test
    fun `smer cizi na cesky`() {
        val gen = generator(Direction.FOREIGN_TO_CZECH)
        repeat(50) {
            val q = gen.generate(words[it % words.size])
            assertEquals(Side.FOREIGN, q.promptSide)
            assertEquals(q.word.foreign, q.prompt)
            assertEquals(q.word.czech, q.correctText)
            assertTrue(q.options.all { o -> words.any { w -> w.czech == o } })
        }
    }

    @Test
    fun `mix dava obe strany`() {
        val gen = generator(Direction.MIX)
        val sides = List(100) { gen.generate(words[0]).promptSide }.toSet()
        assertEquals(setOf(Side.CZECH, Side.FOREIGN), sides)
    }

    @Test
    fun `mix je konzistentni v ramci jedne otazky`() {
        val gen = generator(Direction.MIX)
        repeat(100) {
            val q = gen.generate(words[it % words.size])
            val answerIsForeign = q.promptSide == Side.CZECH
            assertEquals(if (answerIsForeign) q.word.foreign else q.word.czech, q.correctText)
            assertEquals(if (answerIsForeign) q.word.czech else q.word.foreign, q.prompt)
        }
    }

    @Test
    fun `spravna odpoved je na ruznych pozicich`() {
        val gen = generator(Direction.CZECH_TO_FOREIGN)
        val positions = List(200) { gen.generate(words[0]).correctIndex }.toSet()
        assertEquals((0..4).toSet(), positions)
    }

    @Test
    fun `text shodny se spravnou odpovedi se vynecha`() {
        // Dvě slovíčka mají stejný cizí text; nesmí se objevit dvakrát ani jako "nesprávná" varianta.
        val list = listOf(
            Word("zámek", "lock"), Word("zámek (hrad)", "lock"), Word("klíč", "key"), Word("dveře", "door"),
        )
        val gen = ChoiceQuestionGenerator(list, Direction.CZECH_TO_FOREIGN, 2, Random(1))
        repeat(100) {
            val q = gen.generate(list[0])
            assertEquals(1, q.options.count { o -> o == "lock" })
            assertEquals(3, q.options.size)
        }
    }

    @Test
    fun `malo slovicek je chyba pri vytvoreni`() {
        try {
            generator(Direction.MIX, n = 4, list = words.take(4))
            fail()
        } catch (e: NotEnoughOptionsException) {
            assertEquals(5, e.required)
            assertEquals(4, e.available)
        }
    }

    @Test
    fun `presne N plus 1 slovicek staci`() {
        val gen = generator(Direction.MIX, n = 4, list = words.take(5))
        val q = gen.generate(words[0])
        assertEquals(5, q.options.size)
    }

    @Test
    fun `stejne texty snizuji pocet ruznych moznosti`() {
        val list = listOf(Word("a", "x"), Word("b", "x"), Word("c", "y"), Word("d", "z"), Word("e", "w"))
        // 5 slovíček, ale cizí strana má jen 4 různé texty => pro N=4 nestačí
        try {
            ChoiceQuestionGenerator(list, Direction.CZECH_TO_FOREIGN, 4)
            fail()
        } catch (e: NotEnoughOptionsException) {
            assertEquals(4, e.available)
        }
        // česká strana má 5 různých, takže opačný směr projde
        ChoiceQuestionGenerator(list, Direction.FOREIGN_TO_CZECH, 4)
    }

    @Test
    fun `vice sad dohromady`() {
        val a = WordSet("A", words.take(3))
        val b = WordSet("B", words.drop(3))
        val merged = listOf(a, b).flatMap { it.words }
        val gen = ChoiceQuestionGenerator(merged, Direction.CZECH_TO_FOREIGN, 7, Random(5))
        val q = gen.generate(a.words[0])
        assertEquals(merged.map { it.foreign }.toSet(), q.options.toSet())
    }

    @Test
    fun `nulovy pocet variant je neplatny`() {
        try {
            generator(Direction.MIX, n = 0)
            fail()
        } catch (_: IllegalArgumentException) {
        }
    }

    // --- propojení s TestSession ---

    private fun test(minPasses: Int = 1, percent: Int = 100, list: List<Word> = words.take(5), n: Int = 3): ChoiceTest {
        val session = TestSession(list, minPasses, percent, Random(9)) { 0 }
        return ChoiceTest(session, ChoiceQuestionGenerator(list, Direction.MIX, n, Random(9)))
    }

    @Test
    fun `spravna odpoved se zapocita a vrati true`() {
        val t = test()
        val q = t.question!!
        assertEquals(true, t.answer(q.correctIndex))
        assertEquals(true, t.lastAnswerCorrect)
        assertEquals(1, t.session.totalCorrect)
        assertEquals(0, t.session.totalWrong)
    }

    @Test
    fun `spatna odpoved se zapocita a vrati false`() {
        val t = test()
        val q = t.question!!
        val wrong = (0 until q.options.size).first { it != q.correctIndex }
        assertEquals(false, t.answer(wrong))
        assertEquals(false, t.lastAnswerCorrect)
        assertEquals(1, t.session.totalWrong)
    }

    @Test
    fun `na otazku lze odpovedet jen jednou`() {
        val t = test()
        val q = t.question!!
        val wrong = (0 until q.options.size).first { it != q.correctIndex }
        t.answer(wrong)
        assertNull(t.answer(q.correctIndex))
        assertEquals(0, t.session.totalCorrect)
        assertEquals(1, t.session.totalWrong)
    }

    @Test
    fun `neplatny index se ignoruje`() {
        val t = test()
        assertNull(t.answer(-1))
        assertNull(t.answer(99))
        assertFalse(t.isAnswered)
    }

    @Test
    fun `dalsi otazka az po odpovedi`() {
        val t = test()
        val first = t.question
        t.next()
        assertEquals(first, t.question)
        t.answer(first!!.correctIndex)
        t.next()
        assertFalse(first == t.question)
        assertFalse(t.isAnswered)
    }

    @Test
    fun `projde vsechna slovicka a skonci`() {
        val list = words.take(5)
        val t = test(minPasses = 2, list = list)
        val asked = mutableListOf<Word>()
        while (!t.isFinished) {
            val q = t.question!!
            asked += q.word
            t.answer(q.correctIndex)
            t.next()
        }
        assertEquals(10, asked.size)
        assertEquals(list.toSet(), asked.toSet())
        assertTrue(t.result().completed)
        assertEquals(10, t.result().correct)
        assertNull(t.question)
    }

    @Test
    fun `pri nizke uspesnosti se pokracuje dalsimi pruchody`() {
        val t = test(minPasses = 1, percent = 100)
        // první průchod samé chyby
        repeat(5) {
            val q = t.question!!
            t.answer((0 until q.options.size).first { it != q.correctIndex })
            t.next()
        }
        assertFalse(t.isFinished)
        assertNotNull(t.question)
    }

    @Test
    fun `predcasne ukonceni`() {
        val t = test()
        t.answer(t.question!!.correctIndex)
        t.abort()
        assertTrue(t.isFinished)
        assertNull(t.question)
        assertFalse(t.result().completed)
        assertEquals(1, t.result().correct)
    }
}
