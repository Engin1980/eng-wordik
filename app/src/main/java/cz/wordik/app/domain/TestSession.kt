package cz.wordik.app.domain

import java.text.Collator
import java.util.Locale
import kotlin.random.Random

/** Výsledky jednoho slovíčka přes všechny průchody (odpovědi, u hodnocení po písmenkách písmena). */
data class WordResult(val word: Word, val correct: Int, val wrong: Int) {
    val total: Int get() = correct + wrong
    val successRate: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
}

/**
 * Výsledek testu. [words] obsahuje jen slovíčka, na která se došlo (alespoň jedna odpověď),
 * seřazená podle úspěšnosti od nejnižší, při shodě podle abecedy.
 */
data class TestResult(
    /** False při předčasném ukončení ("Test nebyl dokončen"). */
    val completed: Boolean,
    val correct: Int,
    val wrong: Int,
    val durationMillis: Long,
    val words: List<WordResult>,
) {
    val total: Int get() = correct + wrong
    val successRate: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
    val wrongWords: List<Word> get() = words.filter { it.wrong > 0 }.map { it.word }
}

/**
 * Stavový automat testu nezávislý na režimu: pořadí slovíček, průchody, podmínka konce a statistiky.
 *
 * Použití: pro [current] se zaznamená výsledek ([addResult], u písmen i opakovaně) a slovíčko
 * se uzavře [completeWord]; tam se kontroluje konec. Jednotkou statistiky je odpověď,
 * resp. písmeno při hodnocení po písmenkách.
 *
 * @param random zdroj náhodnosti vkládaný zvenku (kvůli testům)
 * @param clock hodiny v milisekundách vkládané zvenku (kvůli testům)
 */
class TestSession(
    private val words: List<Word>,
    private val minPasses: Int,
    private val minSuccessPercent: Int,
    private val random: Random = Random.Default,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    init {
        require(words.isNotEmpty()) { "Test potřebuje alespoň jedno slovíčko" }
        require(minPasses >= 1) { "Minimální počet průchodů musí být alespoň 1" }
        require(minSuccessPercent in 0..100) { "Úspěšnost musí být 0 až 100 %" }
    }

    private val correctByWord = IntArray(words.size)
    private val wrongByWord = IntArray(words.size)
    private val queue = ArrayDeque<Int>()
    private val startedAt = clock()
    private var endedAt: Long? = null
    private var lastAskedIndex = -1

    /** Počet dokončených celých průchodů. */
    var completedPasses = 0
        private set

    var isFinished = false
        private set

    /** True, pokud test ukončil uživatel před splněním podmínek. */
    var wasAborted = false
        private set

    val totalCorrect: Int get() = correctByWord.sum()
    val totalWrong: Int get() = wrongByWord.sum()

    val successRate: Double
        get() = (totalCorrect + totalWrong).let { if (it == 0) 0.0 else totalCorrect.toDouble() / it }

    /** Počet slovíček v jednom průchodu. */
    val wordCount: Int get() = words.size

    /** Slovíček už hotových v rozpracovaném průchodu. */
    val wordsDoneInPass: Int get() = words.size - queue.size

    /** Slovíček zbývajících v rozpracovaném průchodu včetně aktuálního. */
    val wordsLeftInPass: Int get() = queue.size

    /** Aktuálně zkoušené slovíčko, po konci testu null. */
    val current: Word? get() = if (isFinished) null else words[queue.first()]

    init {
        refill()
    }

    /** Přičte výsledek k aktuálnímu slovíčku. */
    fun addResult(correct: Int, wrong: Int) {
        check(!isFinished) { "Test už skončil" }
        require(correct >= 0 && wrong >= 0) { "Počty nesmí být záporné" }
        val index = queue.first()
        correctByWord[index] += correct
        wrongByWord[index] += wrong
    }

    /** Uzavře aktuální slovíčko, posune se na další a zkontroluje podmínky konce. */
    fun completeWord() {
        check(!isFinished) { "Test už skončil" }
        lastAskedIndex = queue.removeFirst()
        if (queue.isEmpty()) completedPasses++

        if (completedPasses >= minPasses && isSuccessTargetReached()) {
            finish(aborted = false)
        } else if (queue.isEmpty()) {
            refill()
        }
    }

    /** Předčasné ukončení uživatelem. */
    fun abort() {
        if (!isFinished) finish(aborted = true)
    }

    fun result(): TestResult {
        val end = endedAt ?: clock()
        val asked = words.indices
            .filter { correctByWord[it] + wrongByWord[it] > 0 }
            .map { WordResult(words[it], correctByWord[it], wrongByWord[it]) }
        return TestResult(
            completed = isFinished && !wasAborted,
            correct = totalCorrect,
            wrong = totalWrong,
            durationMillis = end - startedAt,
            words = asked.sortedWith(RESULT_ORDER),
        )
    }

    private fun isSuccessTargetReached(): Boolean {
        val total = totalCorrect + totalWrong
        return total > 0 && totalCorrect.toLong() * 100 >= minSuccessPercent.toLong() * total
    }

    private fun finish(aborted: Boolean) {
        isFinished = true
        wasAborted = aborted
        endedAt = clock()
    }

    /** Nový náhodný průchod; první slovíčko se nesmí shodovat s právě zkoušeným. */
    private fun refill() {
        val order = words.indices.shuffled(random).toMutableList()
        if (order.size > 1 && order.first() == lastAskedIndex) {
            val other = random.nextInt(1, order.size)
            order[0] = order[other].also { order[other] = order[0] }
        }
        queue.addAll(order)
    }

    private companion object {
        val collator: Collator = Collator.getInstance(Locale.forLanguageTag("cs-CZ"))

        /** Podle úspěšnosti vzestupně (porovnání zlomků násobením), při shodě abecedně. */
        val RESULT_ORDER = Comparator<WordResult> { a, b ->
            val byRate = (a.correct.toLong() * b.total).compareTo(b.correct.toLong() * a.total)
            if (byRate != 0) byRate
            else collator.compare(a.word.czech, b.word.czech).takeIf { it != 0 }
                ?: collator.compare(a.word.foreign, b.word.foreign)
        }
    }
}
