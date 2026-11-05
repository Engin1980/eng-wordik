package cz.wordik.app.domain

/** Převody sady na text `česky;cizí` po řádcích (pro editor) a sloučení textů z více QR kódů. */
object WordSetText {
    /** Text sady pro editor; cizí slovíčko s rozlišením velikosti písmen se zapíše v uvozovkách. */
    fun format(set: WordSet): String =
        set.words.joinToString("\n") { w ->
            val foreign = if (w.caseSensitive) "\"${w.foreign}\"" else w.foreign
            "${w.czech};$foreign"
        }

    /**
     * Sloučí texty z více kódů do jednoho; pořadí kódů nehraje roli. Prázdné řádky se
     * vynechají a duplicity (shodné české i cizí znění) se zahodí, první výskyt zůstane.
     * Chybné řádky se ponechají, aby je uživatel viděl a opravil v editoru.
     */
    fun merge(chunks: List<String>): String {
        val seen = HashSet<String>()
        return chunks.asSequence()
            .flatMap { it.lineSequence() }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { seen.add(it.split(';').joinToString(";") { part -> part.trim() }) }
            .joinToString("\n")
    }

    /** Počet neprázdných řádků textu. */
    fun lineCount(text: String): Int = text.lineSequence().count { it.isNotBlank() }
}
