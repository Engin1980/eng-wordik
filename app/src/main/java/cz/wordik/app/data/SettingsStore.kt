package cz.wordik.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.SettingsCodec
import cz.wordik.app.domain.SizeLevel
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

    /** Volba „Normální / Velké“ pro celou aplikaci; výchozí je [SizeLevel.NORMAL]. */
    val sizeLevel: Flow<SizeLevel>

    suspend fun saveSizeLevel(level: SizeLevel)
}

class DataStoreSettingsStore(
    private val dataStore: DataStore<Preferences>,
    private val config: AppConfig,
) : SettingsStore {

    private val sizeLevelKey = stringPreferencesKey("size_level")

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

    override val sizeLevel: Flow<SizeLevel> = preferences.map { prefs ->
        val stored = runCatching { prefs[sizeLevelKey] }.getOrNull()
        SizeLevel.entries.firstOrNull { it.name == stored } ?: SizeLevel.NORMAL
    }

    override suspend fun saveSizeLevel(level: SizeLevel) {
        dataStore.edit { it[sizeLevelKey] = level.name }
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
