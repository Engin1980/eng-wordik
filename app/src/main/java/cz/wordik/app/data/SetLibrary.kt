package cz.wordik.app.data

import cz.wordik.app.domain.LineError
import cz.wordik.app.domain.SetNameError
import cz.wordik.app.domain.SetNameValidator
import cz.wordik.app.domain.WordSet
import cz.wordik.app.domain.WordSetParseException
import cz.wordik.app.domain.WordSetParser
import cz.wordik.app.domain.WordSetText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SaveResult {
    data class Saved(val set: WordSet) : SaveResult
    data class Invalid(val errors: List<LineError>) : SaveResult
}

/**
 * Sady uživatele: načtení, vytvoření, uložení a smazání. Při prvním spuštění zkopíruje
 * přibalené sady ([bundled]) mezi uživatelské; po smazání se znovu neobjeví.
 *
 * @throws WordSetParseException při vytvoření, pokud je některý uložený soubor chybný
 */
class SetLibrary(private val storage: SetStorage, bundled: SetFileSource? = null) {
    init {
        if (bundled != null && !storage.bundledInstalled) {
            val existing = storage.readAll().map { it.first.lowercase() }.toSet()
            bundled.readAll().filter { it.first.lowercase() !in existing }.forEach { (name, text) -> storage.write(name, text) }
            storage.bundledInstalled = true
        }
    }

    private val _sets = MutableStateFlow(load())
    val sets: StateFlow<List<WordSet>> = _sets.asStateFlow()

    private fun load(): List<WordSet> = SetRepository(storage).loadAll()

    private fun reload() {
        _sets.value = load()
    }

    /** Chyba názvu pro novou sadu, nebo null. */
    fun nameError(name: String): SetNameError? = SetNameValidator.validate(name, _sets.value.map { it.name })

    fun find(name: String): WordSet? = _sets.value.firstOrNull { it.name == name }

    /** Text sady pro editor. */
    fun text(name: String): String = find(name)?.let(WordSetText::format).orEmpty()

    /** Založí prázdnou sadu; název musí projít [nameError]. */
    fun create(name: String): WordSet {
        val trimmed = name.trim()
        require(nameError(trimmed) == null) { "Neplatný název sady" }
        storage.write(trimmed, "")
        reload()
        return find(trimmed)!!
    }

    /** Uloží text sady (vytvoří ji nebo přepíše); chybný text se neuloží a vrátí chyby s čísly řádků. */
    fun save(name: String, text: String): SaveResult {
        val parsed = try {
            WordSetParser.parse(name, text)
        } catch (e: WordSetParseException) {
            return SaveResult.Invalid(e.errors)
        }
        storage.write(name, WordSetText.format(parsed))
        reload()
        return SaveResult.Saved(find(name) ?: parsed)
    }

    fun delete(name: String) {
        storage.delete(name)
        reload()
    }
}
