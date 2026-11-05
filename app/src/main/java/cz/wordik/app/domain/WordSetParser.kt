package cz.wordik.app.domain

/** Chyba na jednom řádku souboru sady; [lineNumber] se počítá od 1. */
data class LineError(val lineNumber: Int, val line: String, val message: String) {
    override fun toString() = "řádek $lineNumber: $message („$line“)"
}

/** Soubor sady nejde zpracovat; [errors] obsahuje všechny nalezené chyby najednou. */
class WordSetParseException(val setName: String, val errors: List<LineError>) :
    Exception("Sada „$setName“ má chyby: " + errors.joinToString("; "))

/** Parser formátu `česky;cizí`, jeden řádek = jedno slovíčko. */
object WordSetParser {
    private const val SEPARATOR = ';'
    private val BOM = 0xFEFF.toChar()

    // Dvojice otevírací a zavírací uvozovky (rovné, anglické, české).
    private val quotePairs = listOf('"' to '"', '“' to '”', '„' to '“', '„' to '”')

    /** @throws WordSetParseException pokud některý řádek není platný. */
    fun parse(name: String, text: String): WordSet {
        val words = mutableListOf<Word>()
        val errors = mutableListOf<LineError>()

        text.removePrefix(BOM.toString()).lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            when (val result = parseLine(line)) {
                is LineResult.Ok -> words += result.word
                is LineResult.Error -> errors += LineError(index + 1, line, result.message)
            }
        }

        if (errors.isNotEmpty()) throw WordSetParseException(name, errors)
        return WordSet(name, words)
    }

    private sealed interface LineResult {
        data class Ok(val word: Word) : LineResult
        data class Error(val message: String) : LineResult
    }

    private fun parseLine(line: String): LineResult {
        val parts = line.split(SEPARATOR)
        if (parts.size < 2) return LineResult.Error("chybí oddělovač „;“")
        if (parts.size > 2) return LineResult.Error("oddělovač „;“ smí být na řádku jen jednou")

        val czech = parts[0].trim()
        var foreign = parts[1].trim()
        if (czech.isEmpty()) return LineResult.Error("chybí české slovíčko")
        if (foreign.isEmpty()) return LineResult.Error("chybí cizí slovíčko")

        var caseSensitive = false
        if (foreign.length >= 2 && quotePairs.any { (open, close) -> foreign.first() == open && foreign.last() == close }) {
            foreign = foreign.substring(1, foreign.length - 1).trim()
            caseSensitive = true
            if (foreign.isEmpty()) return LineResult.Error("v uvozovkách chybí cizí slovíčko")
        }
        return LineResult.Ok(Word(czech, foreign, caseSensitive))
    }
}
