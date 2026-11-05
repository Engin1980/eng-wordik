package cz.wordik.app.ui

import cz.wordik.app.ui.results.ResultFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class ResultFormatTest {
    @Test
    fun `procento se zaokrouhluje`() {
        assertEquals("0 %", ResultFormat.percent(0.0))
        assertEquals("67 %", ResultFormat.percent(2.0 / 3.0))
        assertEquals("33 %", ResultFormat.percent(1.0 / 3.0))
        assertEquals("100 %", ResultFormat.percent(1.0))
        assertEquals("50 %", ResultFormat.percent(0.5))
    }

    @Test
    fun `doba trvani v sekundach`() {
        assertEquals("0 s", ResultFormat.duration(0))
        assertEquals("45 s", ResultFormat.duration(45_000))
        assertEquals("59 s", ResultFormat.duration(59_000))
    }

    @Test
    fun `doba trvani v minutach`() {
        assertEquals("1 min 00 s", ResultFormat.duration(60_000))
        assertEquals("2 min 05 s", ResultFormat.duration(125_000))
        assertEquals("59 min 59 s", ResultFormat.duration(3_599_000))
    }

    @Test
    fun `doba trvani v hodinach`() {
        assertEquals("1 h 00 min 00 s", ResultFormat.duration(3_600_000))
        assertEquals("1 h 02 min 05 s", ResultFormat.duration(3_725_000))
    }

    @Test
    fun `doba se zaokrouhluje na sekundy a zaporna je nula`() {
        assertEquals("2 s", ResultFormat.duration(1_600))
        assertEquals("1 s", ResultFormat.duration(1_400))
        assertEquals("0 s", ResultFormat.duration(-5))
    }

    @Test
    fun `hvezdicky podle uspesnosti`() {
        assertEquals(0, ResultFormat.stars(completed = false, rate = 1.0))
        assertEquals(1, ResultFormat.stars(true, 0.0))
        assertEquals(1, ResultFormat.stars(true, 0.69))
        assertEquals(2, ResultFormat.stars(true, 0.7))
        assertEquals(2, ResultFormat.stars(true, 0.89))
        assertEquals(3, ResultFormat.stars(true, 0.9))
        assertEquals(3, ResultFormat.stars(true, 1.0))
    }
}
