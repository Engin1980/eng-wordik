package cz.wordik.app.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.Direction
import cz.wordik.app.domain.Evaluation
import cz.wordik.app.domain.SettingsCodec
import cz.wordik.app.domain.SettingsRanges
import cz.wordik.app.domain.TestMode
import cz.wordik.app.domain.TestSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val defaults = TestSettings(
        mode = TestMode.ASSEMBLY,
        wrongOptions = 4,
        minPasses = 2,
        minSuccessPercent = 50,
        extraLetters = 0,
        evaluation = Evaluation.PER_LETTER,
        direction = Direction.MIX,
    )
    private val config = AppConfig(
        defaults = defaults,
        ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10),
        autoAdvanceMillis = 1000,
        fontName = "Test",
    )
    private val custom = defaults.copy(
        mode = TestMode.CHOICE,
        wrongOptions = 7,
        minPasses = 5,
        minSuccessPercent = 90,
        direction = Direction.CZECH_TO_FOREIGN,
    )

    private lateinit var file: File
    private var job = Job()

    @Before
    fun setUp() {
        file = File(folder.root, "settings.preferences_pb")
    }

    @After
    fun tearDown() = runBlocking { job.cancelAndJoin() }

    /**
     * DataStore v paměti. Skutečný souborový DataStore na Windows JVM selže při druhém zápisu
     * (přejmenování dočasného souboru přes existující), na Androidu to funguje.
     */
    private class FakeDataStore(private val failReads: Boolean = false) : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: Flow<Preferences> = flow {
            if (failReads) throw CorruptionException("poškozený soubor")
            emitAll(state)
        }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }
    }

    private fun openDataStore(): DataStore<Preferences> = FakeDataStore()

    /** Skutečný souborový DataStore; v testech se do něj zapisuje nejvýše jednou (viz [FakeDataStore]). */
    private fun openFileDataStore(): DataStore<Preferences> {
        job = Job()
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(Dispatchers.IO + job),
            produceFile = { file },
        )
    }

    private fun store(dataStore: DataStore<Preferences> = openDataStore()) =
        DataStoreSettingsStore(dataStore, config)

    /** Zavře souborové úložiště a otevře ho znovu nad stejným souborem, jako po restartu aplikace. */
    private fun reopen(): DataStoreSettingsStore {
        runBlocking { job.cancelAndJoin() }
        return store(openFileDataStore())
    }

    @Test
    fun `nova kombinace dostane vychozi hodnoty`() = runBlocking {
        assertEquals(defaults, store().loadTestSettings(listOf("A")))
    }

    @Test
    fun `ulozene nastaveni se nacte`() = runBlocking {
        val store = store()
        store.saveTestSettings(listOf("A"), custom)
        assertEquals(custom, store.loadTestSettings(listOf("A")))
    }

    @Test
    fun `nastaveni preziji restart`() = runBlocking {
        store(openFileDataStore()).saveTestSettings(listOf("A", "B"), custom)
        assertEquals(custom, reopen().loadTestSettings(listOf("A", "B")))
    }

    @Test
    fun `jine poradi sad je stejna kombinace`() = runBlocking {
        val store = store()
        store.saveTestSettings(listOf("B", "A"), custom)
        assertEquals(custom, store.loadTestSettings(listOf("A", "B")))
    }

    @Test
    fun `jina kombinace ma vlastni nastaveni`() = runBlocking {
        val store = store()
        store.saveTestSettings(listOf("A"), custom)
        assertEquals(defaults, store.loadTestSettings(listOf("A", "B")))
        assertEquals(defaults, store.loadTestSettings(listOf("B")))
        assertEquals(custom, store.loadTestSettings(listOf("A")))
    }

    @Test
    fun `ulozeni prepise predchozi nastaveni`() = runBlocking {
        val store = store()
        store.saveTestSettings(listOf("A"), custom)
        store.saveTestSettings(listOf("A"), defaults)
        assertEquals(defaults, store.loadTestSettings(listOf("A")))
    }

    @Test
    fun `velikost je vychozi normalni`() = runBlocking {
        assertEquals(100, store().sizePercent.first())
    }

    @Test
    fun `velikost se uklada a preziva restart`() = runBlocking {
        store(openFileDataStore()).saveSizePercent(130)
        assertEquals(130, reopen().sizePercent.first())
    }

    @Test
    fun `velikost a nastaveni testu se neovlivnuji`() = runBlocking {
        val store = store()
        store.saveSizePercent(130)
        store.saveTestSettings(listOf("A"), custom)
        assertEquals(130, store.sizePercent.first())
        assertEquals(custom, store.loadTestSettings(listOf("A")))
    }

    @Test
    fun `poskozeny text nastaveni vrati vychozi hodnoty`() = runBlocking {
        val dataStore = openDataStore()
        dataStore.edit { it[stringPreferencesKey("test_settings:" + SettingsCodec.combinationKey(listOf("A")))] = "rozbite" }
        assertEquals(defaults, store(dataStore).loadTestSettings(listOf("A")))
    }

    @Test
    fun `nekompatibilni verze nebo hodnota mimo rozsah vrati vychozi hodnoty`() = runBlocking {
        val dataStore = openDataStore()
        val key = stringPreferencesKey("test_settings:" + SettingsCodec.combinationKey(listOf("A")))
        dataStore.edit { it[key] = "v9,CHOICE,4,2,50,0,WHOLE_WORD,MIX" }
        assertEquals(defaults, store(dataStore).loadTestSettings(listOf("A")))
        dataStore.edit { it[key] = "v1,CHOICE,4,99,50,0,WHOLE_WORD,MIX" }
        assertEquals(defaults, store(dataStore).loadTestSettings(listOf("A")))
    }

    @Test
    fun `hodnota jineho typu pod klicem vrati vychozi hodnoty`() = runBlocking {
        val dataStore = openDataStore()
        val name = "test_settings:" + SettingsCodec.combinationKey(listOf("A"))
        dataStore.edit { it[intPreferencesKey(name)] = 42 }
        assertEquals(defaults, store(dataStore).loadTestSettings(listOf("A")))
    }

    @Test
    fun `neznama velikost vrati normalni`() = runBlocking {
        val dataStore = openDataStore()
        dataStore.edit { it[stringPreferencesKey("size_level")] = "OBROVSKE" }
        assertEquals(100, store(dataStore).sizePercent.first())
    }

    @Test
    fun `stara volba Velke se prevede na 130 procent`() = runBlocking {
        val dataStore = openDataStore()
        dataStore.edit { it[stringPreferencesKey("size_level")] = "LARGE" }
        assertEquals(130, store(dataStore).sizePercent.first())
    }

    @Test
    fun `velikost mimo rozsah se omezi a zaokrouhli`() = runBlocking {
        val store = store()
        store.saveSizePercent(500)
        assertEquals(200, store.sizePercent.first())
        store.saveSizePercent(10)
        assertEquals(80, store.sizePercent.first())
        store.saveSizePercent(133)
        assertEquals(130, store.sizePercent.first())
    }

    @Test
    fun `nepouzitelny soubor vrati vychozi hodnoty`() = runBlocking {
        val store = store(FakeDataStore(failReads = true))
        assertEquals(defaults, store.loadTestSettings(listOf("A")))
        assertEquals(100, store.sizePercent.first())
    }

    @Test
    fun `poskozeny soubor na disku se nahradi prazdnym`() = runBlocking {
        file.writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13))
        // Čtení spustí corruptionHandler; na Windows JVM může jeho zápis selhat, výsledek je ale stejný.
        val store = store(openFileDataStore())
        assertEquals(defaults, store.loadTestSettings(listOf("A")))
        assertEquals(100, store.sizePercent.first())
    }
}
