package cz.wordik.app.ui.setup

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.wordik.app.R
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.BigOutlinedButton
import cz.wordik.app.ui.components.ScreenScaffold

/** Jednoduchý textový editor sady: slovíčka jako volný text `česky;cizí` po řádcích. */
@Composable
fun SetEditorScreen(
    viewModel: SetEditorViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }

    // Zahodit změny (i tlačítkem Zpět): bez změn rovnou, jinak se uživatel nejdřív zeptá.
    val requestDiscard = {
        if (viewModel.isDirty) confirmDiscard = true else onClose()
    }
    BackHandler(onBack = requestDiscard)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.edit_discard_title)) },
            text = { Text(stringResource(R.string.edit_discard_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) { Text(stringResource(R.string.edit_discard_confirm), style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.edit_keep_editing), style = MaterialTheme.typography.titleMedium)
                }
            },
        )
    }

    SetEditorContent(
        name = viewModel.name,
        state = state,
        onTextChange = viewModel::onTextChange,
        onSave = { if (viewModel.save()) onClose() },
        onDiscard = requestDiscard,
    )
}

@Composable
fun SetEditorContent(
    name: String,
    state: SetEditorState,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.edit_title, name),
        scrollable = false,
        bottomBar = {
            BigButton(stringResource(R.string.edit_save), onSave, Modifier.fillMaxWidth())
            BigOutlinedButton(stringResource(R.string.edit_discard), onDiscard, Modifier.fillMaxWidth())
        },
    ) {
        Text(stringResource(R.string.edit_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (state.errors.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().testTag("editor_errors"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.edit_errors_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                state.errors.take(MAX_SHOWN_ERRORS).forEach {
                    Text(it.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                if (state.errors.size > MAX_SHOWN_ERRORS) {
                    Text(
                        stringResource(R.string.edit_errors_more, state.errors.size - MAX_SHOWN_ERRORS),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        OutlinedTextField(
            value = state.text,
            onValueChange = onTextChange,
            isError = state.errors.isNotEmpty(),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 160.dp).testTag("editor"),
        )
    }
}

private const val MAX_SHOWN_ERRORS = 8

/** Skenování QR kódů: naskenované texty se sloučí a po "Hotovo" se pokračuje editací. */
@Composable
fun QrScanScreen(
    viewModel: QrImportViewModel,
    onDone: (text: String) -> Unit,
    onBack: () -> Unit,
    /** Náhled kamery; v testech jde nahradit, aby nebyla potřeba kamera. */
    preview: @Composable (onCode: (String) -> Unit, modifier: Modifier) -> Unit = { onCode, modifier ->
        CameraQrPreview(onCode, modifier)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }

    ScreenScaffold(
        title = stringResource(R.string.scan_title, viewModel.name),
        backLabel = stringResource(R.string.back),
        onBack = onBack,
        scrollable = false,
        bottomBar = {
            Text(
                stringResource(R.string.scan_progress, state.codes, state.words),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("scan_progress"),
            )
            BigButton(
                stringResource(R.string.scan_done),
                { onDone(viewModel.merged()) },
                Modifier.fillMaxWidth(),
                enabled = state.codes > 0,
            )
        },
    ) {
        Text(stringResource(R.string.scan_hint), style = MaterialTheme.typography.bodyLarge)
        if (granted) {
            preview(
                { viewModel.onCode(it) },
                Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).testTag("camera"),
            )
        } else {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (asked) R.string.scan_permission_denied else R.string.scan_permission_rationale),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    BigOutlinedButton(
                        stringResource(R.string.scan_permission_button),
                        { launcher.launch(android.Manifest.permission.CAMERA) },
                    )
                }
            }
        }
    }
}
