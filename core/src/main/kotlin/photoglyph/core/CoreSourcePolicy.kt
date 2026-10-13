package photoglyph.core

/** A forbidden dependency in executable Kotlin source, not a comment/example. */
data class CoreImportViolation(val importName: String, val lineNumber: Int)

/** Pure source-policy function used by the M1 architectural test on actual core files. */
fun findForbiddenAndroidImports(source: String): List<CoreImportViolation> {
    val code = maskCommentsAndStrings(source)
    return ANDROID_IMPORT.findAll(code).map { match ->
        val name = checkNotNull(match.groups[1])
        CoreImportViolation(
            importName = name.value.replace(WHITESPACE, ""),
            lineNumber = code.take(name.range.first).count { it == '\n' } + 1,
        )
    }.toList()
}

private fun maskCommentsAndStrings(source: String): String {
    val masked = source.toCharArray()
    var index = 0
    var state = SourceState.CODE
    var blockDepth = 0
    fun hide(count: Int) {
        repeat(count) {
            if (masked[index] != '\n' && masked[index] != '\r') masked[index] = ' '
            index++
        }
    }
    while (index < source.length) {
        when (state) {
            SourceState.CODE -> when {
                source.startsWith("//", index) -> {
                    hide(COMMENT_DELIMITER_LENGTH)
                    state = SourceState.LINE_COMMENT
                }
                source.startsWith("/*", index) -> {
                    hide(COMMENT_DELIMITER_LENGTH)
                    blockDepth = 1
                    state = SourceState.BLOCK_COMMENT
                }
                source.startsWith("\"\"\"", index) -> {
                    hide(TRIPLE_QUOTE_LENGTH)
                    state = SourceState.RAW_STRING
                }
                source[index] == '"' -> {
                    hide(1)
                    state = SourceState.STRING
                }
                source[index] == '\'' -> {
                    hide(1)
                    state = SourceState.CHARACTER
                }
                else -> index++
            }
            SourceState.LINE_COMMENT -> {
                if (source[index] == '\n') state = SourceState.CODE
                hide(1)
            }
            SourceState.BLOCK_COMMENT -> when {
                source.startsWith("/*", index) -> {
                    hide(COMMENT_DELIMITER_LENGTH)
                    blockDepth++
                }
                source.startsWith("*/", index) -> {
                    hide(COMMENT_DELIMITER_LENGTH)
                    blockDepth--
                    if (blockDepth == 0) state = SourceState.CODE
                }
                else -> hide(1)
            }
            SourceState.RAW_STRING -> {
                if (source.startsWith("\"\"\"", index)) {
                    hide(TRIPLE_QUOTE_LENGTH)
                    state = SourceState.CODE
                } else {
                    hide(1)
                }
            }
            SourceState.STRING, SourceState.CHARACTER -> {
                val end = if (state == SourceState.STRING) '"' else '\''
                when {
                    source[index] == '\\' && index + 1 < source.length -> hide(ESCAPE_LENGTH)
                    source[index] == end -> {
                        hide(1)
                        state = SourceState.CODE
                    }
                    else -> hide(1)
                }
            }
        }
    }
    return String(masked)
}

private enum class SourceState { CODE, LINE_COMMENT, BLOCK_COMMENT, STRING, RAW_STRING, CHARACTER }
private const val COMMENT_DELIMITER_LENGTH = 2
private const val TRIPLE_QUOTE_LENGTH = 3
private const val ESCAPE_LENGTH = 2
private val WHITESPACE = Regex("\\s+")
private val ANDROID_IMPORT = Regex(
    "(?m)^\\s*import\\s+(android(?:\\s*\\.\\s*(?:[\\p{L}\\p{N}_]+|\\*))*)(?=\\s|;|$)",
)
