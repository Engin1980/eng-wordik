package cz.wordik.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Správa sad přes UI: vytvoření, editace s chybami, zahození změn, smazání, kontrola názvu. */
@RunWith(AndroidJUnit4::class)
class SetManagementTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val name = "UI test sada"

    /** Úklid kvůli případnému selhání uprostřed testu; ostatní testy nesmí vidět zbytky. */
    @After
    fun cleanUp() {
        val dir = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir, "sets")
        File(dir, "$name.txt").delete()
    }

    private fun click(text: String) {
        val node = rule.onNodeWithText(text)
        // Dialogy a dolní lišta se neposouvají, proto se posun zkouší jen tam, kde jde.
        runCatching { node.performScrollTo() }
        node.performClick()
        rule.waitForIdle()
    }

    private fun exists(text: String, substring: Boolean = false) =
        rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty()

    private fun enterName(value: String) {
        rule.onNodeWithTag("set_name").performTextInput(value)
        rule.waitForIdle()
    }

    private fun createSet() {
        click("Nová sada")
        enterName(name)
        click("Potvrdit název")
        rule.onNodeWithText("Sada: $name").assertExists()
    }

    private fun typeInEditor(text: String) {
        rule.onNodeWithTag("editor").performTextClearance()
        rule.onNodeWithTag("editor").performTextInput(text)
        rule.waitForIdle()
    }

    private fun deleteSet() {
        val delete = rule.onNodeWithContentDescription("Smazat $name")
        runCatching { delete.performScrollTo() }
        delete.performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Smazat sadu „$name“?").assertExists()
        click("Smazat sadu")
    }

    @Test
    fun vytvoreniEditaceASmazaniSady() {
        createSet()
        typeInEditor("pes;dog\nkočka;cat")
        click("Uložit")
        rule.onNodeWithText(name).assertExists()
        assertTrue(exists("2 slov", substring = true)) // tvar množného čísla závisí na jazyce zařízení

        deleteSet()
        assertTrue(!exists(name))
    }

    @Test
    fun chybnyTextSeNeulozi() {
        createSet()
        typeInEditor("pes;dog\nbez oddelovace")
        click("Uložit")
        rule.onNodeWithTag("editor_errors").assertExists()
        assertTrue(exists("Sadu nelze uložit, opravte tyto řádky:"))
        // Editor zůstává otevřený, po opravě se uloží.
        typeInEditor("pes;dog")
        click("Uložit")
        rule.onNodeWithText(name).assertExists()
        assertTrue(exists("1 slov", substring = true)) // tvar množného čísla závisí na jazyce zařízení
        deleteSet()
    }

    @Test
    fun zahozeniZmenVyzadujePotvrzeniAPrazdnaSadaSeUlozit() {
        createSet()
        typeInEditor("neulozene;zmeny")
        click("Zahodit změny")
        rule.onNodeWithText("Zahodit změny?").assertExists()
        click("Pokračovat v úpravách")
        rule.onNodeWithTag("editor").assertExists()
        click("Zahodit změny")
        click("Zahodit")
        // Zpět v seznamu; sada zůstala prázdná.
        rule.onNodeWithText(name).assertExists()
        assertTrue(exists("0 slov", substring = true)) // tvar množného čísla závisí na jazyce zařízení
        deleteSet()
    }

    @Test
    fun zahozeniBezZmenNeptaSeNaPotvrzeni() {
        createSet()
        click("Zahodit změny")
        assertTrue(!exists("Zahodit změny?"))
        rule.onNodeWithText(name).assertExists()
        deleteSet()
    }

    @Test
    fun nazevSeKontrolujeVDialogu() {
        click("Nová sada")
        click("Potvrdit název")
        assertTrue(exists("Zadejte název sady."))
        enterName("Zvířata")
        click("Potvrdit název")
        assertTrue(exists("Sada s tímto názvem už existuje."))
        rule.onNodeWithTag("set_name").performTextClearance()
        enterName("a/b")
        click("Potvrdit název")
        assertTrue(exists("Název nesmí obsahovat znaky", substring = true))
    }

    @Test
    fun importPoZadaniNazvuOtevreSkener() {
        click("Importovat")
        enterName(name)
        click("Potvrdit název")
        rule.onNodeWithText("Import: $name").assertExists()
        rule.onNodeWithText("Hotovo").assertExists()
        rule.onNodeWithText("Naskenováno kódů: 0, slovíček: 0").assertExists()
        // Bez naskenovaného kódu není co potvrdit; zpět nevytvoří žádnou sadu.
        click("‹ Zpět")
        assertTrue(!exists(name))
    }
}
