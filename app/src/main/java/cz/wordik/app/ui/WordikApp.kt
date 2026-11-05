package cz.wordik.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.settings.AppSettingsScreen
import cz.wordik.app.ui.theme.WordikTheme

/** Kořen aplikace: téma podle volby velikosti a zásobník obrazovek. */
@Composable
fun WordikApp(container: AppContainer) {
    val appViewModel: AppViewModel = viewModel(
        factory = viewModelFactory { initializer { AppViewModel(container.settingsStore) } },
    )
    val stack by appViewModel.stack.collectAsStateWithLifecycle()
    val sizeLevel by appViewModel.sizeLevel.collectAsStateWithLifecycle()

    WordikTheme(sizeLevel = sizeLevel, fontName = container.config.fontName) {
        BackHandler(enabled = stack.size > 1) { appViewModel.back() }

        when (val screen = stack.last()) {
            Screen.AppSettings -> AppSettingsScreen(
                sizeLevel = sizeLevel,
                onSizeLevelChange = appViewModel::setSizeLevel,
                onBack = { appViewModel.back() },
            )
            else -> PlaceholderScreen(screen) { appViewModel.open(Screen.AppSettings) }
        }
    }
}

/** Dočasná obrazovka, než vzniknou skutečné (kroky 9 až 12). */
@Composable
private fun PlaceholderScreen(screen: Screen, onOpenSettings: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.app_name)) {
        Text(screen::class.simpleName.orEmpty(), style = MaterialTheme.typography.headlineMedium)
        BigButton(stringResource(R.string.placeholder_open_settings), onOpenSettings, Modifier.fillMaxWidth())
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
