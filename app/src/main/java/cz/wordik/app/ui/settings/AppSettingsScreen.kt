package cz.wordik.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cz.wordik.app.R
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.ui.components.RadioGroup
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.WordikTheme

/** Nastavení aplikace: velikost tlačítek a písma (Normální / Velké), platí pro celou aplikaci. */
@Composable
fun AppSettingsScreen(
    sizeLevel: SizeLevel,
    onSizeLevelChange: (SizeLevel) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.settings_title),
        backLabel = stringResource(R.string.back),
        onBack = onBack,
    ) {
        Text(stringResource(R.string.settings_size_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.settings_size_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        RadioGroup(
            options = listOf(
                SizeLevel.NORMAL to stringResource(R.string.settings_size_normal),
                SizeLevel.LARGE to stringResource(R.string.settings_size_large),
            ),
            selected = sizeLevel,
            onSelect = onSizeLevelChange,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSettingsPreview() {
    WordikTheme { AppSettingsScreen(SizeLevel.NORMAL, {}, {}) }
}

@Preview(showBackground = true, name = "Velké")
@Composable
private fun AppSettingsLargePreview() {
    WordikTheme(SizeLevel.LARGE) { AppSettingsScreen(SizeLevel.LARGE, {}, {}) }
}
