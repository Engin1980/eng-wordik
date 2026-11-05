package cz.wordik.app.data

import cz.wordik.app.domain.SetNameError
import cz.wordik.app.domain.SetNameValidator
import cz.wordik.app.domain.Word
import cz.wordik.app.domain.WordSet
import cz.wordik.app.domain.WordSetText
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SetLibraryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val bundled = object : SetFileSource {
        override fun readAll() = listOf("Zvířata" to "pes;dog\nkočka;cat", "Škola" to "tužka;pencil")
    }

    private fun storage() = FileSetStorage(File(folder.root, "sets"))

    // --- název ---

    @Test
    fun `nazev sady prazdny je chyba`() {
        assertEquals(SetNameError.EMPTY, SetNameValidator.validate("   ", emptyList()))
    }

    @Test
    fun `duplicitni nazev se nerozlisuje podle velikosti pismen a mezer okolo`() {
        assertEquals(SetNameError.DUPLICATE, SetNameValidator.validate(" zvířata ", listOf("Zvířata")))
        assertNull(SetNameValidator.validate("Jidlo", listOf("Zvířata")))
    }

    @Test
    fun `zakazane znaky konec teckou a prilis dlouhy nazev`() {
        for (c in SetNameValidator.FORBIDDEN_CHARS) {
            assertEquals("znak $c", SetNameError.INVALID_CHARS, SetNameValidator.validate("a${c}b", emptyList()))
        }
        assertEquals(SetNameError.INVALID_CHARS, SetNameValidator.validate("sada.", emptyList()))
        assertEquals(SetNameError.TOO_LONG, SetNameValidator.validate("x".repeat(SetNameValidator.MAX_LENGTH + 1), emptyList()))
        assertNull(SetNameValidator.validate("x".repeat(SetNameValidator.MAX_LENGTH), emptyList()))
        assertNull(SetNameValidator.validate("Škola a domov 2", emptyList()))
    }

    // --- přibalené sady ---

    @Test
    fun `pri prvnim spusteni se pribalene sady zkopiruji do uloziste`() {
        val library = SetLibrary(storage(), bundled)
        assertEquals(listOf("Zvířata", "Škola").sortedBy { it.lowercase() }, library.sets.value.map { it.name })
        assertEquals(2, library.find("Zvířata")!!.words.size)
        assertTrue(storage().bundledInstalled)
    }

    @Test
    fun `smazana pribalena sada se po restartu nevrati`() {
        SetLibrary(storage(), bundled).delete("Zvířata")
        val again = SetLibrary(storage(), bundled)
        assertEquals(listOf("Škola"), again.sets.value.map { it.name })
    }

    @Test
    fun `existujici sada stejneho nazvu se pri kopirovani neprepise`() {
        val storage = storage()
        storage.write("Zvířata", "moje;mine")
        val library = SetLibrary(storage, bundled)
        assertEquals(listOf(Word("moje", "mine")), library.find("Zvířata")!!.words)
    }

    // --- vytvoření, uložení, smazání ---

    @Test
    fun `vytvorena sada je prazdna a prezije restart`() {
        val library = SetLibrary(storage())
        val created = library.create("  Nova ")
        assertEquals("Nova", created.name)
        assertTrue(created.words.isEmpty())
        assertEquals(listOf("Nova"), SetLibrary(storage()).sets.value.map { it.name })
    }

    @Test
    fun `vytvoreni s neplatnym nazvem selze`() {
        val library = SetLibrary(storage())
        library.create("A")
        var failed = 0
        for (name in listOf("", "a", "x/y")) {
            runCatching { library.create(name) }.onFailure { failed++ }
        }
        assertEquals(3, failed)
        assertEquals(1, library.sets.value.size)
    }

    @Test
    fun `ulozeni platneho textu zapise slovicka vcetne uvozovek`() {
        val library = SetLibrary(storage())
        library.create("Dny")
        val result = library.save("Dny", "pondělí;\"Monday\"\n\nstůl;table")
        assertTrue(result is SaveResult.Saved)
        val set = SetLibrary(storage()).find("Dny")!!
        assertEquals(listOf(Word("pondělí", "Monday", caseSensitive = true), Word("stůl", "table")), set.words)
        assertEquals("pondělí;\"Monday\"\nstůl;table", library.text("Dny"))
    }

    @Test
    fun `chybny text se neulozi a vrati cisla radku`() {
        val library = SetLibrary(storage())
        library.create("A")
        library.save("A", "pes;dog")
        val result = library.save("A", "kočka;cat\nbez oddelovace\nx;y;z")
        assertEquals(listOf(2, 3), (result as SaveResult.Invalid).errors.map { it.lineNumber })
        assertEquals(listOf(Word("pes", "dog")), library.find("A")!!.words)
        assertEquals(listOf(Word("pes", "dog")), SetLibrary(storage()).find("A")!!.words)
    }

    @Test
    fun `prazdny text se ulozit smi`() {
        val library = SetLibrary(storage())
        library.create("A")
        library.save("A", "pes;dog")
        assertTrue(library.save("A", "") is SaveResult.Saved)
        assertTrue(library.find("A")!!.words.isEmpty())
    }

    @Test
    fun `ulozeni nove sady bez predchoziho vytvoreni ji zalozi`() {
        val library = SetLibrary(storage())
        assertTrue(library.save("Import", "a;b") is SaveResult.Saved)
        assertEquals(listOf("Import"), library.sets.value.map { it.name })
    }

    @Test
    fun `smazani odstrani sadu a soubor`() {
        val library = SetLibrary(storage())
        library.create("A")
        library.create("B")
        library.delete("A")
        assertEquals(listOf("B"), library.sets.value.map { it.name })
        assertFalse(File(folder.root, "sets/A.txt").exists())
        assertNull(library.find("A"))
    }

    // --- text sady ---

    @Test
    fun `format a parser jsou navzajem inverzni`() {
        val set = WordSet("X", listOf(Word("pondělí", "Monday", true), Word("ice cream", "zmrzlina".reversed())))
        val text = WordSetText.format(set)
        val parsed = cz.wordik.app.domain.WordSetParser.parse("X", text)
        assertEquals(set.words, parsed.words)
    }

    @Test
    fun `sloucen i kodu odstrani duplicity a zachova prvni vyskyt`() {
        val merged = WordSetText.merge(listOf("pes;dog\nkočka;cat\n", "kočka ; cat\nstůl;table\n\n", "pes;dog"))
        assertEquals("pes;dog\nkočka;cat\nstůl;table", merged)
    }

    @Test
    fun `stejne cesky ale jiny cizi preklad neni duplicita`() {
        val merged = WordSetText.merge(listOf("hrad;castle", "hrad;fort"))
        assertEquals(2, WordSetText.lineCount(merged))
    }

    @Test
    fun `sloucen i necha chybne radky aby se daly opravit`() {
        val merged = WordSetText.merge(listOf("bez oddelovace\npes;dog"))
        assertEquals("bez oddelovace\npes;dog", merged)
    }
}
