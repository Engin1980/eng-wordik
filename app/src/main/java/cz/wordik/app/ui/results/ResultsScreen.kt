package cz.wordik.app.ui.results

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cz.wordik.app.R
import cz.wordik.app.domain.TestResult
import cz.wordik.app.domain.Word
import cz.wordik.app.domain.WordResult
import cz.wordik.app.ui.components.BigButton
import cz.wordik.app.ui.components.BigOutlinedButton
import cz.wordik.app.ui.components.ScreenScaffold
import cz.wordik.app.ui.theme.LocalFeedbackColors
import cz.wordik.app.ui.theme.WordikTheme

@Composable
fun ResultsScreen(
    result: TestResult,
    onRepeat: () -> Unit,
    onBackToSelection: () -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = stringResource(R.string.results_title),
        backLabel = stringResource(R.string.back),
        onBack = onBack,
        bottomBar = {
            BigButton(stringResource(R.string.results_repeat), onRepeat, Modifier.fillMaxWidth())
            BigOutlinedButton(stringResource(R.string.results_back_to_selection), onBackToSelection, Modifier.fillMaxWidth())
        },
    ) {
        if (!result.completed) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(R.string.results_incomplete),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }

        val stars = ResultFormat.stars(result.completed, result.successRate)
        if (stars > 0) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { StarRating(filled = stars) }
        }

        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    ResultFormat.percent(result.successRate),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.results_success),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }

        StatRow(stringResource(R.string.results_correct), result.correct.toString())
        StatRow(stringResource(R.string.results_wrong), result.wrong.toString())
        StatRow(stringResource(R.string.results_duration), ResultFormat.duration(result.durationMillis))

        SectionTitle(stringResource(R.string.results_wrong_words))
        val wrongWords = result.wrongWords
        if (wrongWords.isEmpty()) {
            Text(stringResource(R.string.results_no_wrong_words), style = MaterialTheme.typography.bodyLarge)
        } else {
            wrongWords.forEach { WordLine(it) }
        }

        SectionTitle(stringResource(R.string.results_all_words))
        Text(
            stringResource(R.string.results_all_words_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        result.words.forEachIndexed { index, wordResult ->
            if (index > 0) HorizontalDivider()
            WordResultRow(wordResult)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun WordLine(word: Word) {
    Text("${word.czech} – ${word.foreign}", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun WordResultRow(result: WordResult) {
    val feedback = LocalFeedbackColors.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "${result.word.czech} – ${result.word.foreign}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
        )
        Text(
            "${ResultFormat.percent(result.successRate)} (${result.correct}/${result.total})",
            style = MaterialTheme.typography.bodyLarge,
            color = if (result.wrong == 0) feedback.onCorrect else MaterialTheme.colorScheme.error,
        )
    }
}

private val previewResult = TestResult(
    completed = true,
    correct = 8,
    wrong = 4,
    durationMillis = 125_000,
    words = listOf(
        WordResult(Word("pes", "dog"), 1, 2),
        WordResult(Word("kočka", "cat"), 3, 0),
        WordResult(Word("zmrzlina", "ice cream"), 4, 2),
    ),
)

@Preview(showBackground = true)
@Composable
private fun ResultsPreview() {
    WordikTheme { ResultsScreen(previewResult, {}, {}, {}) }
}
