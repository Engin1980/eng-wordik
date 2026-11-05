package cz.wordik.app.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.wordik.app.data.SettingsStore
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.SettingsError
import cz.wordik.app.domain.SettingsRanges
import cz.wordik.app.domain.SettingsValidator
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.WordSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Výběr jedné nebo více sad pro test. */
class SetSelectionViewModel(val sets: List<WordSet>) : ViewModel() {
    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    fun toggle(name: String) {
        if (sets.none { it.name == name }) return
        _selected.update { if (name in it) it - name else it + name }
    }

    /** Vybrané názvy v pořadí, v jakém jsou sady v seznamu. */
    fun selectedNames(): List<String> = sets.map { it.name }.filter { it in _selected.value }

    fun selectedWordCount(): Int = sets.filter { it.name in _selected.value }.sumOf { it.words.size }

    fun canContinue(): Boolean = _selected.value.isNotEmpty()
}

data class TestSetupState(
    val settings: TestSettings,
    /** False, dokud se nenačte poslední uložené nastavení pro kombinaci sad. */
    val loaded: Boolean = false,
    val errors: List<SettingsError> = emptyList(),
)

/** Nastavení testu pro vybranou kombinaci sad; pamatuje si poslední použité hodnoty. */
class TestSetupViewModel(
    private val setNames: List<String>,
    private val allSets: List<WordSet>,
    private val config: AppConfig,
    private val store: SettingsStore,
) : ViewModel() {
    private val selectedSets = allSets.filter { it.name in setNames }

    val ranges: SettingsRanges get() = config.ranges
    val wordCount: Int = selectedSets.sumOf { it.words.size }

    private val _state = MutableStateFlow(TestSetupState(config.defaults))
    val state: StateFlow<TestSetupState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = TestSetupState(store.loadTestSettings(setNames), loaded = true)
        }
    }

    fun setMode(mode: TestMode) = edit { it.copy(mode = mode) }
    fun setWrongOptions(value: Int) = edit { it.copy(wrongOptions = value.coerceIn(ranges.wrongOptions)) }
    fun setMinPasses(value: Int) = edit { it.copy(minPasses = value.coerceIn(ranges.minPasses)) }
    fun setMinSuccessPercent(value: Int) = edit { it.copy(minSuccessPercent = value.coerceIn(ranges.minSuccessPercent)) }
    fun setExtraLetters(value: Int) = edit { it.copy(extraLetters = value.coerceIn(ranges.extraLetters)) }
    fun setEvaluation(evaluation: Evaluation) = edit { it.copy(evaluation = evaluation) }
    fun setDirection(direction: Direction) = edit { it.copy(direction = direction) }

    private fun edit(change: (TestSettings) -> TestSettings) =
        _state.update { it.copy(settings = change(it.settings), errors = emptyList()) }

    /**
     * Ověří nastavení; při chybě ji zobrazí a test se nespustí. Jinak nastavení uloží
     * (pro kombinaci sad) a zavolá [onStart].
     */
    fun start(onStart: (TestSettings) -> Unit) {
        val current = _state.value
        if (!current.loaded) return
        val errors = SettingsValidator.validate(current.settings, ranges, selectedSets)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(errors = errors) }
            return
        }
        viewModelScope.launch {
            store.saveTestSettings(setNames, current.settings)
            onStart(current.settings)
        }
    }
}
