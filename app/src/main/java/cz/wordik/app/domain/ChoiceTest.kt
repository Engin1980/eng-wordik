package cz.wordik.app.domain

import kotlin.random.Random

/**
 * Jedna otázka režimu výběru z variant. [options] má N+1 položek (správná + N nesprávných)
 * v náhodném pořadí, [correctIndex] ukazuje na správnou.
 */
data class ChoiceQuestion(
    val word: Word,
    /** Strana, která se zobrazuje jako zadání. */
    val promptSide: Side,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
) {
    val correctText: String get() = options[correctIndex]
}

/** Strana slovíčka. */
enum class Side { CZECH, FOREIGN }

/** Vybrané sady nestačí na požadovaný počet různých možností. */
class NotEnoughOptionsException(val required: Int, val available: Int) :
    Exception("Pro $required možností je k dispozici jen $available různých")

/**
 * Generuje otázky: nesprávné varianty se losují ze všech slovíček vybraných sad dohromady,
 * text shodný se správnou odpovědí se vynechá a varianty se neopakují.
 *
 * @throws NotEnoughOptionsException pokud pro nějaké slovíčko nelze sestavit [wrongOptions] + 1 různých možností
 */
class ChoiceQuestionGenerator(
    private val allWords: List<Word>,
    private val direction: Direction,
    private val wrongOptions: Int,
    private val random: Random = Random.Default,
) {
    init {
        require(wrongOptions >= 1) { "Počet nesprávných variant musí být alespoň 1" }
        val sides = when (direction) {
            Direction.CZECH_TO_FOREIGN -> listOf(Side.FOREIGN)
            Direction.FOREIGN_TO_CZECH -> listOf(Side.CZECH)
            Direction.MIX -> listOf(Side.CZECH, Side.FOREIGN)
        }
        // Nejhorší případ: nejméně různých možností přes všechna slovíčka a použitelné směry.
        val available = allWords.minOfOrNull { word ->
            sides.minOf { answerSide -> distinctOthers(word, answerSide).size + 1 }
        } ?: 0
        if (available < wrongOptions + 1) throw NotEnoughOptionsException(wrongOptions + 1, available)
    }

    fun generate(word: Word): ChoiceQuestion {
        val answerSide = when (direction) {
            Direction.CZECH_TO_FOREIGN -> Side.FOREIGN
            Direction.FOREIGN_TO_CZECH -> Side.CZECH
            Direction.MIX -> if (random.nextBoolean()) Side.FOREIGN else Side.CZECH
        }
        val correct = text(word, answerSide)
        val wrong = distinctOthers(word, answerSide).shuffled(random).take(wrongOptions)
        val options = (wrong + correct).shuffled(random)
        return ChoiceQuestion(
            word = word,
            promptSide = if (answerSide == Side.FOREIGN) Side.CZECH else Side.FOREIGN,
            prompt = text(word, if (answerSide == Side.FOREIGN) Side.CZECH else Side.FOREIGN),
            options = options,
            correctIndex = options.indexOf(correct),
        )
    }

    /** Různé texty ze strany [answerSide] ostatních slovíček, bez textu shodného se správnou odpovědí. */
    private fun distinctOthers(word: Word, answerSide: Side): List<String> {
        val correct = text(word, answerSide)
        return allWords.map { text(it, answerSide) }.filter { it != correct }.distinct()
    }

    private fun text(word: Word, side: Side) = if (side == Side.CZECH) word.czech else word.foreign
}

/**
 * Propojení otázek se [TestSession]. Na otázku lze odpovědět jen jednou; další přijde po [next].
 */
class ChoiceTest(
    val session: TestSession,
    private val generator: ChoiceQuestionGenerator,
) {
    var question: ChoiceQuestion? = null
        private set

    /** Index zvolené možnosti, dokud se nepřejde dál; null před odpovědí. */
    var answeredIndex: Int? = null
        private set

    val isAnswered: Boolean get() = answeredIndex != null
    val isFinished: Boolean get() = session.isFinished

    /** True, pokud poslední odpověď byla správná; null před odpovědí. */
    val lastAnswerCorrect: Boolean? get() = answeredIndex?.let { it == question?.correctIndex }

    init {
        loadQuestion()
    }

    /** Vyhodnotí odpověď. Druhá odpověď na stejnou otázku se ignoruje (vrací null). */
    fun answer(optionIndex: Int): Boolean? {
        val q = question ?: return null
        if (isAnswered || optionIndex !in q.options.indices) return null
        answeredIndex = optionIndex
        val correct = optionIndex == q.correctIndex
        if (correct) session.addResult(1, 0) else session.addResult(0, 1)
        return correct
    }

    /** Přejde na další slovíčko; bez odpovědi se nic neděje. */
    fun next() {
        if (!isAnswered || session.isFinished) return
        session.completeWord()
        loadQuestion()
    }

    fun abort() {
        session.abort()
        question = null
        answeredIndex = null
    }

    fun result() = session.result()

    private fun loadQuestion() {
        answeredIndex = null
        question = session.current?.let { generator.generate(it) }
    }
}
