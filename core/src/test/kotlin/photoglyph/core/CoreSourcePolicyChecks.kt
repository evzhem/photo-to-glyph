package photoglyph.core

import java.io.File

/** Shared real test cases for JUnit and the network-restricted standalone runner. */
object CoreSourcePolicyChecks {
    fun actualProductionSourcesHaveNoAndroidImports() {
        val root = File(checkNotNull(System.getProperty("photoglyph.projectRoot")))
        val sources = File(root, "core/src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        check(sources.isNotEmpty()) { "No production sources found" }
        val violations = sources.flatMap { source ->
            findForbiddenAndroidImports(source.readText()).map { "${source.relativeTo(root)}:${it.lineNumber}: ${it.importName}" }
        }
        check(violations.isEmpty()) { "Core must remain platform-independent: $violations" }
    }

    fun rejectsQualifiedWildcardAndAliasedAndroidImports() {
        val source = "package demo\nimport android . graphics . Bitmap as B\nimport android.util.*\nimport androidx.example.Type\n"
        val expected = listOf(CoreImportViolation("android.graphics.Bitmap", 2), CoreImportViolation("android.util.*", 3))
        check(findForbiddenAndroidImports(source) == expected)
    }

    fun ignoresNestedCommentsAndStringExamples() {
        val source = listOf(
            "/* import android.fake.One",
            " /* nested comment */",
            "import android.fake.Two */",
            "// import android.fake.Three",
            "val text = \"import android.fake.Four\"",
            "val raw = \"\"\"",
            "import android.fake.Five",
            "\"\"\"",
            "import kotlin.math.hypot",
        ).joinToString("\n")
        check(findForbiddenAndroidImports(source).isEmpty())
    }
}
