package cz.wordik.app.domain

/**
 * Jedno slovíčko. [foreign] je text bez případných uvozovek; ty se v souboru
 * sady projeví jen příznakem [caseSensitive] (v režimu 2 se pak rozlišuje velikost písmen).
 */
data class Word(
    val czech: String,
    val foreign: String,
    val caseSensitive: Boolean = false,
)

/** Sada slovíček; [name] je název souboru bez přípony. */
data class WordSet(
    val name: String,
    val words: List<Word>,
)
