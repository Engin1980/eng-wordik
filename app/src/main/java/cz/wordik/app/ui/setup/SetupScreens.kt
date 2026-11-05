package cz.wordik.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.wordik.app.R
import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.SettingsError
import cz.wordik.app.domain.SettingsRanges
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.Word
import cz.wordik.app.domain.WordSet
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.NumberStepper
import cz.wordik.app.ui.components.RadioGroup
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.LocalSizes
import cz.wordik.app.ui.theme.WordikTheme

@Composable
fun SetSelectionScreen(
    viewModel: SetSelectionViewModel,
    onContinue: (List<String>) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    SetSelectionContent(
        sets = viewModel.sets,
        selected = selected,
        selectedWordCount = viewModel.selectedWordCount(),
        onToggle = viewModel::toggle,
        onContinue = { onContinue(viewModel.selectedNames()) },
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun SetSelectionContent(
    sets: List<WordSet>,
    selected: Set<String>,
    selectedWordCount: Int,
    onToggle: (String) -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.select_title),
        actions = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleMedium)
            }
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Text(
                    stringResource(R.string.select_summary, selected.size, selectedWordCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BigButton(
                text = stringResource(R.string.select_continue),
                onClick = onContinue,
                enabled = selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Text(stringResource(R.string.select_hint), style = MaterialTheme.typography.bodyLarge)
        sets.forEach { set ->
            val checked = set.name in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalSizes.current.buttonHeight)
                    .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(set.name) })
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(set.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        pluralStringResource(R.plurals.word_count, set.words.size, set.words.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun TestSetupScreen(
    viewModel: TestSetupViewModel,
    setNames: List<String>,
    onStart: (TestSettings) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TestSetupContent(
        setNames = setNames,
        settings = state.settings,
        ranges = viewModel.ranges,
        errors = state.errors,
        enabled = state.loaded,
        callbacks = SetupCallbacks(
            onMode = viewModel::setMode,
            onWrongOptions = viewModel::setWrongOptions,
            onMinPasses = viewModel::setMinPasses,
            onMinSuccess = viewModel::setMinSuccessPercent,
            onExtraLetters = viewModel::setExtraLetters,
            onEvaluation = viewModel::setEvaluation,
            onDirection = viewModel::setDirection,
        ),
        onStart = { viewModel.start(onStart) },
        onBack = onBack,
    )
}

data class SetupCallbacks(
    val onMode: (TestMode) -> Unit,
    val onWrongOptions: (Int) -> Unit,
    val onMinPasses: (Int) -> Unit,
    val onMinSuccess: (Int) -> Unit,
    val onExtraLetters: (Int) -> Unit,
    val onEvaluation: (Evaluation) -> Unit,
    val onDirection: (Direction) -> Unit,
)

@Composable
fun TestSetupContent(
    setNames: List<String>,
    settings: TestSettings,
    ranges: SettingsRanges,
    errors: List<SettingsError>,
    enabled: Boolean,
    callbacks: SetupCallbacks,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.setup_title),
        backLabel = stringResource(R.string.back),
        onBack = onBack,
        bottomBar = {
            errors.forEach { error ->
                Text(
                    settingsErrorText(error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            BigButton(stringResource(R.string.setup_start), onStart, Modifier.fillMaxWidth(), enabled = enabled)
        },
    ) {
        Text(
            stringResource(R.string.setup_sets, setNames.joinToString(", ")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionTitle(stringResource(R.string.setup_mode))
        RadioGroup(
            options = listOf(
                TestMode.CHOICE to stringResource(R.string.mode_choice),
                TestMode.ASSEMBLY to stringResource(R.string.mode_assembly),
            ),
            selected = settings.mode,
            onSelect = callbacks.onMode,
        )

        // Volby, které k vybranému režimu nepatří, se skryjí.
        when (settings.mode) {
            TestMode.CHOICE -> {
                NumberStepper(
                    label = stringResource(R.string.setup_wrong_options),
                    value = settings.wrongOptions,
                    range = ranges.wrongOptions,
                    onChange = callbacks.onWrongOptions,
                )
                SectionTitle(stringResource(R.string.setup_direction))
                RadioGroup(
                    options = listOf(
                        Direction.CZECH_TO_FOREIGN to stringResource(R.string.direction_cz_foreign),
                        Direction.FOREIGN_TO_CZECH to stringResource(R.string.direction_foreign_cz),
                        Direction.MIX to stringResource(R.string.direction_mix),
                    ),
                    selected = settings.direction,
                    onSelect = callbacks.onDirection,
                )
            }
            TestMode.ASSEMBLY -> {
                NumberStepper(
                    label = stringResource(R.string.setup_extra_letters),
                    value = settings.extraLetters,
                    range = ranges.extraLetters,
                    onChange = callbacks.onExtraLetters,
                )
                SectionTitle(stringResource(R.string.setup_evaluation))
                RadioGroup(
                    options = listOf(
                        Evaluation.PER_LETTER to stringResource(R.string.evaluation_letters),
                        Evaluation.WHOLE_WORD to stringResource(R.string.evaluation_word),
                    ),
                    selected = settings.evaluation,
                    onSelect = callbacks.onEvaluation,
                )
            }
        }

        NumberStepper(
            label = stringResource(R.string.setup_min_passes),
            value = settings.minPasses,
            range = ranges.minPasses,
            onChange = callbacks.onMinPasses,
        )
        NumberStepper(
            label = stringResource(R.string.setup_min_success),
            value = settings.minSuccessPercent,
            range = ranges.minSuccessPercent,
            step = 5,
            suffix = " %",
            onChange = callbacks.onMinSuccess,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun fieldName(field: SettingsError.Field) = when (field) {
    SettingsError.Field.WRONG_OPTIONS -> stringResource(R.string.setup_wrong_options)
    SettingsError.Field.MIN_PASSES -> stringResource(R.string.setup_min_passes)
    SettingsError.Field.MIN_SUCCESS_PERCENT -> stringResource(R.string.setup_min_success)
    SettingsError.Field.EXTRA_LETTERS -> stringResource(R.string.setup_extra_letters)
}

@Composable
fun settingsErrorText(error: SettingsError): String = when (error) {
    SettingsError.NoWords -> stringResource(R.string.error_no_words)
    is SettingsError.NotEnoughWords ->
        stringResource(R.string.error_not_enough_words, error.required, error.available)
    is SettingsError.OutOfRange ->
        stringResource(R.string.error_out_of_range, fieldName(error.field), error.range.first, error.range.last)
}

private val previewSets = listOf(
    WordSet("Zvířata", List(12) { Word("pes$it", "dog$it") }),
    WordSet("Škola a domov", List(1) { Word("škola", "school") }),
)

@Preview(showBackground = true)
@Composable
private fun SetSelectionPreview() {
    WordikTheme {
        SetSelectionContent(previewSets, setOf("Zvířata"), 12, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400, name = "Tablet na šířku")
@Composable
private fun TestSetupPreview() {
    WordikTheme {
        TestSetupContent(
            setNames = listOf("Zvířata"),
            settings = TestSettings(TestMode.CHOICE, 4, 2, 50, 0, Evaluation.PER_LETTER, Direction.MIX),
            ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10),
            errors = listOf(SettingsError.NotEnoughWords(5, 3)),
            enabled = true,
            callbacks = SetupCallbacks({}, {}, {}, {}, {}, {}, {}),
            onStart = {},
            onBack = {},
        )
    }
}
