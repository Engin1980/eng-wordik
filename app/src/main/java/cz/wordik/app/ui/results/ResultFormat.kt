package cz.wordik.app.ui.results

import kotlin.math.roundToInt

/** Formátování hodnot na obrazovce výsledků (odděleno od UI kvůli testům). */
object ResultFormat {
    /** Celé procento zaokrouhlené na nejbližší, např. 0,666 → "67 %". */
    fun percent(rate: Double): String = "${(rate * 100).roundToInt()} %"

    /** Doba trvání: "45 s", "2 min 05 s", "1 h 02 min 05 s". */
    fun duration(millis: Long): String {
        val totalSeconds = (millis.coerceAtLeast(0) / 1000.0).roundToInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0 -> "$hours h ${minutes.pad()} min ${seconds.pad()} s"
            minutes > 0 -> "$minutes min ${seconds.pad()} s"
            else -> "$seconds s"
        }
    }

    /** Počet hvězdiček odměny (0 až 3): jen za dokončený test, aspoň jedna, 2 od 70 %, 3 od 90 %. */
    fun stars(completed: Boolean, rate: Double): Int = when {
        !completed -> 0
        rate >= 0.9 -> 3
        rate >= 0.7 -> 2
        else -> 1
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
