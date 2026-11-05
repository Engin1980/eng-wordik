package cz.wordik.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.wordik.app.R
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.LocalSizes
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
        SizeOptions(sizeLevel, onSizeLevelChange)
    }
}

@Composable
private fun SizeOptions(selected: SizeLevel, onSelect: (SizeLevel) -> Unit) {
    Column(
        modifier = Modifier.selectableGroup().fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SizeLevel.entries.forEach { level ->
            val label = when (level) {
                SizeLevel.NORMAL -> stringResource(R.string.settings_size_normal)
                SizeLevel.LARGE -> stringResource(R.string.settings_size_large)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalSizes.current.buttonHeight)
                    .selectable(selected = level == selected, onClick = { onSelect(level) }, role = Role.RadioButton)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = level == selected, onClick = null)
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
        }
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
