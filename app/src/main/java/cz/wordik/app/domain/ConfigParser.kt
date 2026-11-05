package cz.wordik.app.domain

import java.io.StringReader
import java.util.Properties

/** Konfigurační soubor je neúplný nebo obsahuje nepřípustnou hodnotu. */
class ConfigException(message: String) : Exception(message)

/** Parser konfiguračního souboru (formát `.properties`, UTF-8). */
object ConfigParser {
    /** @throws ConfigException při chybějící nebo chybné hodnotě; výchozí hodnoty musí ležet v rozsazích. */
    fun parse(text: String): AppConfig {
        val props = Properties().apply { load(StringReader(text)) }

        fun string(key: String): String =
            props.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw ConfigException("Chybí hodnota „$key“")

        fun int(key: String): Int =
            string(key).toIntOrNull() ?: throw ConfigException("Hodnota „$key“ musí být celé číslo, je „${string(key)}“")

        fun range(key: String): IntRange {
            val value = string(key)
            val parts = value.split('-')
            val from = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val to = parts.getOrNull(1)?.trim()?.toIntOrNull()
            if (parts.size != 2 || from == null || to == null || from > to) {
                throw ConfigException("Rozsah „$key“ musí mít tvar „od-do“, je „$value“")
            }
            return from..to
        }

        fun <E : Enum<E>> enum(key: String, values: List<E>): E {
            val value = string(key)
            return values.firstOrNull { it.name == value }
                ?: throw ConfigException(
                    "Hodnota „$key“ musí být jedna z ${values.joinToString { it.name }}, je „$value“",
                )
        }

        val ranges = SettingsRanges(
            wrongOptions = range("range.wrongOptions"),
            minPasses = range("range.minPasses"),
            minSuccessPercent = range("range.minSuccessPercent"),
            extraLetters = range("range.extraLetters"),
        )
        val defaults = TestSettings(
            mode = enum("default.mode", TestMode.entries),
            wrongOptions = int("default.wrongOptions"),
            minPasses = int("default.minPasses"),
            minSuccessPercent = int("default.minSuccessPercent"),
            extraLetters = int("default.extraLetters"),
            evaluation = enum("default.evaluation", Evaluation.entries),
            direction = enum("default.direction", Direction.entries),
        )
        checkInRange("default.wrongOptions", defaults.wrongOptions, ranges.wrongOptions)
        checkInRange("default.minPasses", defaults.minPasses, ranges.minPasses)
        checkInRange("default.minSuccessPercent", defaults.minSuccessPercent, ranges.minSuccessPercent)
        checkInRange("default.extraLetters", defaults.extraLetters, ranges.extraLetters)

        val autoAdvance = int("autoAdvanceMillis")
        if (autoAdvance < 0) throw ConfigException("Hodnota „autoAdvanceMillis“ nesmí být záporná")

        return AppConfig(
            defaults = defaults,
            ranges = ranges,
            autoAdvanceMillis = autoAdvance.toLong(),
            fontName = string("font"),
        )
    }

    private fun checkInRange(key: String, value: Int, range: IntRange) {
        if (value !in range) {
            throw ConfigException("Výchozí hodnota „$key“ ($value) leží mimo povolený rozsah ${range.first}-${range.last}")
        }
    }
}
