package photoglyph.core.img

import photoglyph.core.PipelineParams
import photoglyph.core.geom.Vec2
import kotlin.math.abs
import kotlin.math.roundToInt

/** Heuristic bright-paper detection, not document recognition. Ambiguous inputs return null. */
fun detectPageQuad(image: ArgbImage, params: PipelineParams = PipelineParams()): PageQuad? {
    val preview = resizeToMaxEdge(image, params.pageDetectionMaxEdgePx)
    val gray = toGrayscale(preview)
    val threshold = otsuThreshold(gray)
    val points = mutableListOf<Vec2>()
    var brightCount = 0
    for (y in 0 until preview.height) {
        var left = preview.width
        var right = -1
        for (x in 0 until preview.width) {
            if (gray[x, y] > threshold) {
                left = minOf(left, x); right = maxOf(right, x); brightCount++
            }
        }
        if (right >= left) {
            points.add(Vec2(left.toDouble(), y.toDouble()))
            points.add(Vec2((right + 1).toDouble(), (y + 1).toDouble()))
        }
    }
    val fraction = brightCount.toDouble() / (preview.width.toLong() * preview.height)
    if (points.size < QUAD_CORNERS || fraction !in params.pageMinBrightFraction..params.pageMaxBrightFraction) return null
    val selected = listOf(
        points.minBy { it.x + it.y }, points.maxBy { it.x - it.y },
        points.maxBy { it.x + it.y }, points.minBy { it.x - it.y },
    )
    if (selected.toSet().size != QUAD_CORNERS) return null
    val area = abs(selected.indices.sumOf { index ->
        val first = selected[index]; val next = selected[(index + 1) % selected.size]
        first.x * next.y - next.x * first.y
    }) / 2
    if (area / (preview.width.toLong() * preview.height) < params.pageMinQuadFraction) return null
    val scaleX = image.width.toDouble() / preview.width
    val scaleY = image.height.toDouble() / preview.height
    val scaled = selected.map { Vec2(it.x * scaleX, it.y * scaleY) }
    return try {
        PageQuad(scaled[0], scaled[1], scaled[2], scaled[3])
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Otsu on an independently specified 256-level grayscale histogram. */
fun otsuThreshold(image: GrayImage): Double {
    val histogram = LongArray(CHANNEL_MAX + 1)
    for (value in image.copyPixels()) histogram[value.roundToInt().coerceIn(0, CHANNEL_MAX)]++
    val count = image.width.toLong() * image.height
    val total = histogram.indices.sumOf { it * histogram[it].toDouble() }
    var lowerCount = 0L
    var lowerSum = 0.0
    var largestVariance = -1.0
    var selected = 0
    for (level in 0 until CHANNEL_MAX) {
        lowerCount += histogram[level]
        lowerSum += level * histogram[level]
        val upperCount = count - lowerCount
        if (lowerCount == 0L || upperCount == 0L) continue
        val difference = lowerSum / lowerCount - (total - lowerSum) / upperCount
        val variance = lowerCount.toDouble() * upperCount * difference * difference
        if (variance > largestVariance) {
            largestVariance = variance
            selected = level
        }
    }
    return selected.toDouble()
}

private const val QUAD_CORNERS = 4
