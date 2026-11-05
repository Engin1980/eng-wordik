package cz.wordik.app.testutil

import cz.wordik.app.data.SettingsStore
import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.SettingsCodec
import cz.wordik.app.domain.SettingsRanges
import cz.wordik.app.domain.SizeLevel
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import cz.wordik.app.domain.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Nahradí Dispatchers.Main testovacím dispatcherem, aby šel ovládat čas ve ViewModelech. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

/** Úložiště nastavení v paměti. */
class FakeSettingsStore(private val defaults: TestSettings = TestData.defaults) : SettingsStore {
    val saved = mutableMapOf<String, TestSettings>()
    private val size = MutableStateFlow(SizeLevel.NORMAL)

    override suspend fun loadTestSettings(setNames: Collection<String>): TestSettings =
        saved[SettingsCodec.combinationKey(setNames)] ?: defaults

    override suspend fun saveTestSettings(setNames: Collection<String>, settings: TestSettings) {
        saved[SettingsCodec.combinationKey(setNames)] = settings
    }

    override val sizeLevel: Flow<SizeLevel> = size

    override suspend fun saveSizeLevel(level: SizeLevel) {
        size.value = level
    }
}

object TestData {
    val defaults = TestSettings(
        mode = TestMode.ASSEMBLY,
        wrongOptions = 4,
        minPasses = 2,
        minSuccessPercent = 50,
        extraLetters = 0,
        evaluation = Evaluation.PER_LETTER,
        direction = Direction.MIX,
    )

    val ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10)

    val config = AppConfig(defaults, ranges, autoAdvanceMillis = 1000, fontName = "Atkinson Hyperlegible")
}
