package cz.wordik.app.domain

import cz.wordik.app.data.SetFileSource
import cz.wordik.app.data.SetRepository
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Celé průchody testů nad skutečnými ukázkovými sadami a hraniční případy nastavení. */
class FullFlowTest {
    private object DiskSource : SetFileSource {
        override fun readAll() = File("src/main/assets/sets").listFiles { f -> f.extension == "txt" }.orEmpty()
            .sortedBy { it.name }
            .map { it.nameWithoutExtension to it.readText(Charsets.UTF_8) }
    }

    private val sets = SetRepository(DiskSource).loadAll()
    private val allWords = sets.flatMap { it.words }
    private val ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10)

    private fun session(words: List<Word>, passes: Int, percent: Int, seed: Int = 1) =
        TestSession(words, passes, percent, Random(seed)) { 0 }

    // --- režim 2 ---

    private fun playAssembly(words: List<Word>, evaluation: Evaluation, extra: Int, passes: Int, seed: Int): TestResult {
        val test = AssemblyTest(
            session(words, passes, 100, seed), evaluation, extra,
            AssemblyRound.letterPool(words), Random(seed),
        )
        var guard = 0
        while (!test.isFinished) {
            check(++guard < 10_000) { "Test se neukončil" }
            val round = test.round!!
            // Nabídka ostatních písmen navíc nesmí překážet: skládáme přesně podle cílového slova.
            round.target.forEach { c ->
                test.press(round.tiles.first { it.char == c && !round.isUsed(it) }.id)
            }
            if (evaluation == Evaluation.WHOLE_WORD) test.confirm()
            assertTrue(round.isDone)
            assertFalse(round.hadError)
            test.next()
        }
        return test.result()
    }

    @Test
    fun `rezim 2 po pismenkach projde vsechna slovicka obou sad bez chyby`() {
        val result = playAssembly(allWords, Evaluation.PER_LETTER, extra = 3, passes = 2, seed = 3)
        assertTrue(result.completed)
        assertEquals(2 * allWords.sumOf { it.foreign.length }, result.correct)
        assertEquals(0, result.wrong)
        assertEquals(allWords.size, result.words.size)
    }

    @Test
    fun `rezim 2 cele slovo projde vsechna slovicka obou sad bez chyby`() {
        val result = playAssembly(allWords, Evaluation.WHOLE_WORD, extra = 10, passes = 2, seed = 4)
        assertTrue(result.completed)
        assertEquals(2 * allWords.size, result.correct)
        assertEquals(0, result.wrong)
    }

    @Test
    fun `rezim 2 jedna sada s jednim slovickem`() {
        val result = playAssembly(listOf(Word("pes", "dog")), Evaluation.PER_LETTER, extra = 0, passes = 1, seed = 1)
        assertTrue(result.completed)
        assertEquals(3, result.correct)
    }

    @Test
    fun `rezim 2 slovicko v uvozovkach vyzaduje presnou velikost pismen`() {
        val monday = allWords.first { it.caseSensitive }
        val round = AssemblyRound(monday, Evaluation.PER_LETTER, extraLetters = 0, random = Random(1))
        assertEquals(PressResult.WRONG, round.press(round.tiles.first { it.char == 'o' }.id)) // očekává se "M"
        assertEquals(PressResult.PLACED, round.press(round.tiles.first { it.char == 'M' }.id))
    }

    @Test
    fun `rezim 2 fraze a apostrof z ukazkovych sad`() {
        val phrase = allWords.first { ' ' in it.foreign }
        val apostrophe = allWords.first { '\'' in it.foreign }
        for (word in listOf(phrase, apostrophe)) {
            val round = AssemblyRound(word, Evaluation.PER_LETTER, 0, emptyList(), Random(2))
            word.foreign.forEach { c -> round.press(round.tiles.first { it.char == c && !round.isUsed(it) }.id) }
            assertTrue(round.isDone)
            assertEquals(word.foreign.length, round.correctUnits)
            assertEquals(0, round.wrongUnits)
        }
    }

    // --- režim 1 ---

    private fun playChoice(words: List<Word>, direction: Direction, n: Int, passes: Int, percent: Int, seed: Int): TestResult {
        val random = Random(seed)
        val test = ChoiceTest(
            session(words, passes, percent, seed),
            ChoiceQuestionGenerator(words, direction, n, random),
        )
        var guard = 0
        while (!test.isFinished) {
            check(++guard < 10_000) { "Test se neukončil" }
            val q = test.question!!
            assertEquals(n + 1, q.options.size)
            assertEquals(true, test.answer(q.correctIndex))
            test.next()
        }
        return test.result()
    }

    @Test
    fun `rezim 1 vsechny smery a pocty variant na jedne sade`() {
        val oneSet = sets.first().words // 12 slovíček
        for (direction in Direction.entries) {
            for (n in 1..8) {
                val result = playChoice(oneSet, direction, n, passes = 2, percent = 100, seed = n)
                assertTrue(result.completed)
                assertEquals(2 * oneSet.size, result.correct)
            }
        }
    }

    @Test
    fun `rezim 1 vice sad dohromady`() {
        val result = playChoice(allWords, Direction.MIX, n = 8, passes = 3, percent = 80, seed = 9)
        assertTrue(result.completed)
        assertEquals(3 * allWords.size, result.correct)
    }

    @Test
    fun `rezim 1 minimalni nastaveni`() {
        val result = playChoice(allWords, Direction.CZECH_TO_FOREIGN, n = 1, passes = 1, percent = 1, seed = 2)
        assertTrue(result.completed)
        assertEquals(allWords.size, result.correct)
    }

    @Test
    fun `rezim 1 maximalni nastaveni deset pruchodu`() {
        val result = playChoice(allWords, Direction.MIX, n = 8, passes = 10, percent = 100, seed = 5)
        assertTrue(result.completed)
        assertEquals(10 * allWords.size, result.correct)
        assertTrue(result.words.all { it.correct == 10 && it.wrong == 0 })
    }

    @Test
    fun `rezim 1 s dvanacti slovicky nepodporuje devet variant`() {
        val oneSet = sets.first().words
        // N = 8 vyžaduje 9 slovíček, 12 stačí; ověříme, že validátor i generátor souhlasí.
        val settings = TestSettings(TestMode.CHOICE, 8, 1, 50, 0, Evaluation.PER_LETTER, Direction.MIX)
        assertTrue(SettingsValidator.validate(settings, ranges, listOf(sets.first())).isEmpty())
        ChoiceQuestionGenerator(oneSet, Direction.MIX, 8, Random(1))
    }

    @Test
    fun `jedno slovicko nestaci pro rezim 1 ale pro rezim 2 ano`() {
        val single = listOf(WordSet("Jedno", listOf(Word("pes", "dog"))))
        val choice = TestSettings(TestMode.CHOICE, 1, 1, 50, 0, Evaluation.PER_LETTER, Direction.MIX)
        assertEquals(
            listOf(SettingsError.NotEnoughWords(required = 2, available = 1)),
            SettingsValidator.validate(choice, ranges, single),
        )
        assertTrue(SettingsValidator.validate(choice.copy(mode = TestMode.ASSEMBLY), ranges, single).isEmpty())
    }

    @Test
    fun `prazdna sada se nespusti v zadnem rezimu`() {
        val empty = listOf(WordSet("Prazdna", emptyList()))
        val choice = TestSettings(TestMode.CHOICE, 1, 1, 50, 0, Evaluation.PER_LETTER, Direction.MIX)
        assertEquals(listOf<SettingsError>(SettingsError.NoWords), SettingsValidator.validate(choice, ranges, empty))
        assertEquals(
            listOf<SettingsError>(SettingsError.NoWords),
            SettingsValidator.validate(choice.copy(mode = TestMode.ASSEMBLY), ranges, empty),
        )
        assertEquals(
            listOf<SettingsError>(SettingsError.NoWords),
            SettingsValidator.validate(choice, ranges, emptyList()),
        )
    }

    // --- náhodné posloupnosti akcí (fuzz) ---

    @Test
    fun `nahodna klepani v rezimu po pismenkach drzi invarianty`() {
        val random = Random(123)
        repeat(500) {
            val word = allWords.random(random)
            val round = AssemblyRound(word, Evaluation.PER_LETTER, random.nextInt(0, 6), AssemblyRound.letterPool(allWords), Random(it))
            repeat(80) {
                when (random.nextInt(3)) {
                    0 -> round.press(random.nextInt(-2, round.tiles.size + 3))
                    1 -> round.remove(random.nextInt(-1, word.foreign.length + 2))
                    else -> round.confirm()
                }
                assertEquals(round.filledCount, round.correctUnits)
                assertTrue(round.correctUnits <= word.foreign.length)
                assertEquals(round.filledCount == word.foreign.length, round.isDone)
                assertTrue(round.wrongUnits >= 0)
            }
        }
    }

    @Test
    fun `nahodna klepani v rezimu cele slovo drzi invarianty`() {
        val random = Random(321)
        repeat(500) {
            val word = allWords.random(random)
            val round = AssemblyRound(word, Evaluation.WHOLE_WORD, random.nextInt(0, 6), AssemblyRound.letterPool(allWords), Random(it))
            repeat(80) {
                val filledBefore = round.filledCount
                val wasDone = round.isDone
                when (random.nextInt(3)) {
                    0 -> round.press(random.nextInt(-2, round.tiles.size + 3))
                    1 -> round.remove(random.nextInt(-1, word.foreign.length + 2))
                    else -> round.confirm()
                }
                assertTrue(round.correctUnits + round.wrongUnits <= 1)
                assertTrue(round.filledCount <= word.foreign.length)
                if (wasDone) assertEquals(filledBefore, round.filledCount) // po vyhodnocení se nic nemění
                assertEquals(round.isDone, round.correctUnits + round.wrongUnits == 1)
                // Každé použité písmeno je v nabídce jen jednou ve slotech.
                val used = round.slotTiles.filterNotNull().map { it.id }
                assertEquals(used.size, used.toSet().size)
            }
        }
    }

    @Test
    fun `nahodne akce nad TestSession drzi invarianty`() {
        val random = Random(77)
        repeat(300) { seed ->
            val s = TestSession(allWords, random.nextInt(1, 4), random.nextInt(0, 101), Random(seed)) { 0 }
            var steps = 0
            while (!s.isFinished && steps++ < 400) {
                when (random.nextInt(10)) {
                    0 -> s.abort()
                    else -> {
                        s.addResult(random.nextInt(0, 3), random.nextInt(0, 3))
                        s.completeWord()
                    }
                }
                assertEquals(s.isFinished, s.current == null)
                assertTrue(s.wordsLeftInPass in 0..allWords.size)
                assertEquals(s.totalCorrect, s.result().correct)
                assertTrue(s.result().words.all { it.total > 0 })
            }
            if (s.isFinished) assertNull(s.current)
        }
    }
}
