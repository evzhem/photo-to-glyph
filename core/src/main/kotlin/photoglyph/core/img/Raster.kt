package photoglyph.core.img

/** Platform-neutral immutable raster inputs. Constructors defensively copy arrays. */
class ArgbImage(
    val width: Int,
    val height: Int,
    pixels: IntArray,
) {
    private val data = pixels.copyOf()

    init {
        require(data.size == checkedArea(width, height))
    }

    operator fun get(index: Int): Int = data[index]

    fun copyPixels(): IntArray = data.copyOf()
}

class GrayImage(
    val width: Int,
    val height: Int,
    pixels: DoubleArray,
) {
    private val data = pixels.copyOf()

    init {
        require(data.size == checkedArea(width, height))
        require(data.all { it.isFinite() && it in 0.0..CHANNEL_MAX.toDouble() })
    }

    operator fun get(
        x: Int,
        y: Int,
    ): Double = data[y * width + x]

    fun copyPixels(): DoubleArray = data.copyOf()
}

class BinaryGrid(
    val width: Int,
    val height: Int,
    pixels: BooleanArray,
) {
    private val data = pixels.copyOf()

    init {
        require(data.size == checkedArea(width, height))
    }

    /** Outside the finite raster is background, including during border thinning. */
    operator fun get(
        x: Int,
        y: Int,
    ): Boolean = x >= 0 && y >= 0 && x < width && y < height && data[y * width + x]

    operator fun get(index: Int): Boolean = data[index]

    val foregroundCount: Int get() = data.count { it }

    fun copyPixels(): BooleanArray = data.copyOf()
}

internal const val CHANNEL_MAX = 255
internal val NEIGHBOURS_8 =
    listOf(
        -1 to -1,
        0 to -1,
        1 to -1,
        -1 to 0,
        1 to 0,
        -1 to 1,
        0 to 1,
        1 to 1,
    )

private fun checkedArea(
    width: Int,
    height: Int,
): Int {
    require(width > 0 && height > 0) { "Raster dimensions must be positive" }
    return Math.multiplyExact(width, height)
}
