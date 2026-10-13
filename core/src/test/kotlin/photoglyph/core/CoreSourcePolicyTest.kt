package photoglyph.core

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreSourcePolicyTest {
    @Test
    fun actualProductionSourcesHaveNoAndroidImports() {
        val root = File(checkNotNull(System.getProperty("photoglyph.projectRoot")))
        val sources = File(root, "core/src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertTrue("No production sources found", sources.isNotEmpty())
        val violations = sources.flatMap { source ->
            findForbiddenAndroidImports(source.readText()).map { "${source.relativeTo(root)}:${it.lineNumber}: ${it.importName}" }
        }
        assertEquals("Core must remain platform-independent", emptyList<String>(), violations)
    }

    @Test
    fun rejectsQualifiedWildcardAndAliasedAndroidImports() {
        val source = "package demo\nimport android . graphics . Bitmap as B\nimport android.util.*\nimport androidx.example.Type\n"
        assertEquals(
            listOf(CoreImportViolation("android.graphics.Bitmap", 2), CoreImportViolation("android.util.*", 3)),
            findForbiddenAndroidImports(source),
        )
    }

    @Test
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
        assertTrue(findForbiddenAndroidImports(source).isEmpty())
    }
}
