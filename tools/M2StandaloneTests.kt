package photoglyph.tools

import photoglyph.core.M2Checks
import photoglyph.cli.sandbox.PreparationApiChecks

fun main() {
    val cases = listOf(
        "mirrorIsPixelExactAndNonmutating -> mirrorHorizontal" to M2Checks::mirrorIsPixelExactAndNonmutating,
        "suggestedSizeDoesNotGuessA4 -> suggestedRectifiedSize" to M2Checks::suggestedSizeDoesNotGuessA4,
        "quarterTurnsArePixelExact -> rotateQuarterTurns" to M2Checks::quarterTurnsArePixelExact,
        "cropIsPixelExactAndRejectsOutside -> cropImage" to M2Checks::cropIsPixelExactAndRejectsOutside,
        "resizeIdentityAndAverageAreIndependentExpectations -> resizeToMaxEdge" to M2Checks::resizeIdentityAndAverageAreIndependentExpectations,
        "perspectiveMapsKnownCornersAndCenter -> mapPagePoint" to M2Checks::perspectiveMapsKnownCornersAndCenter,
        "identityPerspectivePreservesArgb -> rectifyPerspective" to M2Checks::identityPerspectivePreservesArgb,
        "rectangularPerspectiveMatchesHandSpecifiedCrop -> rectifyPerspective" to M2Checks::rectangularPerspectiveMatchesHandSpecifiedCrop,
        "degenerateQuadsAndAllocationBoundsAreRejected -> PageQuad, rectifyPerspective" to M2Checks::degenerateQuadsAndAllocationBoundsAreRejected,
        "otsuHasHandDefinedBimodalThreshold -> otsuThreshold" to M2Checks::otsuHasHandDefinedBimodalThreshold,
        "ambiguousBlankPageDoesNotInventDetection -> detectPageQuad" to M2Checks::ambiguousBlankPageDoesNotInventDetection,
        "detectAnalyticYaw30SideLightCorners -> detectPageQuad" to M2Checks::detectAnalyticYaw30SideLightCorners,
        "rectifyAnalyticPhotoAndAlignKnownGridProfiles -> prepareImage, whiteProjectionColumns, whiteProjectionRows" to M2Checks::rectifyAnalyticPhotoAndAlignKnownGridProfiles,
        "autoDetectedRectificationAlsoAlignsKnownGrid -> detectPageQuad, prepareImage, whiteProjectionColumns, whiteProjectionRows" to M2Checks::autoDetectedRectificationAlsoAlignsKnownGrid,
        "gridSheetHasOwnLabelsMarksAndEscaping -> gridSheetSvg, cyrillicGridCharacters" to M2Checks::gridSheetHasOwnLabelsMarksAndEscaping,
    )
    var failures = 0
    for ((name, run) in cases) {
        try { run(); println("PASS M2Test.$name") }
        catch (failure: Throwable) { failures++; System.err.println("FAIL M2Test.$name"); failure.printStackTrace() }
    }
    println("M2 real core checks: ${cases.size}, failures: $failures")
    check(failures == 0)
    val apiCases = listOf(
        "realPageDetectionAndPreparation" to PreparationApiChecks::realPageDetectionAndPreparation,
        "manualCornersAndUploadedRasterReachCore" to PreparationApiChecks::manualCornersAndUploadedRasterReachCore,
        "pixelExactRotationAndCrop" to PreparationApiChecks::pixelExactRotationAndCrop,
        "badQuadAndParametersRecoverActualPreparation" to PreparationApiChecks::badQuadAndParametersRecoverActualPreparation,
        "printableSheetAndStaticRoutesDoNotFakeAnOutline" to PreparationApiChecks::printableSheetAndStaticRoutesDoNotFakeAnOutline,
    )
    for ((name, run) in apiCases) {
        run()
        println("PASS PreparationApiTest.$name -> HTTP + detectPageQuad, prepareImage; cropImage/rotateQuarterTurns/gridSheetSvg as applicable")
    }
    println("M2 API checks: ${apiCases.size}, failures: 0")
}
