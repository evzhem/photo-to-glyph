package photoglyph.cli.sandbox

import org.junit.Test

class PreparationApiTest {
    @Test fun realPageDetectionAndPreparation() = PreparationApiChecks.realPageDetectionAndPreparation()
    @Test fun manualCornersAndUploadedRasterReachCore() = PreparationApiChecks.manualCornersAndUploadedRasterReachCore()
    @Test fun pixelExactRotationAndCrop() = PreparationApiChecks.pixelExactRotationAndCrop()
    @Test fun badQuadAndParametersRecoverActualPreparation() = PreparationApiChecks.badQuadAndParametersRecoverActualPreparation()
    @Test fun printableSheetAndStaticRoutesDoNotFakeAnOutline() = PreparationApiChecks.printableSheetAndStaticRoutesDoNotFakeAnOutline()
}
