package cz.wordik.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.wordik.app.data.SettingsStore
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.domain.TestResult
import cz.wordik.app.domain.TestSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Obrazovky aplikace: výběr sad → nastavení testu → test → výsledky. */
sealed interface Screen {
    data object SetSelection : Screen
    data object AppSettings : Screen

    /** Editace textu sady; [isNew] jen označuje čerstvě vytvořenou nebo importovanou sadu. */
    data class EditSet(val name: String, val initialText: String, val isNew: Boolean) : Screen

    /** Skenování QR kódů pro import sady s daným názvem. */
    data class ImportScan(val name: String) : Screen

    /** Nastavení testu pro vybranou kombinaci sad. */
    data class TestSetup(val setNames: List<String>) : Screen

    /** [runId] odliší jednotlivé běhy, aby opakovaný test dostal nový stav. */
    data class Test(val setNames: List<String>, val settings: TestSettings, val runId: Int) : Screen

    data class Results(val setNames: List<String>, val settings: TestSettings, val result: TestResult) : Screen
}

/**
 * Drží zásobník obrazovek a volbu velikosti. Přežije otočení displeje. Rozběhnutý test
 * se po zničení procesu neobnovuje (přerušený test nelze obnovit).
 */
class AppViewModel(private val store: SettingsStore) : ViewModel() {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.SetSelection))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    val current: Screen get() = _stack.value.last()

    val sizeLevel: StateFlow<SizeLevel> =
        store.sizeLevel.stateIn(viewModelScope, SharingStarted.Eagerly, SizeLevel.NORMAL)

    private var runCounter = 0

    fun open(screen: Screen) = _stack.update { it + screen }

    /** Vrací se o obrazovku zpět; z první obrazovky se nevrací (false). */
    fun back(): Boolean {
        if (_stack.value.size <= 1) return false
        _stack.update { it.dropLast(1) }
        return true
    }

    /** Nahradí aktuální obrazovku jinou (např. skener editorem naskenovaného textu). */
    fun replaceTop(screen: Screen) = _stack.update { it.dropLast(1) + screen }

    fun backToSelection() = _stack.update { listOf(Screen.SetSelection) }

    fun startTest(setNames: List<String>, settings: TestSettings) =
        open(Screen.Test(setNames, settings, ++runCounter))

    /** Test skončil nebo byl ukončen: běžící obrazovka testu se nahradí výsledky. */
    fun showResults(setNames: List<String>, settings: TestSettings, result: TestResult) =
        _stack.update { it.dropLast(1) + Screen.Results(setNames, settings, result) }

    /** Opakování testu se stejným nastavením. */
    fun repeatTest(setNames: List<String>, settings: TestSettings) =
        _stack.update { it.dropLast(1) + Screen.Test(setNames, settings, ++runCounter) }

    fun setSizeLevel(level: SizeLevel) {
        viewModelScope.launch { store.saveSizeLevel(level) }
    }
}
