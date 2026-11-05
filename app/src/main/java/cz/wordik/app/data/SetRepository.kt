package cz.wordik.app.data

import android.content.res.AssetManager
import cz.wordik.app.domain.WordSet
import cz.wordik.app.domain.WordSetParser

/** Zdroj textových souborů sad; odděleno kvůli testům bez Androidu. */
interface SetFileSource {
    /** Vrací dvojice (název souboru bez přípony, obsah souboru v UTF-8), seřazené podle názvu. */
    fun readAll(): List<Pair<String, String>>
}

/** Sady přibalené v `assets/sets/`. */
class AssetSetFileSource(
    private val assets: AssetManager,
    private val directory: String = SETS_DIRECTORY,
) : SetFileSource {
    override fun readAll(): List<Pair<String, String>> =
        assets.list(directory).orEmpty()
            .filter { it.endsWith(SET_EXTENSION, ignoreCase = true) }
            .sorted()
            .map { file ->
                val text = assets.open("$directory/$file").bufferedReader(Charsets.UTF_8).use { it.readText() }
                file.substringBeforeLast('.') to text
            }

    companion object {
        const val SETS_DIRECTORY = "sets"
        const val SET_EXTENSION = ".txt"
    }
}

class SetRepository(private val source: SetFileSource) {
    /**
     * Načte všechny sady. Chybná sada vyhodí [cz.wordik.app.domain.WordSetParseException]
     * s čísly řádků, aby šla opravit.
     */
    fun loadAll(): List<WordSet> =
        source.readAll().map { (name, text) -> WordSetParser.parse(name, text) }
}
