package photoglyph.cli.sandbox

import org.junit.Test

class SandboxApiTest {
    @Test
    fun fiveFixturesExecuteActualCore() = SandboxApiChecks.fiveFixturesExecuteActualCore()

    @Test
    fun parameterChangesReachActualCore() = SandboxApiChecks.parameterChangesReachActualCore()

    @Test
    fun uploadedPngExecutesActualCore() = SandboxApiChecks.uploadedPngExecutesActualCore()

    @Test
    fun uploadedJpegExecutesActualCore() = SandboxApiChecks.uploadedJpegExecutesActualCore()

    @Test
    fun invalidParametersReleaseTheCoreForRecovery() = SandboxApiChecks.invalidParametersReleaseTheCoreForRecovery()

    @Test
    fun badImagesReleaseTheCoreForRecovery() = SandboxApiChecks.badImagesReleaseTheCoreForRecovery()

    @Test
    fun oversizedInputsReleaseTheCoreForRecovery() = SandboxApiChecks.oversizedInputsReleaseTheCoreForRecovery()

    @Test
    fun routesAndLocalesPreserveAWorkingCore() = SandboxApiChecks.routesAndLocalesPreserveAWorkingCore()
}
