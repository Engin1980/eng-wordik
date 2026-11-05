package cz.wordik.app

import android.app.Application
import android.content.Context
import cz.wordik.app.data.AssetSetFileSource
import cz.wordik.app.data.ConfigRepository
import cz.wordik.app.data.DataStoreSettingsStore
import cz.wordik.app.data.SetRepository
import cz.wordik.app.data.SettingsStore
import cz.wordik.app.data.settingsDataStore
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.WordSet
import cz.wordik.app.sound.NoSoundPlayer
import cz.wordik.app.sound.SoundPlayer
import cz.wordik.app.sound.SoundPoolPlayer

/** Ručně sestavené závislosti aplikace. */
class AppContainer(
    val config: AppConfig,
    val sets: List<WordSet>,
    val settingsStore: SettingsStore,
    val soundPlayer: SoundPlayer = NoSoundPlayer,
) {
    companion object {
        /** @throws Exception pokud je chybná konfigurace nebo některá přibalená sada. */
        fun load(context: Context): AppContainer {
            val config = ConfigRepository(context.assets).load()
            val sets = SetRepository(AssetSetFileSource(context.assets)).loadAll()
            return AppContainer(
                config,
                sets,
                DataStoreSettingsStore(context.settingsDataStore, config),
                SoundPoolPlayer(context),
            )
        }
    }
}

class WordikApplication : Application() {
    /** Chyba v přibalených datech se zobrazí na úvodní obrazovce místo pádu aplikace. */
    val container: Result<AppContainer> by lazy { runCatching { AppContainer.load(this) } }
}
