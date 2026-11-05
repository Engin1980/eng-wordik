package cz.wordik.app.domain

enum class TestMode { CHOICE, ASSEMBLY }

/** Způsob hodnocení v režimu skládání slova. */
enum class Evaluation { WHOLE_WORD, PER_LETTER }

/** Směr v režimu výběru z variant. */
enum class Direction { CZECH_TO_FOREIGN, FOREIGN_TO_CZECH, MIX }

/**
 * Nastavení jednoho testu. "N" ze zadání je rozdělené na samostatná pole:
 * [wrongOptions] (režim [TestMode.CHOICE]), [minPasses], [minSuccessPercent] (oba režimy)
 * a [extraLetters] (režim [TestMode.ASSEMBLY]).
 */
data class TestSettings(
    val mode: TestMode,
    val wrongOptions: Int,
    val minPasses: Int,
    val minSuccessPercent: Int,
    val extraLetters: Int,
    val evaluation: Evaluation,
    val direction: Direction,
)

/** Povolené rozsahy hodnot nastavení (obě meze včetně). */
data class SettingsRanges(
    val wrongOptions: IntRange,
    val minPasses: IntRange,
    val minSuccessPercent: IntRange,
    val extraLetters: IntRange,
)

/** Obsah konfiguračního souboru aplikace. */
data class AppConfig(
    val defaults: TestSettings,
    val ranges: SettingsRanges,
    /** Prodleva před automatickým přechodem na další slovíčko po správné odpovědi. */
    val autoAdvanceMillis: Long,
    /** Název fontu pro slovíčka (viz konfigurační soubor). */
    val fontName: String,
)
