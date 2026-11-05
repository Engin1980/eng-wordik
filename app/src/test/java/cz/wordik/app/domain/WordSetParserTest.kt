package cz.wordik.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WordSetParserTest {
    private fun parse(text: String) = WordSetParser.parse("test", text)

    private fun errorsOf(text: String): List<LineError> =
        try {
            parse(text)
            fail("Očekávala se WordSetParseException")
            emptyList()
        } catch (e: WordSetParseException) {
            e.errors
        }

    @Test
    fun `bezny radek`() {
        val set = parse("pes;dog")
        assertEquals("test", set.name)
        assertEquals(listOf(Word("pes", "dog", caseSensitive = false)), set.words)
    }

    @Test
    fun `orezava mezery kolem obou stran a radku`() {
        val set = parse("   pes  ;   dog \n")
        assertEquals(Word("pes", "dog"), set.words.single())
    }

    @Test
    fun `mezery uvnitr fraze zustavaji`() {
        val set = parse("zmrzlina;ice cream")
        assertEquals("ice cream", set.words.single().foreign)
    }

    @Test
    fun `ignoruje prazdne radky a radky jen s mezerami`() {
        val set = parse("\n\npes;dog\n   \n\t\nkočka;cat\n\n")
        assertEquals(listOf("pes", "kočka"), set.words.map { it.czech })
    }

    @Test
    fun `podporuje CRLF i LF`() {
        val set = parse("pes;dog\r\nkočka;cat\r\n")
        assertEquals(listOf("dog", "cat"), set.words.map { it.foreign })
    }

    @Test
    fun `ignoruje BOM na zacatku souboru`() {
        val set = parse(0xFEFF.toChar() + "pes;dog")
        assertEquals("pes", set.words.single().czech)
    }

    @Test
    fun `uvozovky nastavi priznak a odstrani se`() {
        val word = parse("pondělí;\"Monday\"").words.single()
        assertEquals("Monday", word.foreign)
        assertTrue(word.caseSensitive)
    }

    @Test
    fun `bez uvozovek se velikost pismen nerozlisuje`() {
        assertFalse(parse("pondělí;Monday").words.single().caseSensitive)
    }

    @Test
    fun `uvozovky jsou rovne i typograficke`() {
        val set = parse("a;\"One\"\nb;“Two”\nc;„Three“\nd;„Four”")
        assertEquals(listOf("One", "Two", "Three", "Four"), set.words.map { it.foreign })
        assertTrue(set.words.all { it.caseSensitive })
    }

    @Test
    fun `uvozovky kolem fraze`() {
        val word = parse("jsem;\"I am\"").words.single()
        assertEquals("I am", word.foreign)
        assertTrue(word.caseSensitive)
    }

    @Test
    fun `jen otevrena uvozovka neni priznak`() {
        val word = parse("x;\"abc").words.single()
        assertEquals("\"abc", word.foreign)
        assertFalse(word.caseSensitive)
    }

    @Test
    fun `uvozovky na ceske strane se neodstranuji`() {
        val word = parse("\"pes\";dog").words.single()
        assertEquals("\"pes\"", word.czech)
        assertFalse(word.caseSensitive)
    }

    @Test
    fun `prazdne uvozovky jsou chyba`() {
        assertEquals(1, errorsOf("x;\"\"").size)
    }

    @Test
    fun `apostrof pomlcka zavorky a interpunkce zustavaji`() {
        val set = parse("já jsem;I'm\nkrásný (muž);well-known (man)!")
        assertEquals("I'm", set.words[0].foreign)
        assertEquals("well-known (man)!", set.words[1].foreign)
    }

    @Test
    fun `diakritika a UTF-8`() {
        val word = parse("příliš žluťoučký kůň;ěščřžýáíéúůďťň").words.single()
        assertEquals("příliš žluťoučký kůň", word.czech)
        assertEquals("ěščřžýáíéúůďťň", word.foreign)
    }

    @Test
    fun `prazdny soubor je prazdna sada`() {
        assertTrue(parse("").words.isEmpty())
        assertTrue(parse("\n \n").words.isEmpty())
    }

    @Test
    fun `zachovava poradi`() {
        val set = parse("c;3\na;1\nb;2")
        assertEquals(listOf("c", "a", "b"), set.words.map { it.czech })
    }

    @Test
    fun `radek bez oddelovace je chyba s cislem radku`() {
        val errors = errorsOf("pes;dog\n\nkočka cat\n")
        assertEquals(1, errors.size)
        assertEquals(3, errors.single().lineNumber)
        assertEquals("kočka cat", errors.single().line)
    }

    @Test
    fun `vice oddelovacu je chyba`() {
        val errors = errorsOf("pes;dog;extra")
        assertEquals(1, errors.single().lineNumber)
        assertTrue(errors.single().message.contains("jen jednou"))
    }

    @Test
    fun `prazdna strana je chyba`() {
        assertEquals(1, errorsOf(";dog").size)
        assertEquals(1, errorsOf("pes;").size)
        assertEquals(1, errorsOf("pes;   ").size)
    }

    @Test
    fun `hlasi vsechny chyby najednou`() {
        val errors = errorsOf("ok;ok\nspatne\na;b;c\n;x\nok2;ok2")
        assertEquals(listOf(2, 3, 4), errors.map { it.lineNumber })
    }

    @Test
    fun `vyjimka obsahuje nazev sady a cisla radku`() {
        try {
            WordSetParser.parse("Zvířata", "pes;dog\nspatne")
            fail()
        } catch (e: WordSetParseException) {
            assertEquals("Zvířata", e.setName)
            assertTrue(e.message!!.contains("Zvířata"))
            assertTrue(e.message!!.contains("řádek 2"))
        }
    }
}
