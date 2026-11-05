package cz.wordik.app.ui

import cz.wordik.app.data.FileSetStorage
import cz.wordik.app.data.SetLibrary
import cz.wordik.app.domain.SetNameError
import cz.wordik.app.ui.setup.QrImportViewModel
import cz.wordik.app.ui.setup.SetEditorViewModel
import cz.wordik.app.ui.setup.SetSelectionViewModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SetManagementViewModelsTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun library() = SetLibrary(FileSetStorage(File(folder.root, "sets")))

    // --- výběr sad ---

    @Test
    fun `nova sada se objevi v seznamu a prazdny nazev se odmitne`() {
        val library = library()
        val vm = SetSelectionViewModel(library.sets, library)
        assertEquals(SetNameError.EMPTY, vm.nameError(""))
        assertEquals("Nova", vm.createSet(" Nova "))
        assertEquals(listOf("Nova"), vm.setsState.value.map { it.name })
        assertEquals(SetNameError.DUPLICATE, vm.nameError("nova"))
    }

    @Test
    fun `smazani odebere sadu z vyberu i ze seznamu`() {
        val library = library()
        library.save("A", "a;a")
        library.save("B", "b;b")
        val vm = SetSelectionViewModel(library.sets, library)
        vm.toggle("A")
        vm.toggle("B")
        vm.deleteSet("A")
        assertEquals(listOf("B"), vm.selectedNames())
        assertEquals(listOf("B"), vm.setsState.value.map { it.name })
        assertEquals(1, vm.selectedWordCount())
        assertTrue(vm.canContinue())
    }

    @Test
    fun `text sady pro editor odpovida ulozenym slovickum`() {
        val library = library()
        library.save("A", "pes;dog\nkočka;\"Cat\"")
        val vm = SetSelectionViewModel(library.sets, library)
        assertEquals("pes;dog\nkočka;\"Cat\"", vm.setText("A"))
    }

    // --- editor ---

    @Test
    fun `editor bez zmen neni dirty a po uprave ano`() {
        val library = library()
        library.save("A", "pes;dog")
        val vm = SetEditorViewModel("A", "pes;dog", library)
        assertFalse(vm.isDirty)
        vm.onTextChange("pes;dog\nkočka;cat")
        assertTrue(vm.isDirty)
        vm.onTextChange("pes;dog")
        assertFalse(vm.isDirty)
    }

    @Test
    fun `editor ulozi platny text`() {
        val library = library()
        library.create("A")
        val vm = SetEditorViewModel("A", "", library)
        vm.onTextChange("pes;dog\nkočka;cat")
        assertTrue(vm.save())
        assertEquals(2, library.find("A")!!.words.size)
        assertTrue(vm.state.value.errors.isEmpty())
    }

    @Test
    fun `editor s chybou neulozi ukaze cisla radku a chyba zmizi po uprave`() {
        val library = library()
        library.save("A", "pes;dog")
        val vm = SetEditorViewModel("A", "pes;dog", library)
        vm.onTextChange("pes;dog\nspatne\n\nx;y;z")
        assertFalse(vm.save())
        assertEquals(listOf(2, 4), vm.state.value.errors.map { it.lineNumber })
        assertEquals(setOf(2, 4), vm.state.value.errorLines)
        assertEquals(1, library.find("A")!!.words.size) // původní obsah zůstal
        vm.onTextChange("pes;dog\nkočka;cat")
        assertTrue(vm.state.value.errors.isEmpty())
        assertTrue(vm.save())
    }

    @Test
    fun `zahozeni zmen nic nezapise`() {
        val library = library()
        library.save("A", "pes;dog")
        val vm = SetEditorViewModel("A", "pes;dog", library)
        vm.onTextChange("uplne;jine")
        // zahození = ViewModel se prostě zahodí bez volání save()
        assertEquals("pes;dog", library.text("A"))
    }

    @Test
    fun `importovana sada se zalozi az ulozenim v editoru`() {
        val library = library()
        val qr = QrImportViewModel("Import")
        qr.onCode("pes;dog\nkočka;cat")
        val editor = SetEditorViewModel("Import", qr.merged(), library)
        assertNull(library.find("Import"))
        assertTrue(editor.save())
        assertEquals(2, library.find("Import")!!.words.size)
    }

    // --- QR import ---

    @Test
    fun `qr import pocita ruzne kody a opakovane skenovani stejneho ignoruje`() {
        val vm = QrImportViewModel("X")
        assertTrue(vm.onCode("pes;dog\nkočka;cat"))
        assertFalse(vm.onCode("pes;dog\nkočka;cat"))
        assertFalse(vm.onCode("  "))
        assertTrue(vm.onCode("kočka;cat\nstůl;table"))
        assertEquals(2, vm.state.value.codes)
        assertEquals(3, vm.state.value.words)
        assertEquals("pes;dog\nkočka;cat\nstůl;table", vm.merged())
    }

    @Test
    fun `qr import bez kodu je prazdny`() {
        val vm = QrImportViewModel("X")
        assertEquals(0, vm.state.value.codes)
        assertEquals("", vm.merged())
    }
}
