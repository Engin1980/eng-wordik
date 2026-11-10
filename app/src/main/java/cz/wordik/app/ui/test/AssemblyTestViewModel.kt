package cz.wordik.app.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.AssemblyRound
import cz.wordik.app.domain.AssemblyTest
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.PressResult
import cz.wordik.app.domain.TestResult
import cz.wordik.app.domain.TestSession
import cz.wordik.app.domain.TestMode
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

/** Jedno místo ve skládaném slově; [char] je null, dokud není vyplněno. */
data class SlotUi(val char: Char?)

/** Klávesa v nabídce; použitá je zešedlá. */
data class TileUi(val id: Int, val char: Char, val used: Boolean)

data class AssemblyUiState(
    /** České znění, ke kterému se skládá cizí slovo. */
    val prompt: String = "",
    val evaluation: Evaluation = Evaluation.PER_LETTER,
    val slots: List<SlotUi> = emptyList(),
    val tiles: List<TileUi> = emptyList(),
    /** Slovo je vyhodnoceno (po písmenkách zaplněno, jinak potvrzeno). */
    val done: Boolean = false,
    val hadError: Boolean = false,
    /** Slovo bylo přeskočeno a doplnilo se správné řešení. */
    val gaveUp: Boolean = false,
    /** Všechna místa jsou vyplněná (u celého slova před potvrzením). */
    val complete: Boolean = false,
    /** Poslední klepnutí bylo špatné písmeno (hodnocení po písmenkách). */
    val letterError: Boolean = false,
    /** Správné znění, zobrazí se po vyhodnocení slova s chybou. */
    val correctAnswer: String? = null,
    val pass: Int = 1,
    /** Režim "Regresivní": průchody se nazývají kola. */
    val regressive: Boolean = false,
    /** Hotová slovíčka rozpracovaného průchodu a jejich celkový počet. */
    val passDone: Int = 0,
    val passTotal: Int = 1,
    val successPercent: Int = 0,
    val result: TestResult? = null,
    val error: Boolean = false,
) {
    /** Po slově s chybou je "Dál" povinné, jinak se přechází automaticky. */
    val needsManualNext: Boolean get() = done && hadError
}

class AssemblyTestViewModel(
    words: List<Word>,
    settings: TestSettings,
    private val config: AppConfig,
    random: Random = Random.Default,
    private val sounds: SoundPlayer = NoSoundPlayer,
    clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val regressive = settings.mode == TestMode.REGRESSIVE

    private val test = AssemblyTest(
        session = TestSession(
            words, settings.minPasses, settings.minSuccessPercent, random,
            regressive = regressive, clock = clock,
        ),
        evaluation = if (regressive) Evaluation.PER_LETTER else settings.evaluation,
        extraLetters = settings.extraLetters,
        letterPool = AssemblyRound.letterPool(words),
        random = random,
    )

    private val _state = MutableStateFlow(AssemblyUiState())
    val state: StateFlow<AssemblyUiState> = _state.asStateFlow()

    private var autoAdvance: Job? = null
    private var letterError = false
    private var finishSoundPlayed = false

    init {
        refresh()
    }

    fun onTileClick(tileId: Int) {
        when (test.press(tileId)) {
            PressResult.PLACED -> {
                letterError = false
                // Po písmenkách se hotové slovo bez chyby ohlásí zvukem správné odpovědi.
                if (test.round?.advancesAutomatically == true) sounds.play(Sound.CORRECT)
            }
            PressResult.WRONG -> {
                letterError = true
                sounds.play(Sound.WRONG)
            }
            PressResult.IGNORED -> return
        }
        afterChange()
    }

    /** Vrácení vloženého písmena do nabídky (jen při hodnocení celého slova). */
    fun onSlotClick(index: Int) {
        if (test.remove(index)) afterChange()
    }

    fun onConfirm() {
        val correct = test.confirm() ?: return
        sounds.play(if (correct) Sound.CORRECT else Sound.WRONG)
        afterChange()
    }

    /** "Napověz" (po písmenkách): doplní další písmeno a započítá chybu. */
    fun onHint() {
        if (test.hint()) {
            letterError = false
            afterChange()
        }
    }

    /** Přeskočení slova (po písmenkách): zbylá písmena se započítají jako chyby a zobrazí se řešení. */
    fun onSkip() {
        if (test.giveUp()) {
            letterError = false
            sounds.play(Sound.WRONG)
            afterChange()
        }
    }

    /** "Dál": po slově bez chyby dřív než automaticky, po slově s chybou povinné. */
    fun onNext() {
        autoAdvance?.cancel()
        autoAdvance = null
        letterError = false
        test.next()
        refresh()
    }

    fun onQuit() {
        autoAdvance?.cancel()
        autoAdvance = null
        test.abort()
        refresh()
    }

    override fun onCleared() {
        autoAdvance?.cancel()
    }

    private fun afterChange() {
        refresh()
        val round = test.round ?: return
        if (round.advancesAutomatically && autoAdvance == null) {
            autoAdvance = viewModelScope.launch {
                delay(config.autoAdvanceMillis)
                onNext()
            }
        }
    }

    private fun refresh() {
        playFinishSound()
        val round = test.round
        _state.value = AssemblyUiState(
            prompt = round?.word?.czech.orEmpty(),
            evaluation = round?.evaluation ?: Evaluation.PER_LETTER,
            slots = round?.slotTiles?.map { SlotUi(it?.char) }.orEmpty(),
            tiles = round?.tiles?.map { TileUi(it.id, it.char, round.isUsed(it)) }.orEmpty(),
            done = round?.isDone ?: false,
            hadError = round?.hadError ?: false,
            gaveUp = round?.gaveUp ?: false,
            complete = round?.let { it.filledCount == it.target.length } ?: false,
            letterError = letterError,
            correctAnswer = round?.takeIf { it.needsManualNext }?.target,
            pass = test.session.completedPasses + 1,
            regressive = regressive,
            passDone = test.session.wordsDoneInPass,
            passTotal = test.session.wordCount,
            successPercent = (test.session.successRate * 100).roundToInt(),
            result = if (test.isFinished) test.result() else null,
        )
    }

    /** Fanfára jen při dokončení testu (ne při předčasném ukončení), a jen jednou. */
    private fun playFinishSound() {
        if (!test.isFinished || finishSoundPlayed) return
        finishSoundPlayed = true
        if (test.result().completed) sounds.play(Sound.FINISH)
    }
}
