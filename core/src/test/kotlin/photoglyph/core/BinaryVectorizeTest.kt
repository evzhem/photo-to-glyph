package photoglyph.core

import org.junit.Test

class BinaryVectorizeTest {
    @Test fun preparedOnePixelLineProducesAnOpenCenterlineWithoutASecondThresholdPass() = BinaryVectorizeChecks.preparedOnePixelLineProducesAnOpenCenterlineWithoutASecondThresholdPass()

    @Test fun defaultBinaryPathPreservesSmallDetachedMarks() = BinaryVectorizeChecks.defaultBinaryPathPreservesSmallDetachedMarks()
}
