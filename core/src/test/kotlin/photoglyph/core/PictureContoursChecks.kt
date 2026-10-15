package photoglyph.core

import photoglyph.core.img.ArgbImage
import photoglyph.core.img.blackContourImage

/** Production-function checks for the independent photo-to-contour workflow. */
object PictureContoursChecks {
    fun straightStepProducesThinBlackContourWithoutMutatingSource() {
        val sourcePixels = IntArray(49) { index -> if (index % IMAGE_WIDTH <= 2) BLACK_ARGB else WHITE_ARGB }
        val source = ArgbImage(IMAGE_WIDTH, IMAGE_HEIGHT, sourcePixels)
        val result = blackContourImage(source)
        val expected =
            IntArray(49) { WHITE_ARGB }.also { pixels ->
                for (y in 1..5) pixels[y * IMAGE_WIDTH + 2] = BLACK_ARGB
            }
        check(result.copyPixels().contentEquals(expected))
        check(source.copyPixels().contentEquals(sourcePixels))
    }

    fun uniformImageProducesNoContours() {
        val source = ArgbImage(5, 5, IntArray(25) { WHITE_ARGB })
        check(blackContourImage(source).copyPixels().all { it == WHITE_ARGB })
    }

    fun thresholdControlsLowContrastEdges() {
        val source = ArgbImage(IMAGE_WIDTH, IMAGE_HEIGHT, IntArray(49) { index -> if (index % IMAGE_WIDTH <= 2) GRAY_90 else GRAY_110 })
        val lowSensitivity = blackContourImage(source, PipelineParams(contourEdgeThreshold = 20.1))
        val highSensitivity = blackContourImage(source, PipelineParams(contourEdgeThreshold = 19.9))
        check(lowSensitivity.copyPixels().all { it == WHITE_ARGB })
        check(highSensitivity.copyPixels().count { it == BLACK_ARGB } == 5)
    }

    fun thresholdMustMatchNormalizedSobelRange() {
        rejects { PipelineParams(contourEdgeThreshold = Double.NaN) }
        rejects { PipelineParams(contourEdgeThreshold = -1.0) }
        rejects { PipelineParams(contourEdgeThreshold = 256.0) }
        check(PipelineParams(contourEdgeThreshold = 0.0).contourEdgeThreshold == 0.0)
        check(PipelineParams(contourEdgeThreshold = 255.0).contourEdgeThreshold == 255.0)
    }

    private fun rejects(run: () -> Unit) {
        var rejected = false
        try {
            run()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        check(rejected) { "Invalid contour threshold was not rejected" }
    }

    private const val IMAGE_WIDTH = 7
    private const val IMAGE_HEIGHT = 7
    private const val GRAY_90 = 0xff5a5a5a.toInt()
    private const val GRAY_110 = 0xff6e6e6e.toInt()
    private const val BLACK_ARGB = 0xff000000.toInt()
    private const val WHITE_ARGB = 0xffffffff.toInt()
}
