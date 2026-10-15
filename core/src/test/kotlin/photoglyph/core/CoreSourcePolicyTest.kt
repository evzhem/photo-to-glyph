package photoglyph.core

import org.junit.Test

class CoreSourcePolicyTest {
    @Test
    fun actualProductionSourcesHaveNoAndroidImports() {
        CoreSourcePolicyChecks.actualProductionSourcesHaveNoAndroidImports()
    }

    @Test
    fun rejectsQualifiedWildcardAndAliasedAndroidImports() {
        CoreSourcePolicyChecks.rejectsQualifiedWildcardAndAliasedAndroidImports()
    }

    @Test
    fun ignoresNestedCommentsAndStringExamples() {
        CoreSourcePolicyChecks.ignoresNestedCommentsAndStringExamples()
    }
}
