package photoglyph.core.img

/** Integer raster bounds with an exclusive right and bottom edge. */
data class PixelBounds(
    val left: Int,
    val top: Int,
    val rightExclusive: Int,
    val bottomExclusive: Int,
) {
    init {
        require(left >= 0 && top >= 0 && rightExclusive > left && bottomExclusive > top)
    }

    val width: Int get() = rightExclusive - left
    val height: Int get() = bottomExclusive - top
}

/** One 8-connected ink region; this is a candidate component, not a recognized character. */
data class InkComponent(
    val bounds: PixelBounds,
    val pixelCount: Int,
) {
    init {
        require(pixelCount > 0 && pixelCount.toLong() <= bounds.width.toLong() * bounds.height)
    }
}

/** Returns every 8-connected foreground component in top-to-bottom raster scan order. */
fun connectedInkComponents(binary: BinaryGrid): List<InkComponent> {
    val width = binary.width
    val height = binary.height
    val visited = BooleanArray(width * height)
    val queue = IntArray(width * height)
    val components = ArrayList<InkComponent>()

    for (seed in 0 until width * height) {
        if (!binary[seed] || visited[seed]) continue
        var head = 0
        var tail = 1
        queue[0] = seed
        visited[seed] = true
        var minX = seed % width
        var maxX = minX
        var minY = seed / width
        var maxY = minY
        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
            for ((dx, dy) in NEIGHBOURS_8) {
                val neighbourX = x + dx
                val neighbourY = y + dy
                if (!binary[neighbourX, neighbourY]) continue
                val neighbour = neighbourY * width + neighbourX
                if (visited[neighbour]) continue
                visited[neighbour] = true
                queue[tail++] = neighbour
            }
        }
        components.add(InkComponent(PixelBounds(minX, minY, maxX + 1, maxY + 1), tail))
    }
    return components
}

/** Copies one bounded binary region; all non-foreground pixels remain background. */
fun cropBinaryGrid(
    binary: BinaryGrid,
    bounds: PixelBounds,
): BinaryGrid {
    require(bounds.rightExclusive <= binary.width && bounds.bottomExclusive <= binary.height)
    val output = BooleanArray(bounds.width * bounds.height)
    for (y in 0 until bounds.height) {
        for (x in 0 until bounds.width) {
            output[y * bounds.width + x] = binary[bounds.left + x, bounds.top + y]
        }
    }
    return BinaryGrid(bounds.width, bounds.height, output)
}
