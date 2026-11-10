package cz.wordik.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cz.wordik.app.R
import cz.wordik.app.domain.SizeScale
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.WordikTheme

/**
 * Nastavení aplikace: velikost tlačítek a písma na škále [SizeScale.MIN_PERCENT]–[SizeScale.MAX_PERCENT] %,
 * platí pro celou aplikaci. Hodnota se uloží (a aplikace se přepočítá) až po puštění posuvníku.
 */
@Composable
fun AppSettingsScreen(
    sizePercent: Int,
    onSizePercentChange: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var dragged by remember(sizePercent) { mutableFloatStateOf(sizePercent.toFloat()) }
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
        Text(
            stringResource(R.string.settings_size_value, SizeScale.normalize(dragged.toInt())),
            style = MaterialTheme.typography.headlineSmall,
        )
        Slider(
            value = dragged,
            onValueChange = { dragged = it },
            onValueChangeFinished = { onSizePercentChange(SizeScale.normalize(dragged.toInt())) },
            valueRange = SizeScale.MIN_PERCENT.toFloat()..SizeScale.MAX_PERCENT.toFloat(),
            steps = (SizeScale.MAX_PERCENT - SizeScale.MIN_PERCENT) / SizeScale.STEP_PERCENT - 1,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AppSettingsPreview() {
    WordikTheme { AppSettingsScreen(100, {}, {}) }
}

@Preview(showBackground = true, name = "Velké")
@Composable
private fun AppSettingsLargePreview() {
    WordikTheme(sizePercent = 160) { AppSettingsScreen(160, {}, {}) }
}
