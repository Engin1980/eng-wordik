package cz.wordik.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.ChoiceQuestionGenerator
import cz.wordik.app.domain.ChoiceTest
import cz.wordik.app.domain.TestResult
import cz.wordik.app.domain.TestSession
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.Word
import cz.wordik.app.sound.NoSoundPlayer
import cz.wordik.app.sound.Sound
import cz.wordik.app.sound.SoundPlayer
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChoiceUiState(
    val prompt: String = "",
    val options: List<String> = emptyList(),
    /** Zvolená možnost; null před odpovědí. */
    val answeredIndex: Int? = null,
    /** Správná možnost, zná se (a zobrazuje) až po odpovědi. */
    val correctIndex: Int? = null,
    val isCorrect: Boolean? = null,
    /** Číslo právě probíhajícího průchodu (od 1). */
    val pass: Int = 1,
    /** Hotová slovíčka rozpracovaného průchodu a jejich celkový počet. */
    val passDone: Int = 0,
    val passTotal: Int = 1,
    val successPercent: Int = 0,
    /** Test skončil (splněny podmínky, nebo ukončil uživatel); UI přejde na výsledky. */
    val result: TestResult? = null,
    /** Test nešlo sestavit (málo různých možností). */
    val error: Boolean = false,
)

class ChoiceTestViewModel(
    words: List<Word>,
    settings: TestSettings,
    private val config: AppConfig,
    random: Random = Random.Default,
    private val sounds: SoundPlayer = NoSoundPlayer,
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val test: ChoiceTest? = runCatching {
        ChoiceTest(
            TestSession(words, settings.minPasses, settings.minSuccessPercent, random, clock = clock),
            ChoiceQuestionGenerator(words, settings.direction, settings.wrongOptions, random),
        )
    }.getOrNull()

    private val _state = MutableStateFlow(ChoiceUiState(error = test == null))
    val state: StateFlow<ChoiceUiState> = _state.asStateFlow()

    private var autoAdvance: Job? = null
    private var finishSoundPlayed = false

    init {
        refresh()
    }

    fun onOptionClick(index: Int) {
        val t = test ?: return
        val correct = t.answer(index) ?: return
        sounds.play(if (correct) Sound.CORRECT else Sound.WRONG)
        refresh()
        if (correct) {
            autoAdvance = viewModelScope.launch {
                delay(config.autoAdvanceMillis)
                onNext()
            }
        }
    }

    /** "Dál": po správné odpovědi dřív než automaticky, po chybné povinné. */
    fun onNext() {
        autoAdvance?.cancel()
        autoAdvance = null
        test?.next()
        refresh()
    }

    fun onQuit() {
        autoAdvance?.cancel()
        autoAdvance = null
        test?.abort()
        refresh()
    }

    override fun onCleared() {
        autoAdvance?.cancel()
    }

    private fun refresh() {
        val t = test ?: return
        playFinishSound(t)
        val question = t.question
        val answered = t.answeredIndex
        _state.value = ChoiceUiState(
            prompt = question?.prompt.orEmpty(),
            options = question?.options.orEmpty(),
            answeredIndex = answered,
            correctIndex = if (answered != null) question?.correctIndex else null,
            isCorrect = t.lastAnswerCorrect,
            pass = t.session.completedPasses + 1,
            passDone = t.session.wordsDoneInPass,
            passTotal = t.session.wordCount,
            successPercent = (t.session.successRate * 100).roundToInt(),
            result = if (t.isFinished) t.result() else null,
        )
    }

    /** Fanfára jen při dokončení testu (ne při předčasném ukončení), a jen jednou. */
    private fun playFinishSound(t: ChoiceTest) {
        if (!t.isFinished || finishSoundPlayed) return
        finishSoundPlayed = true
        if (t.result().completed) sounds.play(Sound.FINISH)
    }
}
