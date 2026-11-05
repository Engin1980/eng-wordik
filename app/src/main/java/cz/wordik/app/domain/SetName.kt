package cz.wordik.app.domain

enum class SetNameError { EMPTY, DUPLICATE, INVALID_CHARS, TOO_LONG }

/** Pravidla názvu sady; název je zároveň název souboru, proto jsou omezení stejná jako u souborů. */
object SetNameValidator {
    const val MAX_LENGTH = 60
    const val FORBIDDEN_CHARS = "\\/:*?\"<>|"

    /** Vrací první nalezenou chybu názvu, nebo null, je-li název v pořádku. [existing] jsou už použité názvy. */
    fun validate(name: String, existing: Collection<String>): SetNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> SetNameError.EMPTY
            trimmed.length > MAX_LENGTH -> SetNameError.TOO_LONG
            trimmed.any { it in FORBIDDEN_CHARS || it.isISOControl() } || trimmed.endsWith('.') ->
                SetNameError.INVALID_CHARS
            existing.any { it.equals(trimmed, ignoreCase = true) } -> SetNameError.DUPLICATE
            else -> null
        }
    }
}
