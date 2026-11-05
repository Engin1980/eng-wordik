package cz.wordik.app.domain

/** Velikost tlačítek a písma v celé aplikaci. */
enum class SizeLevel { NORMAL, LARGE }

/** Převody mezi nastavením a textem, ve kterém se ukládá. */
object SettingsCodec {
    private const val VERSION = "v1"
    private const val SEPARATOR = ","
    private const val FIELD_COUNT = 7

    /**
     * Klíč kombinace sad: nezávisí na pořadí výběru a jednoznačně odliší i názvy
     * obsahující oddělovače (každý název nese svou délku).
     */
    fun combinationKey(setNames: Collection<String>): String =
        setNames.distinct().sorted().joinToString("|") { "${it.length}:$it" }

    fun encode(settings: TestSettings): String = listOf(
        VERSION,
        settings.mode.name,
        settings.wrongOptions,
        settings.minPasses,
        settings.minSuccessPercent,
        settings.extraLetters,
        settings.evaluation.name,
        settings.direction.name,
    ).joinToString(SEPARATOR)

    /**
     * Vrací null, pokud je text poškozený, má jinou verzi, neznámou hodnotu
     * nebo pokud hodnoty (u polí, která k režimu patří) leží mimo [ranges].
     */
    fun decode(text: String, ranges: SettingsRanges): TestSettings? {
        val parts = text.split(SEPARATOR)
        if (parts.size != FIELD_COUNT + 1 || parts[0] != VERSION) return null

        val mode = TestMode.entries.firstOrNull { it.name == parts[1] } ?: return null
        val wrongOptions = parts[2].toIntOrNull() ?: return null
        val minPasses = parts[3].toIntOrNull() ?: return null
        val minSuccessPercent = parts[4].toIntOrNull() ?: return null
        val extraLetters = parts[5].toIntOrNull() ?: return null
        val evaluation = Evaluation.entries.firstOrNull { it.name == parts[6] } ?: return null
        val direction = Direction.entries.firstOrNull { it.name == parts[7] } ?: return null

        // Skrytá pole (podle režimu) se nekontrolují, ale musí být čitelná jako čísla.
        if (minPasses !in ranges.minPasses || minSuccessPercent !in ranges.minSuccessPercent) return null
        if (mode == TestMode.CHOICE && wrongOptions !in ranges.wrongOptions) return null
        if (mode == TestMode.ASSEMBLY && extraLetters !in ranges.extraLetters) return null

        return TestSettings(mode, wrongOptions, minPasses, minSuccessPercent, extraLetters, evaluation, direction)
    }
}
