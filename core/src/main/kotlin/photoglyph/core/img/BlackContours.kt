package photoglyph.core.img

import photoglyph.core.PipelineParams
import kotlin.math.abs
import kotlin.math.hypot

/** Creates a black-on-white raster edge image, separate from handwriting centerline vectorization. */
fun blackContourImage(
    image: ArgbImage,
    params: PipelineParams = PipelineParams(),
): ArgbImage {
    val grayscale = toGrayscale(image)
    val width = image.width
    val height = image.height
    val gradientsX = DoubleArray(width * height)
    val gradientsY = DoubleArray(width * height)
    val magnitudes = DoubleArray(width * height)

    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val topLeft = grayscale[x - 1, y - 1]
            val top = grayscale[x, y - 1]
            val topRight = grayscale[x + 1, y - 1]
            val left = grayscale[x - 1, y]
            val right = grayscale[x + 1, y]
            val bottomLeft = grayscale[x - 1, y + 1]
            val bottom = grayscale[x, y + 1]
            val bottomRight = grayscale[x + 1, y + 1]
            val index = y * width + x
            val gradientX = -topLeft + topRight - 2.0 * left + 2.0 * right - bottomLeft + bottomRight
            val gradientY = -topLeft - 2.0 * top - topRight + bottomLeft + 2.0 * bottom + bottomRight
            gradientsX[index] = gradientX
            gradientsY[index] = gradientY
            magnitudes[index] = hypot(gradientX, gradientY) / SOBEL_NORMALIZATION
        }
    }

    val output = IntArray(width * height) { WHITE_ARGB }
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val index = y * width + x
            val magnitude = magnitudes[index]
            if (magnitude < params.contourEdgeThreshold) continue
            val gradientX = gradientsX[index]
            val gradientY = gradientsY[index]
            val delta = nmsDirection(gradientX, gradientY)
            val before = magnitudes[(y - delta.second) * width + x - delta.first]
            val after = magnitudes[(y + delta.second) * width + x + delta.first]
            if (magnitude > before && magnitude >= after) output[index] = BLACK_ARGB
        }
    }
    return ArgbImage(width, height, output)
}

private fun nmsDirection(
    gradientX: Double,
    gradientY: Double,
): Pair<Int, Int> {
    val absoluteX = abs(gradientX)
    val absoluteY = abs(gradientY)
    return when {
        absoluteY * DIAGONAL_RATIO <= absoluteX -> HORIZONTAL
        absoluteX * DIAGONAL_RATIO <= absoluteY -> VERTICAL
        gradientX * gradientY >= 0.0 -> DIAGONAL_DOWN_RIGHT
        else -> DIAGONAL_UP_RIGHT
    }
}

private const val SOBEL_NORMALIZATION = 4.0
private const val DIAGONAL_RATIO = 2.414213562373095
private const val BLACK_ARGB = 0xff000000.toInt()
private const val WHITE_ARGB = 0xffffffff.toInt()
private val HORIZONTAL = 1 to 0
private val VERTICAL = 0 to 1
private val DIAGONAL_DOWN_RIGHT = 1 to 1
private val DIAGONAL_UP_RIGHT = 1 to -1
