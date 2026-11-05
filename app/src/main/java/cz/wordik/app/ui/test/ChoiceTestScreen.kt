package cz.wordik.app.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.wordik.app.R
import cz.wordik.app.domain.TestResult
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.BigOutlinedButton
import cz.wordik.app.ui.components.PassProgress
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.LocalFeedbackColors
import cz.wordik.app.ui.theme.WordikTheme

/** Potvrzení předčasného ukončení testu. */
@Composable
fun QuitTestDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quit_title)) },
        text = { Text(stringResource(R.string.quit_text)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.quit_confirm), style = MaterialTheme.typography.titleMedium) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.quit_cancel), style = MaterialTheme.typography.titleMedium) }
        },
    )
}

/** Zobrazí se na místě běžícího testu, který nešlo sestavit. */
@Composable
fun TestErrorContent(onBack: () -> Unit) {
    ScreenScaffold(
        title = stringResource(R.string.test_title),
        backLabel = stringResource(R.string.back),
        onBack = onBack,
    ) {
        Text(stringResource(R.string.test_error), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun ChoiceTestScreen(
    viewModel: ChoiceTestViewModel,
    onFinished: (TestResult) -> Unit,
    onAbandon: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmQuit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.result) { state.result?.let(onFinished) }

    if (state.error) {
        TestErrorContent(onBack = onAbandon)
        return
    }

    BackHandler { confirmQuit = true }
    if (confirmQuit) {
        QuitTestDialog(
            onConfirm = {
                confirmQuit = false
                viewModel.onQuit()
            },
            onDismiss = { confirmQuit = false },
        )
    }
    ChoiceTestContent(
        state = state,
        onOption = viewModel::onOptionClick,
        onNext = viewModel::onNext,
        onQuit = { confirmQuit = true },
    )
}

@Composable
fun ChoiceTestContent(
    state: ChoiceUiState,
    onOption: (Int) -> Unit,
    onNext: () -> Unit,
    onQuit: () -> Unit,
) {
    val answered = state.answeredIndex != null
    ScreenScaffold(
        title = stringResource(R.string.test_progress, state.pass, state.successPercent),
        actions = {
            TextButton(onClick = onQuit) {
                Text(stringResource(R.string.test_quit), style = MaterialTheme.typography.titleMedium)
            }
        },
        bottomBar = {
            BigButton(
                text = stringResource(R.string.test_next),
                onClick = onNext,
                enabled = answered,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        PassProgress(state.passDone, state.passTotal)

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = state.prompt,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp).testTag("prompt"),
            )
        }

        // Na širokém displeji (tablet, telefon na šířku) jsou možnosti ve dvou sloupcích.
        val columns = if (LocalConfiguration.current.screenWidthDp >= 600) 2 else 1
        state.options.chunked(columns).forEachIndexed { rowIndex, rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowOptions.forEachIndexed { colIndex, option ->
                    val index = rowIndex * columns + colIndex
                    OptionButton(
                        text = option,
                        state = optionState(state, index),
                        enabled = !answered,
                        onClick = { onOption(index) },
                        modifier = Modifier.weight(1f).fillMaxHeight().testTag("option"),
                    )
                }
                // Prázdné místo, aby poslední tlačítko v liché řadě nebylo roztažené.
                repeat(columns - rowOptions.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

private enum class OptionState { NEUTRAL, CORRECT, WRONG, DIMMED }

private fun optionState(state: ChoiceUiState, index: Int) = when {
    state.answeredIndex == null -> OptionState.NEUTRAL
    index == state.correctIndex -> OptionState.CORRECT
    index == state.answeredIndex -> OptionState.WRONG
    else -> OptionState.DIMMED
}

@Composable
private fun OptionButton(
    text: String,
    state: OptionState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val feedback = LocalFeedbackColors.current
    when (state) {
        OptionState.NEUTRAL, OptionState.DIMMED ->
            BigOutlinedButton(text = text, onClick = onClick, enabled = enabled, modifier = modifier)
        OptionState.CORRECT -> BigButton(
            text = text,
            onClick = {},
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = feedback.correct, contentColor = feedback.onCorrect),
        )
        OptionState.WRONG -> BigButton(
            text = text,
            onClick = {},
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(containerColor = feedback.wrong, contentColor = feedback.onWrong),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ChoicePreview() {
    WordikTheme {
        ChoiceTestContent(
            ChoiceUiState(
                prompt = "kočka",
                options = listOf("dog", "cat", "horse", "fish", "mouse"),
                answeredIndex = 0,
                correctIndex = 1,
                isCorrect = false,
                pass = 1,
                successPercent = 60,
            ),
            {}, {}, {},
        )
    }
}
