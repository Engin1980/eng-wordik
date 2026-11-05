package cz.wordik.app.domain

import kotlin.random.Random

/** Jedna "klávesa" v nabídce; [id] odlišuje i shodná písmena (např. dvě písmena "o" ve slově "book"). */
data class Tile(val id: Int, val char: Char)

/** Výsledek klepnutí na písmeno z nabídky. */
enum class PressResult {
    /** Písmeno se vložilo. */
    PLACED,

    /** Hodnocení po písmenkách: špatné písmeno, nevložilo se a započítala se chyba. */
    WRONG,

    /** Nic se nestalo (použité písmeno, plná slova, kolo už je vyhodnoceno). */
    IGNORED,
}

/**
 * Jedno slovíčko v režimu skládání: nabídka písmen, rozpracované slovo a hodnocení.
 *
 * Hodnotí se vždy cizí znění [Word.foreign]. Velikost písmen se nerozlišuje, kromě slovíčka
 * s příznakem [Word.caseSensitive]; diakritika je vždy jiné písmeno.
 *
 * @param extraLetters počet písmen navíc, losovaných z [letterPool]
 * @param letterPool povolená písmena pro losování navíc (jen písmena cizích slovíček vybraných sad)
 */
class AssemblyRound(
    val word: Word,
    val evaluation: Evaluation,
    extraLetters: Int = 0,
    letterPool: List<Char> = emptyList(),
    random: Random = Random.Default,
) {
    val target: String = word.foreign

    /** Nabídka v náhodném pořadí: všechny znaky slovíčka (i mezera, apostrof, pomlčka) a písmena navíc. */
    val tiles: List<Tile>

    private val slots = arrayOfNulls<Tile>(target.length)

    /** Počty jednotek pro statistiku: písmena (po písmenkách) nebo 0/1 za celé slovo. */
    var correctUnits = 0
        private set
    var wrongUnits = 0
        private set

    /** Hodnocení po písmenkách: počet špatných písmen. Hodnocení celého slova: 1 při špatném slově. */
    val hadError: Boolean get() = wrongUnits > 0

    /** Slovo je vyhodnoceno (po písmenkách zaplněné, jinak potvrzené). */
    var isDone = false
        private set

    /** Po slově s chybou je tlačítko "Dál" povinné a automatický přechod se nepoužije. */
    val needsManualNext: Boolean get() = isDone && hadError

    /** Po slově bez chyby se přechází automaticky po intervalu z konfigurace. */
    val advancesAutomatically: Boolean get() = isDone && !hadError

    val slotTiles: List<Tile?> get() = slots.toList()
    val filledCount: Int get() = slots.count { it != null }

    init {
        require(extraLetters >= 0) { "Počet písmen navíc nesmí být záporný" }
        val extras = if (letterPool.isEmpty()) emptyList() else List(extraLetters) { letterPool.random(random) }
        tiles = (target.toList() + extras)
            .shuffled(random)
            .mapIndexed { index, char -> Tile(index, char) }
    }

    fun isUsed(tile: Tile): Boolean = slots.any { it?.id == tile.id }

    /** Klepnutí na písmeno v nabídce. */
    fun press(tileId: Int): PressResult {
        val tile = tiles.firstOrNull { it.id == tileId }
        if (isDone || tile == null || isUsed(tile)) return PressResult.IGNORED

        return when (evaluation) {
            Evaluation.PER_LETTER -> {
                val position = filledCount
                if (matches(target[position], tile.char)) {
                    slots[position] = tile
                    correctUnits++
                    if (filledCount == target.length) isDone = true
                    PressResult.PLACED
                } else {
                    wrongUnits++
                    PressResult.WRONG
                }
            }
            Evaluation.WHOLE_WORD -> {
                val position = slots.indexOfFirst { it == null }
                if (position < 0) {
                    PressResult.IGNORED
                } else {
                    slots[position] = tile
                    PressResult.PLACED
                }
            }
        }
    }

    /** Vrátí vložené písmeno do nabídky (jen při hodnocení celého slova, před potvrzením). */
    fun remove(slotIndex: Int): Boolean {
        if (evaluation != Evaluation.WHOLE_WORD || isDone) return false
        if (slotIndex !in slots.indices || slots[slotIndex] == null) return false
        slots[slotIndex] = null
        return true
    }

    /**
     * Vyhodnotí celé slovo (jen při hodnocení celého slova). Lze kdykoliv, i s nevyplněným
     * slovem, které se pak vyhodnotí jako chyba. Vrací true při správném slově.
     */
    fun confirm(): Boolean? {
        if (evaluation != Evaluation.WHOLE_WORD || isDone) return null
        isDone = true
        val correct = slots.indices.all { i -> slots[i]?.let { matches(target[i], it.char) } == true }
        if (correct) correctUnits++ else wrongUnits++
        return correct
    }

    private fun matches(expected: Char, actual: Char) =
        if (word.caseSensitive) expected == actual else expected.equals(actual, ignoreCase = true)

    companion object {
        /** Písmena (ne mezera ani interpunkce) z cizích slovíček, ze kterých se losují písmena navíc. */
        fun letterPool(words: List<Word>): List<Char> =
            words.flatMap { it.foreign.toList() }.filter { it.isLetter() }.distinct()
    }
}

/** Propojení kol skládání se [TestSession]. */
class AssemblyTest(
    val session: TestSession,
    private val evaluation: Evaluation,
    private val extraLetters: Int,
    private val letterPool: List<Char>,
    private val random: Random = Random.Default,
) {
    var round: AssemblyRound? = null
        private set

    val isFinished: Boolean get() = session.isFinished

    init {
        loadRound()
    }

    fun press(tileId: Int): PressResult = track { it.press(tileId) } ?: PressResult.IGNORED

    fun remove(slotIndex: Int): Boolean = track { it.remove(slotIndex) } ?: false

    fun confirm(): Boolean? = track { it.confirm() }

    /** Přejde na další slovíčko; funguje jen po vyhodnoceném slově. */
    fun next() {
        val current = round ?: return
        if (!current.isDone || session.isFinished) return
        session.completeWord()
        loadRound()
    }

    fun abort() {
        session.abort()
        round = null
    }

    fun result() = session.result()

    /** Provede akci nad kolem a rozdíl jednotek zapíše do statistiky testu. */
    private fun <T> track(action: (AssemblyRound) -> T): T? {
        val current = round ?: return null
        val correctBefore = current.correctUnits
        val wrongBefore = current.wrongUnits
        val result = action(current)
        val dc = current.correctUnits - correctBefore
        val dw = current.wrongUnits - wrongBefore
        if (dc > 0 || dw > 0) session.addResult(dc, dw)
        return result
    }

    private fun loadRound() {
        round = session.current?.let {
            AssemblyRound(it, evaluation, extraLetters, letterPool, random)
        }
    }
}
