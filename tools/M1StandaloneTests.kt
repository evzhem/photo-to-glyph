package photoglyph.tools

import photoglyph.cli.sandbox.SandboxApiChecks
import photoglyph.core.CoreSourcePolicyChecks

/** Calls exactly the shared JUnit cases, never a copy of core or HTTP implementation. */
fun main() {
    photoglyph.core.main()
    val policyCases = listOf(
        "actualProductionSourcesHaveNoAndroidImports" to CoreSourcePolicyChecks::actualProductionSourcesHaveNoAndroidImports,
        "rejectsQualifiedWildcardAndAliasedAndroidImports" to CoreSourcePolicyChecks::rejectsQualifiedWildcardAndAliasedAndroidImports,
        "ignoresNestedCommentsAndStringExamples" to CoreSourcePolicyChecks::ignoresNestedCommentsAndStringExamples,
    )
    for ((name, run) in policyCases) {
        run()
        println("PASS CoreSourcePolicyTest.$name -> findForbiddenAndroidImports")
    }
    val apiCases = listOf(
        "fiveFixturesExecuteActualCore" to SandboxApiChecks::fiveFixturesExecuteActualCore,
        "parameterChangesReachActualCore" to SandboxApiChecks::parameterChangesReachActualCore,
        "uploadedPngExecutesActualCore" to SandboxApiChecks::uploadedPngExecutesActualCore,
        "uploadedJpegExecutesActualCore" to SandboxApiChecks::uploadedJpegExecutesActualCore,
        "invalidParametersReleaseTheCoreForRecovery" to SandboxApiChecks::invalidParametersReleaseTheCoreForRecovery,
        "badImagesReleaseTheCoreForRecovery" to SandboxApiChecks::badImagesReleaseTheCoreForRecovery,
        "oversizedInputsReleaseTheCoreForRecovery" to SandboxApiChecks::oversizedInputsReleaseTheCoreForRecovery,
        "routesAndLocalesPreserveAWorkingCore" to SandboxApiChecks::routesAndLocalesPreserveAWorkingCore,
    )
    for ((name, run) in apiCases) {
        run()
        println("PASS SandboxApiTest.$name -> HTTP + vectorize, strokesToSvg, requireStrokeOnlySvg")
    }
    println("Standalone real checks: 21 existing core + ${policyCases.size} source policy + ${apiCases.size} API; all passed")
    println("This is NOT a Gradle/Android result. The mandatory Gradle command is reported separately.")
}
