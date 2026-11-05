package cz.wordik.app.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Test

class PassProgressTest {
    private val words = List(3) { Word("cz$it", "en$it") }

    @Test
    fun `hotova slovicka pruchodu rostou a po pruchodu se nuluji`() {
        val s = TestSession(words, minPasses = 2, minSuccessPercent = 0, random = Random(1)) { 0 }
        assertEquals(3, s.wordCount)
        assertEquals(0, s.wordsDoneInPass)
        s.addResult(1, 0); s.completeWord()
        assertEquals(1, s.wordsDoneInPass)
        s.addResult(1, 0); s.completeWord()
        assertEquals(2, s.wordsDoneInPass)
        s.addResult(1, 0); s.completeWord()
        // nový průchod
        assertEquals(0, s.wordsDoneInPass)
        assertEquals(1, s.completedPasses)
    }
}
