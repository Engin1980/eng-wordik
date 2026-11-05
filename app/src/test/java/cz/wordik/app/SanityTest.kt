package cz.wordik.app

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SanityTest {
    @Test
    fun `testovaci infrastruktura funguje`() = runTest {
        assertEquals(4, 2 + 2)
    }
}
