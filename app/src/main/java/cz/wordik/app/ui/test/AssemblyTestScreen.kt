package cz.wordik.app.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cz.wordik.app.R
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.TestResult
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.PassProgress
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.LocalFeedbackColors
import cz.wordik.app.ui.theme.LocalSizes
import cz.wordik.app.ui.theme.WordikTheme

@Composable
fun AssemblyTestScreen(
    viewModel: AssemblyTestViewModel,
    onFinished: (TestResult) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmQuit by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.result) { state.result?.let(onFinished) }

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
    AssemblyTestContent(
        state = state,
        onTile = viewModel::onTileClick,
        onSlot = viewModel::onSlotClick,
        onConfirm = viewModel::onConfirm,
        onNext = viewModel::onNext,
        onQuit = { confirmQuit = true },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssemblyTestContent(
    state: AssemblyUiState,
    onTile: (Int) -> Unit,
    onSlot: (Int) -> Unit,
    onConfirm: () -> Unit,
    onNext: () -> Unit,
    onQuit: () -> Unit,
) {
    val whole = state.evaluation == Evaluation.WHOLE_WORD
    ScreenScaffold(
        title = stringResource(R.string.test_progress, state.pass, state.successPercent),
        actions = {
            TextButton(onClick = onQuit) {
                Text(stringResource(R.string.test_quit), style = MaterialTheme.typography.titleMedium)
            }
        },
        bottomBar = {
            // Celé slovo: "Potvrdit" je dostupné kdykoliv, i u nevyplněného slova; po vyhodnocení "Dál".
            if (whole && !state.done) {
                BigButton(stringResource(R.string.test_confirm), onConfirm, Modifier.fillMaxWidth())
            } else {
                BigButton(stringResource(R.string.test_next), onNext, Modifier.fillMaxWidth(), enabled = state.done)
            }
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

        // Rozpracované slovo: místa podle délky slova (včetně mezer), zalamují se na malé obrazovce.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            state.slots.forEachIndexed { index, slot ->
                SlotBox(
                    char = slot.char,
                    clickable = whole && !state.done && slot.char != null,
                    onClick = { onSlot(index) },
                )
            }
        }

        Message(state)

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.tiles.forEach { tile ->
                TileKey(
                    char = tile.char,
                    used = tile.used,
                    enabled = !tile.used && !state.done,
                    onClick = { onTile(tile.id) },
                )
            }
        }
    }
}

@Composable
private fun Message(state: AssemblyUiState) {
    val feedback = LocalFeedbackColors.current
    val (text, color) = when {
        state.letterError -> stringResource(R.string.assembly_wrong_letter) to MaterialTheme.colorScheme.error
        state.done && state.hadError ->
            stringResource(R.string.assembly_wrong_word, state.correctAnswer.orEmpty()) to MaterialTheme.colorScheme.error
        state.done -> stringResource(R.string.assembly_correct) to feedback.onCorrect
        else -> return
    }
    Text(
        text = text,
        color = color,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SlotBox(char: Char?, clickable: Boolean, onClick: () -> Unit) {
    val size = LocalSizes.current.tileSize
    val filled = char != null
    Surface(
        onClick = onClick,
        enabled = clickable,
        shape = RoundedCornerShape(10.dp),
        color = if (filled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(2.dp, if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(min = size).widthIn(min = size).testTag("slot"),
    ) {
        Text(
            text = when (char) {
                null -> ""
                ' ' -> "•"
                else -> char.toString()
            },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun TileKey(char: Char, used: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val size = LocalSizes.current.tileSize
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.heightIn(min = size).widthIn(min = size).alpha(if (used) 0.25f else 1f).testTag("tile"),
    ) {
        Text(
            text = if (char == ' ') stringResource(R.string.assembly_space) else char.toString(),
            style = if (char == ' ') MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

private fun previewState(done: Boolean = false, hadError: Boolean = false, whole: Boolean = false) = AssemblyUiState(
    prompt = "zmrzlina",
    evaluation = if (whole) Evaluation.WHOLE_WORD else Evaluation.PER_LETTER,
    slots = "ice cream".map { cz.wordik.app.ui.test.SlotUi(if (it in "ice ") it else null) },
    tiles = "ice cream".toList().shuffled(kotlin.random.Random(1)).mapIndexed { i, c ->
        TileUi(i, c, used = c in "ice ")
    },
    done = done,
    hadError = hadError,
    correctAnswer = if (hadError) "ice cream" else null,
    pass = 1,
    successPercent = 80,
)

@Preview(showBackground = true)
@Composable
private fun AssemblyPreview() {
    WordikTheme { AssemblyTestContent(previewState(), {}, {}, {}, {}, {}) }
}

@Preview(showBackground = true, name = "Celé slovo, chyba")
@Composable
private fun AssemblyWholeErrorPreview() {
    WordikTheme { AssemblyTestContent(previewState(done = true, hadError = true, whole = true), {}, {}, {}, {}, {}) }
}
