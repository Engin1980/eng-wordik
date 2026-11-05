package cz.wordik.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import cz.wordik.app.data.AssetSetFileSource
import cz.wordik.app.data.SetRepository
import cz.wordik.app.domain.Word
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Celý tok aplikace přes UI: výběr sady → nastavení → test v obou režimech → výsledky.
 * Testy vždy nastaví režim a hodnocení výslovně, protože se nastavení ukládá mezi běhy.
 */
@RunWith(AndroidJUnit4::class)
class FlowTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val words: List<Word> by lazy {
        val assets = InstrumentationRegistry.getInstrumentation().targetContext.assets
        SetRepository(AssetSetFileSource(assets)).loadAll().flatMap { it.words }
    }

    private fun click(text: String) {
        rule.onNodeWithText(text).performClick()
        rule.waitForIdle()
    }

    private fun clickInScroll(text: String) {
        rule.onNodeWithText(text).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun exists(text: String) = rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun waitForText(text: String, millis: Long = 5_000) {
        rule.waitUntil(millis) { exists(text) }
    }

    private fun promptText(): String =
        rule.onNodeWithTag("prompt").fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)!!.first().text

    private fun startTest(sets: List<String>, mode: String, evaluation: String? = null) {
        sets.forEach { click(it) }
        click("Pokračovat")
        clickInScroll(mode)
        if (evaluation != null) clickInScroll(evaluation)
        click("Začít test")
    }

    private fun resultsShown() = exists("Výsledky")

    /** Odpovídá správně na všechny otázky režimu 1, dokud se nezobrazí výsledky. */
    private fun playChoiceToEnd(maxSteps: Int = 300) {
        repeat(maxSteps) {
            if (resultsShown()) return
            val prompt = promptText()
            val answer = words.firstNotNullOf { w ->
                when (prompt) {
                    w.czech -> w.foreign
                    w.foreign -> w.czech
                    else -> null
                }
            }
            rule.onAllNodes(hasTestTag("option") and hasText(answer)).onFirst().performClick()
            rule.waitForIdle()
            if (!resultsShown()) click("Dál")
        }
        assertTrue("Test se neukončil", resultsShown())
    }

    /** Složí správně všechna slova režimu 2, dokud se nezobrazí výsledky. */
    private fun playAssemblyToEnd(wholeWord: Boolean, maxSteps: Int = 300) {
        repeat(maxSteps) {
            if (resultsShown()) return
            val prompt = promptText()
            val word = words.first { it.czech == prompt }
            word.foreign.forEach { c ->
                val label = if (c == ' ') "mezera" else c.toString()
                rule.onAllNodes(hasTestTag("tile") and hasText(label) and isEnabled()).onFirst().performClick()
                rule.waitForIdle()
            }
            if (wholeWord) click("Potvrdit")
            if (!resultsShown()) click("Dál")
        }
        assertTrue("Test se neukončil", resultsShown())
    }

    @Test
    fun pokracovatJeNedostupneBezVyberuSady() {
        rule.onNodeWithText("Pokračovat").assertIsNotEnabled()
        click("Zvířata")
        rule.onNodeWithText("Pokračovat").assertIsEnabled()
        click("Zvířata")
        rule.onNodeWithText("Pokračovat").assertIsNotEnabled()
    }

    @Test
    fun rezim1ProjdeAzKVysledkum() {
        startTest(listOf("Zvířata"), "Výběr z variant")
        playChoiceToEnd()
        rule.onNodeWithText("Výsledky").assertExists()
        rule.onNodeWithText("100 %").assertExists()
        assertTrue(!exists("Test nebyl dokončen"))
        rule.onNodeWithText("Žádná, výborně!").assertExists()
    }

    @Test
    fun rezim2PoPismenkachProjdeAzKVysledkum() {
        startTest(listOf("Škola a domov"), "Skládání slova z písmen", "Po písmenkách")
        playAssemblyToEnd(wholeWord = false)
        rule.onNodeWithText("100 %").assertExists()
        assertTrue(!exists("Test nebyl dokončen"))
    }

    @Test
    fun rezim2CeleSlovoProjdeAzKVysledkum() {
        startTest(listOf("Škola a domov"), "Skládání slova z písmen", "Celé slovo (po potvrzení)")
        playAssemblyToEnd(wholeWord = true)
        rule.onNodeWithText("100 %").assertExists()
    }

    @Test
    fun vicSadNajednou() {
        startTest(listOf("Zvířata", "Škola a domov"), "Výběr z variant")
        playChoiceToEnd(maxSteps = 400)
        rule.onNodeWithText("Výsledky").assertExists()
    }

    @Test
    fun chybnaOdpovedZvyraznisSpravnouAPokracujeAzPoDal() {
        startTest(listOf("Zvířata"), "Výběr z variant")
        val prompt = promptText()
        val correct = words.firstNotNullOf { w ->
            when (prompt) {
                w.czech -> w.foreign
                w.foreign -> w.czech
                else -> null
            }
        }
        rule.onAllNodes(hasTestTag("option") and !hasText(correct)).onFirst().performClick()
        rule.waitForIdle()
        // Po chybné odpovědi se nepřechází automaticky, ani po delší době.
        Thread.sleep(1_800)
        rule.waitForIdle()
        assertTrue(promptText() == prompt)
        rule.onNodeWithText("Dál").assertIsEnabled()
        click("Dál")
        assertTrue(promptText() != prompt)
    }

    @Test
    fun predcasneUkonceniZobraziNedokonceneVysledky() {
        startTest(listOf("Zvířata"), "Skládání slova z písmen", "Po písmenkách")
        click("Ukončit")
        rule.onNodeWithText("Ukončit test?").assertExists()
        click("Pokračovat v testu")
        assertTrue(!resultsShown())
        click("Ukončit")
        click("Ukončit test")
        waitForText("Test nebyl dokončen")
        rule.onNodeWithText("Výsledky").assertExists()
        click("Zpět na výběr sad")
        // Výběr sad se po návratu zachová, aby šlo snadno zkoušet znovu.
        rule.onNodeWithText("Pokračovat").assertIsEnabled()
    }

    @Test
    fun opakovaniTestuZnovuSpustiTest() {
        startTest(listOf("Zvířata"), "Výběr z variant")
        playChoiceToEnd()
        click("Opakovat test")
        rule.onNodeWithTag("prompt").assertExists()
        click("Ukončit")
        click("Ukončit test")
        waitForText("Výsledky")
    }

    @Test
    fun nastaveniVelikostiSeUlozi() {
        click("Nastavení")
        click("Velké")
        click("‹ Zpět")
        rule.onNodeWithText("Nastavení").assertExists()
        click("Nastavení")
        click("Normální")
        click("‹ Zpět")
        rule.onNodeWithText("Pokračovat").assertExists()
    }
}
