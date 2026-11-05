package cz.wordik.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsCodecTest {
    private val ranges = SettingsRanges(1..8, 1..10, 1..100, 0..10)
    private val settings = TestSettings(
        mode = TestMode.CHOICE,
        wrongOptions = 6,
        minPasses = 3,
        minSuccessPercent = 80,
        extraLetters = 2,
        evaluation = Evaluation.WHOLE_WORD,
        direction = Direction.FOREIGN_TO_CZECH,
    )

    @Test
    fun `encode a decode jsou inverzni`() {
        assertEquals(settings, SettingsCodec.decode(SettingsCodec.encode(settings), ranges))
        val assembly = settings.copy(mode = TestMode.ASSEMBLY)
        assertEquals(assembly, SettingsCodec.decode(SettingsCodec.encode(assembly), ranges))
    }

    @Test
    fun `klic kombinace nezavisi na poradi a duplicitach`() {
        assertEquals(
            SettingsCodec.combinationKey(listOf("A", "B", "C")),
            SettingsCodec.combinationKey(listOf("C", "A", "B", "A")),
        )
    }

    @Test
    fun `jina kombinace ma jiny klic`() {
        assertNotEquals(SettingsCodec.combinationKey(listOf("A")), SettingsCodec.combinationKey(listOf("A", "B")))
        assertNotEquals(SettingsCodec.combinationKey(listOf("A", "B")), SettingsCodec.combinationKey(listOf("AB")))
    }

    @Test
    fun `nazvy s oddelovaci se nepletou`() {
        assertNotEquals(
            SettingsCodec.combinationKey(listOf("a|1:b")),
            SettingsCodec.combinationKey(listOf("a", "b")),
        )
    }

    @Test
    fun `poskozena data davaji null`() {
        listOf(
            "",
            "nesmysl",
            "v1,CHOICE,6,3,80,2,WHOLE_WORD",
            "v1,CHOICE,6,3,80,2,WHOLE_WORD,MIX,navic",
            "v1,CHOICE,x,3,80,2,WHOLE_WORD,MIX",
            "v1,MODE9,6,3,80,2,WHOLE_WORD,MIX",
            "v1,CHOICE,6,3,80,2,NEZNAME,MIX",
            "v1,CHOICE,6,3,80,2,WHOLE_WORD,NEZNAME",
        ).forEach { assertNull("Mělo být null: $it", SettingsCodec.decode(it, ranges)) }
    }

    @Test
    fun `jina verze formatu davaji null`() {
        assertNull(SettingsCodec.decode("v2,CHOICE,6,3,80,2,WHOLE_WORD,MIX", ranges))
    }

    @Test
    fun `hodnoty mimo aktualni rozsah davaji null`() {
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings.copy(minPasses = 11)), ranges))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings.copy(minSuccessPercent = 0)), ranges))
        assertNull(SettingsCodec.decode(SettingsCodec.encode(settings.copy(wrongOptions = 9)), ranges))
        val assembly = settings.copy(mode = TestMode.ASSEMBLY)
        assertNull(SettingsCodec.decode(SettingsCodec.encode(assembly.copy(extraLetters = 11)), ranges))
    }

    @Test
    fun `skryte pole mimo rozsah neprekazi`() {
        // V režimu 2 se počet variant nepoužívá, takže nevadí, že je mimo rozsah.
        val assembly = settings.copy(mode = TestMode.ASSEMBLY, wrongOptions = 99)
        assertEquals(assembly, SettingsCodec.decode(SettingsCodec.encode(assembly), ranges))
    }
}
