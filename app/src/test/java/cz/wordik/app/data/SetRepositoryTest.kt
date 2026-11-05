package cz.wordik.app.data

import cz.wordik.app.domain.WordSetParseException
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SetRepositoryTest {
    private class FakeSource(private val files: List<Pair<String, String>>) : SetFileSource {
        override fun readAll() = files
    }

    @Test
    fun `nacte vsechny sady a nazev je nazev souboru`() {
        val sets = SetRepository(FakeSource(listOf("A" to "a;a", "B" to "b;b\nc;c"))).loadAll()
        assertEquals(listOf("A", "B"), sets.map { it.name })
        assertEquals(listOf(1, 2), sets.map { it.words.size })
    }

    @Test
    fun `chybna sada vyhodi vyjimku s cislem radku`() {
        val repo = SetRepository(FakeSource(listOf("A" to "a;a", "Spatna" to "x;y\nbez oddelovace")))
        try {
            repo.loadAll()
            fail()
        } catch (e: WordSetParseException) {
            assertEquals("Spatna", e.setName)
            assertEquals(2, e.errors.single().lineNumber)
        }
    }

    /** Čte skutečné soubory z `src/main/assets/sets` (unit testy běží v adresáři modulu `app`). */
    private object DiskSource : SetFileSource {
        val dir = File("src/main/assets/sets")
        override fun readAll() = dir.listFiles { f -> f.extension == "txt" }.orEmpty()
            .sortedBy { it.name }
            .map { it.nameWithoutExtension to it.readText(Charsets.UTF_8) }
    }

    @Test
    fun `ukazkove sady z assets jdou nacist bez chyb`() {
        assertTrue("Adresář sad neexistuje: ${DiskSource.dir.absolutePath}", DiskSource.dir.isDirectory)
        val sets = SetRepository(DiskSource).loadAll()
        assertEquals(2, sets.size)
        sets.forEach { set ->
            assertEquals("Sada ${set.name} má mít 12 slovíček", 12, set.words.size)
        }
    }

    @Test
    fun `ukazkove sady obsahuji frazi apostrof a slovicko v uvozovkach`() {
        val words = SetRepository(DiskSource).loadAll().flatMap { it.words }
        assertTrue(words.any { ' ' in it.foreign })
        assertTrue(words.any { '\'' in it.foreign })
        assertTrue(words.any { it.caseSensitive })
        assertTrue(words.none { '"' in it.foreign })
    }

    @Test
    fun `ukazkove sady maji unikatni nazvy a neprazdne preklady`() {
        val sets = SetRepository(DiskSource).loadAll()
        assertEquals(sets.size, sets.map { it.name }.toSet().size)
        assertTrue(sets.flatMap { it.words }.all { it.czech.isNotBlank() && it.foreign.isNotBlank() })
    }
}
