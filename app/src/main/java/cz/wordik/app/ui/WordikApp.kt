package cz.wordik.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cz.wordik.app.AppContainer
import cz.wordik.app.R
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestResult
import cz.wordik.app.domain.WordSet
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.results.ResultsScreen
import cz.wordik.app.ui.settings.AppSettingsScreen
import cz.wordik.app.ui.setup.QrImportViewModel
import cz.wordik.app.ui.setup.QrScanScreen
import cz.wordik.app.ui.setup.SetEditorScreen
import cz.wordik.app.ui.setup.SetEditorViewModel
import cz.wordik.app.ui.setup.SetSelectionScreen
import cz.wordik.app.ui.setup.SetSelectionViewModel
import cz.wordik.app.ui.setup.TestSetupScreen
import cz.wordik.app.ui.setup.TestSetupViewModel
import cz.wordik.app.ui.test.AssemblyTestScreen
import cz.wordik.app.ui.test.AssemblyTestViewModel
import cz.wordik.app.ui.test.ChoiceTestScreen
import cz.wordik.app.ui.test.ChoiceTestViewModel
import cz.wordik.app.ui.theme.WordikTheme

/** Kořen aplikace: téma podle volby velikosti a zásobník obrazovek. */
@Composable
fun WordikApp(container: AppContainer) {
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory { initializer { AppViewModel(container.settingsStore) } },
    )
    val stack by appViewModel.stack.collectAsStateWithLifecycle()
    val sizeLevel by appViewModel.sizeLevel.collectAsStateWithLifecycle()
    val sets by container.library.sets.collectAsStateWithLifecycle()

    WordikTheme(sizeLevel = sizeLevel, fontName = container.config.fontName) {
        BackHandler(enabled = stack.size > 1) { appViewModel.back() }

        when (val screen = stack.last()) {
            Screen.AppSettings -> AppSettingsScreen(
                sizeLevel = sizeLevel,
                onSizeLevelChange = appViewModel::setSizeLevel,
                onBack = { appViewModel.back() },
            )
            Screen.SetSelection -> {
                val selectionViewModel: SetSelectionViewModel = viewModel(
                    factory = viewModelFactory { initializer { SetSelectionViewModel(container.library.sets, container.library) } },
                )
                SetSelectionScreen(
                    viewModel = selectionViewModel,
                    onContinue = { appViewModel.open(Screen.TestSetup(it)) },
                    onOpenSettings = { appViewModel.open(Screen.AppSettings) },
                    onEdit = { name, text -> appViewModel.open(Screen.EditSet(name, text, isNew = false)) },
                    onCreated = { appViewModel.open(Screen.EditSet(it, "", isNew = true)) },
                    onImport = { appViewModel.open(Screen.ImportScan(it)) },
                )
            }
            is Screen.TestSetup -> {
                val setupViewModel: TestSetupViewModel = viewModel(
                    key = "setup:" + screen.setNames.joinToString("|"),
                    factory = viewModelFactory {
                        initializer {
                            TestSetupViewModel(screen.setNames, sets, container.config, container.settingsStore)
                        }
                    },
                )
                TestSetupScreen(
                    viewModel = setupViewModel,
                    setNames = screen.setNames,
                    onStart = { appViewModel.startTest(screen.setNames, it) },
                    onBack = { appViewModel.back() },
                )
            }
            is Screen.EditSet -> {
                val editorViewModel: SetEditorViewModel = viewModel(
                    key = "edit:" + screen.name + ":" + screen.isNew,
                    factory = viewModelFactory {
                        initializer { SetEditorViewModel(screen.name, screen.initialText, container.library) }
                    },
                )
                SetEditorScreen(editorViewModel, onClose = { appViewModel.back() })
            }
            is Screen.ImportScan -> {
                val importViewModel: QrImportViewModel = viewModel(
                    key = "import:" + screen.name,
                    factory = viewModelFactory { initializer { QrImportViewModel(screen.name) } },
                )
                QrScanScreen(
                    viewModel = importViewModel,
                    onDone = { text -> appViewModel.replaceTop(Screen.EditSet(screen.name, text, isNew = true)) },
                    onBack = { appViewModel.back() },
                )
            }
            is Screen.Test -> TestHost(screen, sets, container, appViewModel)
            is Screen.Results -> ResultsScreen(
                result = screen.result,
                onRepeat = { appViewModel.repeatTest(screen.setNames, screen.settings) },
                onBackToSelection = { appViewModel.backToSelection() },
                onBack = { appViewModel.back() },
            )
        }
    }
}

/** Spustí test podle zvoleného režimu; každý běh ([Screen.Test.runId]) má vlastní ViewModel. */
@Composable
private fun TestHost(screen: Screen.Test, sets: List<WordSet>, container: AppContainer, appViewModel: AppViewModel) {
    val words = sets.filter { it.name in screen.setNames }.flatMap { it.words }
    val finish: (TestResult) -> Unit = { appViewModel.showResults(screen.setNames, screen.settings, it) }
    when (screen.settings.mode) {
        TestMode.CHOICE -> {
            val vm: ChoiceTestViewModel = viewModel(
                key = "test:${screen.runId}",
                factory = viewModelFactory {
                    initializer { ChoiceTestViewModel(words, screen.settings, container.config, sounds = container.soundPlayer) }
                },
            )
            ChoiceTestScreen(vm, onFinished = finish, onAbandon = { appViewModel.back() })
        }
        TestMode.ASSEMBLY -> {
            val vm: AssemblyTestViewModel = viewModel(
                key = "test:${screen.runId}",
                factory = viewModelFactory {
                    initializer { AssemblyTestViewModel(words, screen.settings, container.config, sounds = container.soundPlayer) }
                },
            )
            AssemblyTestScreen(vm, onFinished = finish)
        }
    }
}

/** Chyba v přibalených datech (konfigurace nebo sada). */
@Composable
fun StartupErrorScreen(error: Throwable) {
    WordikTheme {
        ScreenScaffold(title = stringResource(R.string.startup_error_title)) {
            Text(stringResource(R.string.startup_error_hint), style = MaterialTheme.typography.bodyLarge)
            Column { Text(error.message ?: error.toString(), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}
