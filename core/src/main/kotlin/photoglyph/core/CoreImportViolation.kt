package photoglyph.core

/** A forbidden dependency in executable Kotlin source, not a comment/example. */
data class CoreImportViolation(
    val importName: String,
    val lineNumber: Int,
)
