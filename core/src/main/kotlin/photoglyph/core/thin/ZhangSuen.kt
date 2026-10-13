package photoglyph.core.thin

import photoglyph.core.img.BinaryGrid

/** Immutable skeleton plus the number of complete two-pass rounds performed. */
data class SkeletonPixels(val grid: BinaryGrid, val iterations: Int)

/** Zhang-Suen's two subiterations. Deletions are simultaneous within each subiteration. */
fun zhangSuen(input: BinaryGrid): SkeletonPixels {
    val width = input.width
    val height = input.height
    val pixels = input.copyPixels()
    if (pixels.none { it }) return SkeletonPixels(input, 0)
    val deletions = IntArray(pixels.size)
    var rounds = 0
    fun at(x: Int, y: Int): Boolean =
        x >= 0 && y >= 0 && x < width && y < height && pixels[y * width + x]

    fun subiteration(firstPass: Boolean): Int {
        var count = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (!pixels[index]) continue
                // P2..P9 clockwise: N, NE, E, SE, S, SW, W, NW.
                val neighbours = booleanArrayOf(
                    at(x, y - 1), at(x + 1, y - 1), at(x + 1, y), at(x + 1, y + 1),
                    at(x, y + 1), at(x - 1, y + 1), at(x - 1, y), at(x - 1, y - 1),
                )
                val occupied = neighbours.count { it }
                if (occupied !in MIN_NEIGHBOURS..MAX_NEIGHBOURS) continue
                var transitions = 0
                for (n in neighbours.indices) {
                    if (!neighbours[n] && neighbours[(n + 1) % neighbours.size]) transitions++
                }
                if (transitions != ONE_CONNECTED_ARC) continue
                val north = neighbours[0]
                val east = neighbours[2]
                val south = neighbours[4]
                val west = neighbours[6]
                val protected = if (firstPass) {
                    (north && east && south) || (east && south && west)
                } else {
                    (north && east && west) || (north && south && west)
                }
                if (!protected) deletions[count++] = index
            }
        }
        for (position in 0 until count) pixels[deletions[position]] = false
        return count
    }

    do {
        val removed = subiteration(firstPass = true) + subiteration(firstPass = false)
        rounds++
    } while (removed > 0)
    return SkeletonPixels(BinaryGrid(width, height, pixels), rounds)
}

// These predicates define Zhang-Suen, not experimentally chosen thresholds.
private const val MIN_NEIGHBOURS = 2
private const val MAX_NEIGHBOURS = 6
private const val ONE_CONNECTED_ARC = 1
