package photoglyph.core.img

import photoglyph.core.PipelineParams

/** BT.601 luma; alpha is composited onto white paper before thresholding. */
fun toGrayscale(image: ArgbImage): GrayImage {
    val gray =
        DoubleArray(image.width * image.height) { index ->
            val argb = image[index]
            val alpha = ((argb ushr ALPHA_SHIFT) and CHANNEL_MAX).toDouble() / CHANNEL_MAX
            val red = (argb ushr RED_SHIFT) and CHANNEL_MAX
            val green = (argb ushr GREEN_SHIFT) and CHANNEL_MAX
            val blue = argb and CHANNEL_MAX
            val luma = BT601_RED * red + BT601_GREEN * green + BT601_BLUE * blue
            (alpha * luma + (1.0 - alpha) * CHANNEL_MAX).coerceIn(0.0, CHANNEL_MAX.toDouble())
        }
    return GrayImage(image.width, image.height, gray)
}

/** Foreground iff value < clipped local mean - offset. O(width * height) integral image. */
fun adaptiveThreshold(
    image: GrayImage,
    params: PipelineParams = PipelineParams(),
): BinaryGrid {
    val width = image.width
    val height = image.height
    val integralWidth = Math.addExact(width, 1)
    val integralHeight = Math.addExact(height, 1)
    val integral = DoubleArray(Math.multiplyExact(integralWidth, integralHeight))
    for (y in 0 until height) {
        var rowSum = 0.0
        for (x in 0 until width) {
            rowSum += image[x, y]
            integral[(y + 1) * integralWidth + x + 1] = integral[y * integralWidth + x + 1] + rowSum
        }
    }
    val radius = params.adaptiveWindowSizePx / 2
    val output = BooleanArray(width * height)
    for (y in 0 until height) {
        val top = (y.toLong() - radius).coerceAtLeast(0).toInt()
        val bottom = (y.toLong() + radius + 1).coerceAtMost(height.toLong()).toInt()
        for (x in 0 until width) {
            val left = (x.toLong() - radius).coerceAtLeast(0).toInt()
            val right = (x.toLong() + radius + 1).coerceAtMost(width.toLong()).toInt()
            val sum =
                integral[bottom * integralWidth + right] - integral[top * integralWidth + right] -
                    integral[bottom * integralWidth + left] + integral[top * integralWidth + left]
            val area = (right - left).toDouble() * (bottom - top)
            output[y * width + x] = image[x, y] < sum / area - params.adaptiveOffset
        }
    }
    return BinaryGrid(width, height, output)
}

/** Eight-connected components with area strictly less than the parameter are removed. */
fun removeSmallComponents(
    input: BinaryGrid,
    params: PipelineParams = PipelineParams(),
): BinaryGrid {
    val output = input.copyPixels()
    val visited = BooleanArray(output.size)
    val queue = IntArray(output.size)
    for (seed in output.indices) {
        if (!input[seed] || visited[seed]) continue
        var head = 0
        var tail = 1
        queue[0] = seed
        visited[seed] = true
        while (head < tail) {
            val index = queue[head++]
            val x = index % input.width
            val y = index / input.width
            for ((dx, dy) in NEIGHBOURS_8) {
                val nx = x + dx
                val ny = y + dy
                if (!input[nx, ny]) continue
                val neighbour = ny * input.width + nx
                if (!visited[neighbour]) {
                    visited[neighbour] = true
                    queue[tail++] = neighbour
                }
            }
        }
        if (tail < params.minComponentAreaPx) {
            for (position in 0 until tail) output[queue[position]] = false
        }
    }
    return BinaryGrid(input.width, input.height, output)
}

// Pixel layout and BT.601 coefficients are mathematical/format constants, not tuning knobs.
private const val ALPHA_SHIFT = 24
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BT601_RED = 0.299
private const val BT601_GREEN = 0.587
private const val BT601_BLUE = 0.114
