package photoglyph.tools

import photoglyph.core.M2Checks

fun main() {
    val cases = listOf(
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
}
