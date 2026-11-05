package cz.wordik.app.data

import java.io.File

/** Zapisovatelné úložiště souborů sad (jeden soubor `název.txt` na sadu). */
interface SetStorage : SetFileSource {
    fun write(name: String, text: String)

    fun delete(name: String)

    /** Přibalené sady se do úložiště zkopírují jen jednou; po smazání se nevrací. */
    var bundledInstalled: Boolean
}

/** Úložiště ve složce na disku (ve vnitřním úložišti aplikace). */
class FileSetStorage(private val directory: File) : SetStorage {
    private val marker get() = File(directory, INSTALLED_MARKER)

    init {
        directory.mkdirs()
    }

    override fun readAll(): List<Pair<String, String>> =
        directory.listFiles { f -> f.isFile && f.name.endsWith(AssetSetFileSource.SET_EXTENSION, ignoreCase = true) }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map { it.nameWithoutExtension to it.readText(Charsets.UTF_8) }

    override fun write(name: String, text: String) {
        directory.mkdirs()
        File(directory, name + AssetSetFileSource.SET_EXTENSION).writeText(text, Charsets.UTF_8)
    }

    override fun delete(name: String) {
        File(directory, name + AssetSetFileSource.SET_EXTENSION).delete()
    }

    override var bundledInstalled: Boolean
        get() = marker.exists()
        set(value) {
            if (value) {
                directory.mkdirs()
                marker.writeText("1")
            } else {
                marker.delete()
            }
        }

    private companion object {
        const val INSTALLED_MARKER = ".bundled_installed"
    }
}
