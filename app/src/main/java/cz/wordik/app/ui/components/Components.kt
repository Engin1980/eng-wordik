package cz.wordik.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.ui.theme.LocalSizes
import cz.wordik.app.ui.theme.WordikTheme

/** Nejširší obsah na tabletu; na telefonu zabere celou šířku. */
val MaxContentWidth = 720.dp

private val ButtonShape = RoundedCornerShape(16.dp)

/** Hlavní tlačítko; výška podle volby Normální / Velké. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = LocalSizes.current.buttonHeight),
        enabled = enabled,
        shape = ButtonShape,
        colors = colors,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/** Vedlejší tlačítko s obrysem. */
@Composable
fun BigOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = LocalSizes.current.buttonHeight),
        enabled = enabled,
        shape = ButtonShape,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}

/**
 * Kostra obrazovky: hlavička s názvem, volitelným tlačítkem zpět a akcemi,
 * obsah vystředěný a omezený na [MaxContentWidth] (telefon i tablet, na výšku i na šířku).
 * Při nedostatku místa (např. na šířku) se obsah posouvá.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    backLabel: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollable: Boolean = true,
    bottomBar: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { inner ->
        Column(
            modifier = Modifier.padding(inner).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null && backLabel != null) {
                    TextButton(onClick = onBack) {
                        Text("‹ $backLabel", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                actions()
            }
            val contentModifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .weight(1f)
                .let { if (scrollable) it.verticalScroll(rememberScrollState()) else it }
                .padding(horizontal = 16.dp, vertical = 8.dp)
            Column(
                modifier = contentModifier,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
            // Pevná spodní část (např. hlavní tlačítko), neposouvá se s obsahem.
            Column(
                modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = bottomBar,
            )
        }
    }
}

/** Skupina přepínačů s velkými dotykovými plochami. */
@Composable
fun <T> RadioGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup().fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LocalSizes.current.buttonHeight)
                    .selectable(selected = value == selected, onClick = { onSelect(value) }, role = Role.RadioButton)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = value == selected, onClick = null)
                Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

/** Nastavení čísla tlačítky − a +; hodnota zůstává v rozsahu [range]. */
@Composable
fun NumberStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    suffix: String = "",
) {
    val size = LocalSizes.current.buttonHeight
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = { onChange((value - step).coerceIn(range)) },
            enabled = value > range.first,
            modifier = Modifier.size(size),
            contentPadding = PaddingValues(0.dp),
            shape = ButtonShape,
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp).padding(horizontal = 4.dp),
        )
        OutlinedButton(
            onClick = { onChange((value + step).coerceIn(range)) },
            enabled = value < range.last,
            modifier = Modifier.size(size),
            contentPadding = PaddingValues(0.dp),
            shape = ButtonShape,
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}

@Preview(showBackground = true)
@Composable
private fun BigButtonPreview() {
    WordikTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("Pokračovat", {}, Modifier.fillMaxWidth())
                BigOutlinedButton("Zpět", {}, Modifier.fillMaxWidth())
                Spacer(Modifier.padding(4.dp))
            }
        }
    }
}

@Preview(showBackground = true, name = "Velké")
@Composable
private fun BigButtonLargePreview() {
    WordikTheme(SizeLevel.LARGE) {
        Surface {
            Column(Modifier.padding(16.dp)) { BigButton("Pokračovat", {}, Modifier.fillMaxWidth()) }
        }
    }
}
