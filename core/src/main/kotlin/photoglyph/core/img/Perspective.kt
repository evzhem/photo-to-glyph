package photoglyph.core.img

import photoglyph.core.PipelineParams
import photoglyph.core.geom.Vec2
import kotlin.math.abs

/** Explicit requested output dimensions; neither font units nor page type are guessed. */
fun rectifyPerspective(image: ArgbImage, quad: PageQuad, width: Int, height: Int, params: PipelineParams = PipelineParams()): ArgbImage {
    quad.requireInside(image)
    require(width > 0 && height > 0 && width.toLong() * height <= params.maxPreparedPixels)
    val matrix = unitSquareHomography(quad)
    val pixels = IntArray(Math.multiplyExact(width, height)) { index ->
        val source = project(matrix, Vec2((index % width + 0.5) / width, (index / width + 0.5) / height))
        sampleArgb(image, source.x - 0.5, source.y - 0.5)
    }
    return ArgbImage(width, height, pixels)
}

/** Useful for overlays/oracles: maps a unit-square point into the supplied page quad. */
fun mapPagePoint(quad: PageQuad, point: Vec2): Vec2 = project(unitSquareHomography(quad), point)

private fun unitSquareHomography(quad: PageQuad): DoubleArray {
    val unit = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 1.0))
    val system = Array(HOMOGRAPHY_UNKNOWNS) { DoubleArray(HOMOGRAPHY_UNKNOWNS + 1) }
    for (index in unit.indices) {
        val (x, y) = unit[index]
        val (u, v) = quad.corners[index]
        val first = system[index * 2]
        val second = system[index * 2 + 1]
        first[0] = x; first[1] = y; first[2] = 1.0; first[6] = -u * x; first[7] = -u * y; first[8] = u
        second[3] = x; second[4] = y; second[5] = 1.0; second[6] = -v * x; second[7] = -v * y; second[8] = v
    }
    // Independently implemented Gauss-Jordan elimination with partial pivoting.
    for (column in 0 until HOMOGRAPHY_UNKNOWNS) {
        val pivot = (column until HOMOGRAPHY_UNKNOWNS).maxBy { abs(system[it][column]) }
        require(abs(system[pivot][column]) > GEOMETRY_EPSILON) { "Degenerate perspective" }
        val old = system[column]; system[column] = system[pivot]; system[pivot] = old
        val divisor = system[column][column]
        for (entry in column..HOMOGRAPHY_UNKNOWNS) system[column][entry] /= divisor
        for (row in 0 until HOMOGRAPHY_UNKNOWNS) {
            if (row == column) continue
            val factor = system[row][column]
            for (entry in column..HOMOGRAPHY_UNKNOWNS) system[row][entry] -= factor * system[column][entry]
        }
    }
    return DoubleArray(HOMOGRAPHY_UNKNOWNS + 1) { index -> if (index == HOMOGRAPHY_UNKNOWNS) 1.0 else system[index][HOMOGRAPHY_UNKNOWNS] }
}

private fun project(matrix: DoubleArray, point: Vec2): Vec2 {
    val denominator = matrix[6] * point.x + matrix[7] * point.y + matrix[8]
    require(abs(denominator) > GEOMETRY_EPSILON)
    return Vec2((matrix[0] * point.x + matrix[1] * point.y + matrix[2]) / denominator, (matrix[3] * point.x + matrix[4] * point.y + matrix[5]) / denominator)
}

private const val HOMOGRAPHY_UNKNOWNS = 8
