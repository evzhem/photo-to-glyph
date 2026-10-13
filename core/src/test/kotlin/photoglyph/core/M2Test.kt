package photoglyph.core

import org.junit.Test

class M2Test {
    @Test fun quarterTurnsArePixelExact() = M2Checks.quarterTurnsArePixelExact()

    @Test fun cropIsPixelExactAndRejectsOutside() = M2Checks.cropIsPixelExactAndRejectsOutside()

    @Test fun resizeIdentityAndAverageAreIndependentExpectations() = M2Checks.resizeIdentityAndAverageAreIndependentExpectations()

    @Test fun perspectiveMapsKnownCornersAndCenter() = M2Checks.perspectiveMapsKnownCornersAndCenter()

    @Test fun identityPerspectivePreservesArgb() = M2Checks.identityPerspectivePreservesArgb()

    @Test fun rectangularPerspectiveMatchesHandSpecifiedCrop() = M2Checks.rectangularPerspectiveMatchesHandSpecifiedCrop()

    @Test fun degenerateQuadsAndAllocationBoundsAreRejected() = M2Checks.degenerateQuadsAndAllocationBoundsAreRejected()

    @Test fun otsuHasHandDefinedBimodalThreshold() = M2Checks.otsuHasHandDefinedBimodalThreshold()

    @Test fun ambiguousBlankPageDoesNotInventDetection() = M2Checks.ambiguousBlankPageDoesNotInventDetection()

    @Test fun detectAnalyticYaw30SideLightCorners() = M2Checks.detectAnalyticYaw30SideLightCorners()

    @Test fun rectifyAnalyticPhotoAndAlignKnownGridProfiles() = M2Checks.rectifyAnalyticPhotoAndAlignKnownGridProfiles()

    @Test fun autoDetectedRectificationAlsoAlignsKnownGrid() = M2Checks.autoDetectedRectificationAlsoAlignsKnownGrid()

    @Test fun gridSheetHasOwnLabelsMarksAndEscaping() = M2Checks.gridSheetHasOwnLabelsMarksAndEscaping()
}
