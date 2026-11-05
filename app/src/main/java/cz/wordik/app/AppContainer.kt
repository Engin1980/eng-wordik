package cz.wordik.app

import android.app.Application
import android.content.Context
import cz.wordik.app.data.AssetSetFileSource
import cz.wordik.app.data.ConfigRepository
import cz.wordik.app.data.DataStoreSettingsStore
import cz.wordik.app.data.FileSetStorage
import cz.wordik.app.data.SetLibrary
import cz.wordik.app.data.SettingsStore
import cz.wordik.app.data.settingsDataStore
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.sound.NoSoundPlayer
import cz.wordik.app.sound.SoundPlayer
import cz.wordik.app.sound.SoundPoolPlayer
import java.io.File

/** Ručně sestavené závislosti aplikace. */
class AppContainer(
    val config: AppConfig,
    val library: SetLibrary,
    val settingsStore: SettingsStore,
    val soundPlayer: SoundPlayer = NoSoundPlayer,
) {
    companion object {
        /** @throws Exception pokud je chybná konfigurace nebo některá přibalená sada. */
        fun load(context: Context): AppContainer {
            val config = ConfigRepository(context.assets).load()
            val library = SetLibrary(FileSetStorage(File(context.filesDir, "sets")), AssetSetFileSource(context.assets))
            return AppContainer(
                config,
                library,
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
