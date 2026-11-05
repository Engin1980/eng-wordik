package cz.wordik.app.domain

import cz.wordik.app.domain.SettingsError.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsValidatorTest {
    private val ranges = SettingsRanges(
        wrongOptions = 1..8,
        minPasses = 1..10,
        minSuccessPercent = 1..100,
        extraLetters = 0..10,
    )

    private val choice = TestSettings(
        mode = TestMode.CHOICE,
        wrongOptions = 4,
        minPasses = 2,
        minSuccessPercent = 50,
        extraLetters = 0,
        evaluation = Evaluation.PER_LETTER,
        direction = Direction.MIX,
    )
    private val assembly = choice.copy(mode = TestMode.ASSEMBLY)

    private fun set(name: String, count: Int) =
        WordSet(name, List(count) { Word("cz$name$it", "en$name$it") })

    private fun validate(settings: TestSettings, vararg sets: WordSet) =
        SettingsValidator.validate(settings, ranges, sets.toList())

    @Test
    fun `platne nastaveni nema chyby`() {
        assertTrue(validate(choice, set("A", 12)).isEmpty())
        assertTrue(validate(assembly, set("A", 12)).isEmpty())
    }

    @Test
    fun `rezim 1 vyzaduje alespon N plus 1 slovicek`() {
        assertEquals(
            listOf(SettingsError.NotEnoughWords(required = 5, available = 4)),
            validate(choice, set("A", 4)),
        )
        assertTrue(validate(choice, set("A", 5)).isEmpty())
    }

    @Test
    fun `slovicka se scitaji pres vice sad`() {
        assertTrue(validate(choice, set("A", 2), set("B", 3)).isEmpty())
        assertEquals(1, validate(choice, set("A", 2), set("B", 2)).size)
    }

    @Test
    fun `pocet slovicek zavisi na zvolenem N`() {
        val withEight = choice.copy(wrongOptions = 8)
        assertEquals(
            listOf(SettingsError.NotEnoughWords(required = 9, available = 8)),
            validate(withEight, set("A", 8)),
        )
        assertTrue(validate(withEight, set("A", 9)).isEmpty())
    }

    @Test
    fun `rezim 2 nepotrebuje N plus 1 slovicek`() {
        assertTrue(validate(assembly, set("A", 1)).isEmpty())
    }

    @Test
    fun `zadna sada nebo prazdna sada`() {
        assertEquals(listOf(SettingsError.NoWords), validate(choice))
        assertEquals(listOf(SettingsError.NoWords), validate(assembly, set("A", 0)))
        assertEquals(listOf(SettingsError.NoWords), validate(choice, set("A", 0), set("B", 0)))
    }

    @Test
    fun `hranice rozsahu jsou platne`() {
        val minimal = choice.copy(wrongOptions = 1, minPasses = 1, minSuccessPercent = 1)
        val maximal = choice.copy(wrongOptions = 8, minPasses = 10, minSuccessPercent = 100)
        assertTrue(validate(minimal, set("A", 12)).isEmpty())
        assertTrue(validate(maximal, set("A", 12)).isEmpty())
        assertTrue(validate(assembly.copy(extraLetters = 0), set("A", 1)).isEmpty())
        assertTrue(validate(assembly.copy(extraLetters = 10), set("A", 1)).isEmpty())
    }

    @Test
    fun `hodnoty mimo rozsah`() {
        val sets = arrayOf(set("A", 20))
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.WRONG_OPTIONS, 0, 1..8)),
            validate(choice.copy(wrongOptions = 0), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.WRONG_OPTIONS, 9, 1..8)),
            validate(choice.copy(wrongOptions = 9), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.MIN_PASSES, 0, 1..10)),
            validate(choice.copy(minPasses = 0), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.MIN_PASSES, 11, 1..10)),
            validate(assembly.copy(minPasses = 11), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.MIN_SUCCESS_PERCENT, 101, 1..100)),
            validate(assembly.copy(minSuccessPercent = 101), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.EXTRA_LETTERS, 11, 0..10)),
            validate(assembly.copy(extraLetters = 11), *sets),
        )
        assertEquals(
            listOf(SettingsError.OutOfRange(Field.EXTRA_LETTERS, -1, 0..10)),
            validate(assembly.copy(extraLetters = -1), *sets),
        )
    }

    @Test
    fun `hodnoty nepatrici k rezimu se nekontroluji`() {
        // V režimu 2 je počet variant skrytý, v režimu 1 jsou skrytá písmena navíc.
        assertTrue(validate(assembly.copy(wrongOptions = 99), set("A", 3)).isEmpty())
        assertTrue(validate(choice.copy(extraLetters = 99), set("A", 12)).isEmpty())
    }

    @Test
    fun `vice chyb najednou`() {
        val errors = validate(choice.copy(wrongOptions = 9, minPasses = 0), set("A", 3))
        assertEquals(
            setOf(
                SettingsError.OutOfRange(Field.WRONG_OPTIONS, 9, 1..8),
                SettingsError.OutOfRange(Field.MIN_PASSES, 0, 1..10),
                SettingsError.NotEnoughWords(required = 10, available = 3),
            ),
            errors.toSet(),
        )
    }
}
