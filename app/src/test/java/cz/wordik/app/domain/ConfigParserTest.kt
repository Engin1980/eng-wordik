package cz.wordik.app.domain

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ConfigParserTest {
    private val valid = """
        default.mode=ASSEMBLY
        default.wrongOptions=4
        default.minPasses=2
        default.minSuccessPercent=50
        default.extraLetters=0
        default.evaluation=PER_LETTER
        default.direction=MIX
        range.wrongOptions=1-8
        range.minPasses=1-10
        range.minSuccessPercent=1-100
        range.extraLetters=0-10
        autoAdvanceMillis=1000
        font=Atkinson Hyperlegible
    """.trimIndent()

    private fun assertConfigError(text: String, messagePart: String) {
        try {
            ConfigParser.parse(text)
            fail("Očekávala se ConfigException")
        } catch (e: ConfigException) {
            assertTrue("Zpráva „${e.message}“ neobsahuje „$messagePart“", e.message!!.contains(messagePart))
        }
    }

    private fun without(key: String) = valid.lines().filterNot { it.startsWith("$key=") }.joinToString("\n")

    private fun replaced(key: String, value: String) =
        valid.lines().joinToString("\n") { if (it.startsWith("$key=")) "$key=$value" else it }

    @Test
    fun `nacte vsechny hodnoty`() {
        val config = ConfigParser.parse(valid)
        assertEquals(
            TestSettings(
                mode = TestMode.ASSEMBLY,
                wrongOptions = 4,
                minPasses = 2,
                minSuccessPercent = 50,
                extraLetters = 0,
                evaluation = Evaluation.PER_LETTER,
                direction = Direction.MIX,
            ),
            config.defaults,
        )
        assertEquals(1..8, config.ranges.wrongOptions)
        assertEquals(1..10, config.ranges.minPasses)
        assertEquals(1..100, config.ranges.minSuccessPercent)
        assertEquals(0..10, config.ranges.extraLetters)
        assertEquals(1000L, config.autoAdvanceMillis)
        assertEquals("Atkinson Hyperlegible", config.fontName)
    }

    @Test
    fun `skutecny konfiguracni soubor z assets jde nacist a odpovida zadani`() {
        val file = File("src/main/assets/config.properties")
        assertTrue("Soubor neexistuje: ${file.absolutePath}", file.isFile)
        val config = ConfigParser.parse(file.readText(Charsets.UTF_8))
        assertEquals(TestMode.ASSEMBLY, config.defaults.mode)
        assertEquals(4, config.defaults.wrongOptions)
        assertEquals(2, config.defaults.minPasses)
        assertEquals(50, config.defaults.minSuccessPercent)
        assertEquals(0, config.defaults.extraLetters)
        assertEquals(Evaluation.PER_LETTER, config.defaults.evaluation)
        assertEquals(Direction.MIX, config.defaults.direction)
        assertEquals(1..8, config.ranges.wrongOptions)
        assertEquals(1..10, config.ranges.minPasses)
        assertEquals(1..100, config.ranges.minSuccessPercent)
        assertEquals(0..10, config.ranges.extraLetters)
    }

    @Test
    fun `vychozi hodnoty na hranicich rozsahu jsou platne`() {
        val config = ConfigParser.parse(
            replaced("default.wrongOptions", "8").let {
                it.replace("default.minPasses=2", "default.minPasses=1")
                    .replace("default.minSuccessPercent=50", "default.minSuccessPercent=100")
                    .replace("default.extraLetters=0", "default.extraLetters=10")
            },
        )
        assertEquals(8, config.defaults.wrongOptions)
        assertEquals(1, config.defaults.minPasses)
        assertEquals(100, config.defaults.minSuccessPercent)
        assertEquals(10, config.defaults.extraLetters)
    }

    @Test
    fun `chybejici hodnota je chyba s nazvem klice`() {
        listOf(
            "default.mode", "default.wrongOptions", "default.minPasses", "default.minSuccessPercent",
            "default.extraLetters", "default.evaluation", "default.direction", "range.wrongOptions",
            "range.minPasses", "range.minSuccessPercent", "range.extraLetters", "autoAdvanceMillis", "font",
        ).forEach { assertConfigError(without(it), it) }
    }

    @Test
    fun `prazdna hodnota je jako chybejici`() {
        assertConfigError(replaced("font", "   "), "font")
    }

    @Test
    fun `neciselna hodnota`() {
        assertConfigError(replaced("default.minPasses", "dva"), "default.minPasses")
        assertConfigError(replaced("autoAdvanceMillis", "1.5"), "autoAdvanceMillis")
    }

    @Test
    fun `neznamy nazev v enumu`() {
        assertConfigError(replaced("default.mode", "MODE3"), "default.mode")
        assertConfigError(replaced("default.evaluation", "per_letter"), "default.evaluation")
        assertConfigError(replaced("default.direction", "CZ"), "default.direction")
    }

    @Test
    fun `chybny tvar rozsahu`() {
        assertConfigError(replaced("range.minPasses", "5"), "range.minPasses")
        assertConfigError(replaced("range.minPasses", "a-b"), "range.minPasses")
        assertConfigError(replaced("range.minPasses", "10-1"), "range.minPasses")
        assertConfigError(replaced("range.minPasses", "1-5-9"), "range.minPasses")
    }

    @Test
    fun `vychozi hodnota mimo rozsah`() {
        assertConfigError(replaced("default.wrongOptions", "9"), "default.wrongOptions")
        assertConfigError(replaced("default.wrongOptions", "0"), "default.wrongOptions")
        assertConfigError(replaced("default.minPasses", "11"), "default.minPasses")
        assertConfigError(replaced("default.minSuccessPercent", "0"), "default.minSuccessPercent")
        assertConfigError(replaced("default.minSuccessPercent", "101"), "default.minSuccessPercent")
        assertConfigError(replaced("default.extraLetters", "11"), "default.extraLetters")
        assertConfigError(replaced("default.extraLetters", "-1"), "default.extraLetters")
    }

    @Test
    fun `zaporny interval automatickeho prechodu`() {
        assertConfigError(replaced("autoAdvanceMillis", "-5"), "autoAdvanceMillis")
    }

    @Test
    fun `komentare a prazdne radky se ignoruji`() {
        val config = ConfigParser.parse("# komentář\n\n$valid\n! jiný komentář\n")
        assertEquals(1000L, config.autoAdvanceMillis)
    }
}
