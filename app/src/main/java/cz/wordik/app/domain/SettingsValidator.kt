package cz.wordik.app.domain

/** Důvod, proč nelze test s daným nastavením spustit. UI k nim doplní české hlášky. */
sealed interface SettingsError {
    /** Není vybraná žádná sada nebo vybrané sady nemají žádné slovíčko. */
    data object NoWords : SettingsError

    /** Režim výběru z variant potřebuje alespoň [required] slovíček (N+1), vybrané sady mají [available]. */
    data class NotEnoughWords(val required: Int, val available: Int) : SettingsError

    data class OutOfRange(val field: Field, val value: Int, val range: IntRange) : SettingsError

    enum class Field { WRONG_OPTIONS, MIN_PASSES, MIN_SUCCESS_PERCENT, EXTRA_LETTERS }
}

object SettingsValidator {
    /**
     * Zkontroluje [settings] podle [ranges] a podle [selectedSets]. Kontrolují se jen hodnoty,
     * které k vybranému režimu patří (počet variant jen v režimu 1, písmena navíc jen v režimu 2).
     * Prázdný výsledek znamená, že test lze spustit.
     */
    fun validate(
        settings: TestSettings,
        ranges: SettingsRanges,
        selectedSets: List<WordSet>,
    ): List<SettingsError> {
        val errors = mutableListOf<SettingsError>()

        fun check(field: SettingsError.Field, value: Int, range: IntRange) {
            if (value !in range) errors += SettingsError.OutOfRange(field, value, range)
        }

        check(SettingsError.Field.MIN_PASSES, settings.minPasses, ranges.minPasses)
        check(SettingsError.Field.MIN_SUCCESS_PERCENT, settings.minSuccessPercent, ranges.minSuccessPercent)
        when (settings.mode) {
            TestMode.CHOICE -> check(SettingsError.Field.WRONG_OPTIONS, settings.wrongOptions, ranges.wrongOptions)
            TestMode.ASSEMBLY -> check(SettingsError.Field.EXTRA_LETTERS, settings.extraLetters, ranges.extraLetters)
        }

        val wordCount = selectedSets.sumOf { it.words.size }
        if (wordCount == 0) {
            errors += SettingsError.NoWords
        } else if (settings.mode == TestMode.CHOICE) {
            val required = settings.wrongOptions + 1
            if (wordCount < required) errors += SettingsError.NotEnoughWords(required, wordCount)
        }
        return errors
    }
}
