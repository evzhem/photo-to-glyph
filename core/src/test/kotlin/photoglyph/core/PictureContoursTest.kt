package photoglyph.core

import org.junit.Test

class PictureContoursTest {
    @Test fun straightStepProducesThinBlackContourWithoutMutatingSource() = PictureContoursChecks.straightStepProducesThinBlackContourWithoutMutatingSource()

    @Test fun uniformImageProducesNoContours() = PictureContoursChecks.uniformImageProducesNoContours()

    @Test fun thresholdControlsLowContrastEdges() = PictureContoursChecks.thresholdControlsLowContrastEdges()

    @Test fun thresholdMustMatchNormalizedSobelRange() = PictureContoursChecks.thresholdMustMatchNormalizedSobelRange()
}
