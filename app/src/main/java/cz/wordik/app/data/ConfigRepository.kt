package cz.wordik.app.data

import android.content.res.AssetManager
import cz.wordik.app.domain.AppConfig
import cz.wordik.app.domain.ConfigParser

/** Načítá konfiguraci přibalenou v `assets/config.properties`. */
class ConfigRepository(private val assets: AssetManager) {
    /** @throws cz.wordik.app.domain.ConfigException pokud je konfigurace chybná. */
    fun load(): AppConfig {
        val text = assets.open(CONFIG_FILE).bufferedReader(Charsets.UTF_8).use { it.readText() }
        return ConfigParser.parse(text)
    }

    companion object {
        const val CONFIG_FILE = "config.properties"
    }
}
