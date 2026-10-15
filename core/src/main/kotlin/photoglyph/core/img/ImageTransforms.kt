package photoglyph.core.img

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** Immutable, pixel-exact clockwise rotations. Negative turns are allowed. */
fun rotateQuarterTurns(
    image: ArgbImage,
    turns: Int,
): ArgbImage {
    val normalized = Math.floorMod(turns, QUARTER_TURNS_PER_REVOLUTION)
    if (normalized == 0) return ArgbImage(image.width, image.height, image.copyPixels())
    val width = if (normalized % 2 == 1) image.height else image.width
    val height = if (normalized % 2 == 1) image.width else image.height
    val pixels = IntArray(Math.multiplyExact(width, height))
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val target =
                when (normalized) {
                    1 -> (image.height - 1 - y) to x
                    2 -> (image.width - 1 - x) to (image.height - 1 - y)
                    else -> y to (image.width - 1 - x)
                }
            pixels[target.second * width + target.first] = image[y * image.width + x]
        }
    }
    return ArgbImage(width, height, pixels)
}

/** EXIF horizontal mirror, implemented in the same pure raster core as rotation. */
fun mirrorHorizontal(image: ArgbImage): ArgbImage {
    val pixels =
        IntArray(image.width * image.height) { index ->
            image[index / image.width * image.width + image.width - 1 - index % image.width]
        }
    return ArgbImage(image.width, image.height, pixels)
}

/** Crop uses integer raster edge coordinates; no mutation or implicit resampling. */
fun cropImage(
    image: ArgbImage,
    left: Int,
    top: Int,
    width: Int,
    height: Int,
): ArgbImage {
    require(left >= 0 && top >= 0 && width > 0 && height > 0)
    require(left.toLong() + width <= image.width && top.toLong() + height <= image.height)
    val pixels =
        IntArray(Math.multiplyExact(width, height)) { index ->
            image[(top + index / width) * image.width + left + index % width]
        }
    return ArgbImage(width, height, pixels)
}

/** Estimated aspect ratio from averaged quad edges; it does not infer paper millimeters. */
fun suggestedRectifiedSize(
    quad: PageQuad,
    maxEdgePx: Int,
): Pair<Int, Int> {
    require(maxEdgePx > 0)
    val width = (quad.topLeft.distanceTo(quad.topRight) + quad.bottomLeft.distanceTo(quad.bottomRight)) / 2
    val height = (quad.topLeft.distanceTo(quad.bottomLeft) + quad.topRight.distanceTo(quad.bottomRight)) / 2
    val scale = minOf(1.0, maxEdgePx / max(width, height))
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}

/** Bounded preview resize. Input is never enlarged; identity preserves every ARGB bit. */
fun resizeToMaxEdge(
    image: ArgbImage,
    maxEdgePx: Int,
): ArgbImage {
    require(maxEdgePx > 0)
    val scale = minOf(1.0, maxEdgePx.toDouble() / max(image.width, image.height))
    if (scale == 1.0) return ArgbImage(image.width, image.height, image.copyPixels())
    val width = max(1, (image.width * scale).roundToInt())
    val height = max(1, (image.height * scale).roundToInt())
    val pixels =
        IntArray(Math.multiplyExact(width, height)) { index ->
            sampleArgb(image, (index % width + PIXEL_CENTER_OFFSET) * image.width / width - PIXEL_CENTER_OFFSET, (index / width + PIXEL_CENTER_OFFSET) * image.height / height - PIXEL_CENTER_OFFSET)
        }
    return ArgbImage(width, height, pixels)
}

/** Bilinear sampling in pixel-center index coordinates, clamped at finite raster borders. */
internal fun sampleArgb(
    image: ArgbImage,
    x: Double,
    y: Double,
): Int {
    val clippedX = x.coerceIn(0.0, (image.width - 1).toDouble())
    val clippedY = y.coerceIn(0.0, (image.height - 1).toDouble())
    val left = floor(clippedX).toInt()
    val top = floor(clippedY).toInt()
    val right = minOf(left + 1, image.width - 1)
    val bottom = minOf(top + 1, image.height - 1)
    val fractionX = clippedX - left
    val fractionY = clippedY - top
    val values = intArrayOf(image[top * image.width + left], image[top * image.width + right], image[bottom * image.width + left], image[bottom * image.width + right])
    val weights = doubleArrayOf((1 - fractionX) * (1 - fractionY), fractionX * (1 - fractionY), (1 - fractionX) * fractionY, fractionX * fractionY)
    if (fractionX == 0.0 && fractionY == 0.0) return values[0]
    val alpha = values.indices.sumOf { index -> ((values[index] ushr ALPHA_SHIFT) and CHANNEL_MAX) * weights[index] }
    if (alpha == 0.0) return 0
    var result = alpha.roundToInt().coerceIn(0, CHANNEL_MAX) shl ALPHA_SHIFT
    // Premultiplied interpolation avoids dark/colored halos around transparent drawings.
    for (shift in RGB_SHIFTS) {
        val channel =
            values.indices.sumOf { index ->
                ((values[index] ushr shift) and CHANNEL_MAX).toDouble() * ((values[index] ushr ALPHA_SHIFT) and CHANNEL_MAX) * weights[index]
            } / alpha
        result = result or (channel.roundToInt().coerceIn(0, CHANNEL_MAX) shl shift)
    }
    return result
}

private const val QUARTER_TURNS_PER_REVOLUTION = 4
private val RGB_SHIFTS = intArrayOf(16, 8, 0)
private const val ALPHA_SHIFT = 24
internal const val PIXEL_CENTER_OFFSET = 0.5
