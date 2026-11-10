package cz.wordik.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.SettingsCodec
import cz.wordik.app.domain.SizeScale
import cz.wordik.app.domain.TestSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Místní úložiště nastavení: poslední nastavení testu pro kombinaci sad a velikost ovládacích prvků. */
interface SettingsStore {
    /** Poslední nastavení pro přesnou kombinaci sad, jinak (i při poškozených datech) výchozí z konfigurace. */
    suspend fun loadTestSettings(setNames: Collection<String>): TestSettings

    suspend fun saveTestSettings(setNames: Collection<String>, settings: TestSettings)

    /** Velikost ovládacích prvků a písma v % pro celou aplikaci; výchozí je [SizeScale.DEFAULT_PERCENT]. */
    val sizePercent: Flow<Int>

    suspend fun saveSizePercent(percent: Int)
}

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val config: AppConfig,
) : SettingsStore {

    private val sizePercentKey = intPreferencesKey("size_percent")

    /** Klíč ze starší verze (volba Normální / Velké), čte se jen jako záloha. */
    private val legacySizeLevelKey = stringPreferencesKey("size_level")

    private fun testSettingsKey(setNames: Collection<String>) =
        stringPreferencesKey("test_settings:" + SettingsCodec.combinationKey(setNames))

    /** Čte preference; nečitelný soubor se bere jako prázdný. */
    private val preferences: Flow<Preferences> =
        dataStore.data.catch { emit(emptyPreferences()) }

    override suspend fun loadTestSettings(setNames: Collection<String>): TestSettings {
        val stored = readString(testSettingsKey(setNames))
        return stored?.let { SettingsCodec.decode(it, config.ranges) } ?: config.defaults
    }

    override suspend fun saveTestSettings(setNames: Collection<String>, settings: TestSettings) {
        dataStore.edit { it[testSettingsKey(setNames)] = SettingsCodec.encode(settings) }
    }

    override val sizePercent: Flow<Int> = preferences.map { prefs ->
        val stored = runCatching { prefs[sizePercentKey] }.getOrNull()
        val legacy = runCatching { prefs[legacySizeLevelKey] }.getOrNull()
        stored?.let(SizeScale::normalize) ?: SizeScale.fromLegacy(legacy) ?: SizeScale.DEFAULT_PERCENT
    }

    override suspend fun saveSizePercent(percent: Int) {
        dataStore.edit { it[sizePercentKey] = SizeScale.normalize(percent) }
    }

    /** Hodnota jiného typu pod stejným klíčem (ClassCastException) se bere jako chybějící. */
    private suspend fun readString(key: Preferences.Key<String>): String? =
        runCatching { preferences.first()[key] }.getOrNull()
}

/** Produkční úložiště; poškozený soubor se nahradí prázdným. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
