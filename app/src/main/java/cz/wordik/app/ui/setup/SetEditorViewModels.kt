package cz.wordik.app.ui.setup

import androidx.lifecycle.ViewModel
import cz.wordik.app.data.SaveResult
import cz.wordik.app.data.SetLibrary
import cz.wordik.app.domain.LineError
import cz.wordik.app.domain.WordSetText
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SetEditorState(
    val text: String,
    /** Chyby z posledního pokusu o uložení; mizí při další úpravě textu. */
    val errors: List<LineError> = emptyList(),
) {
    /** Řádky s chybou, pro zvýraznění. */
    val errorLines: Set<Int> get() = errors.map { it.lineNumber }.toSet()
}

/** Jednoduchý textový editor sady: uložit (jen platný text) nebo zahodit změny. */
class SetEditorViewModel(
    val name: String,
    private val initialText: String,
    private val library: SetLibrary,
) : ViewModel() {
    private val _state = MutableStateFlow(SetEditorState(initialText))
    val state: StateFlow<SetEditorState> = _state.asStateFlow()

    /** Text se liší od původního; zahození pak vyžaduje potvrzení. */
    val isDirty: Boolean get() = _state.value.text != initialText

    fun onTextChange(text: String) = _state.update { it.copy(text = text, errors = emptyList()) }

    /** Uloží text; vrací true při úspěchu. Chybný text se neuloží a chyby se zobrazí. */
    fun save(): Boolean =
        when (val result = library.save(name, _state.value.text)) {
            is SaveResult.Saved -> true
            is SaveResult.Invalid -> {
                _state.update { it.copy(errors = result.errors) }
                false
            }
        }
}

data class QrImportState(
    /** Počet různých naskenovaných kódů. */
    val codes: Int = 0,
    /** Počet slovíček (řádků) po sloučení a odstranění duplicit. */
    val words: Int = 0,
)

/** Sbírá texty z QR kódů; stejný kód naskenovaný opakovaně se počítá jednou. */
class QrImportViewModel(val name: String) : ViewModel() {
    private val chunks = LinkedHashSet<String>()

    private val _state = MutableStateFlow(QrImportState())
    val state: StateFlow<QrImportState> = _state.asStateFlow()

    /** Přidá text kódu; vrací true, pokud byl kód nový. */
    fun onCode(text: String): Boolean {
        if (text.isBlank() || !chunks.add(text)) return false
        _state.value = QrImportState(chunks.size, WordSetText.lineCount(merged()))
        return true
    }

    /** Sloučený text všech kódů bez duplicit. */
    fun merged(): String = WordSetText.merge(chunks.toList())
}
