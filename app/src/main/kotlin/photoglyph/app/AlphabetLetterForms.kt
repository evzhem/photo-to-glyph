package photoglyph.app

/** Letter cases are separate glyph entries in a handwriting set. */
internal enum class HandwritingScript {
    CYRILLIC,
    LATIN,
}

internal data class AlphabetCaseProgress(
    val uppercaseAdded: Int,
    val uppercaseTotal: Int,
    val lowercaseAdded: Int,
    val lowercaseTotal: Int,
)

/** Production catalog used by the builder's case picker and coverage indicator. */
internal object AlphabetLetterForms {
    fun uppercase(script: HandwritingScript): List<String> =
        when (script) {
            HandwritingScript.CYRILLIC -> CYRILLIC_UPPERCASE.map { character -> character.toString() }
            HandwritingScript.LATIN -> LATIN_UPPERCASE.map { character -> character.toString() }
        }

    fun lowercase(script: HandwritingScript): List<String> =
        uppercase(script).map { character -> character.lowercase() }

    fun progress(
        script: HandwritingScript,
        savedLabels: Set<String>,
    ): AlphabetCaseProgress {
        val uppercase = uppercase(script)
        val lowercase = lowercase(script)
        return AlphabetCaseProgress(
            uppercaseAdded = uppercase.count { character -> character in savedLabels },
            uppercaseTotal = uppercase.size,
            lowercaseAdded = lowercase.count { character -> character in savedLabels },
            lowercaseTotal = lowercase.size,
        )
    }

    private const val CYRILLIC_UPPERCASE = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
    private const val LATIN_UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
}
