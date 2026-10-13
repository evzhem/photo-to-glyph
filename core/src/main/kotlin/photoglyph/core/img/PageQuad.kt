package photoglyph.core.img

import photoglyph.core.geom.Vec2

/** Clockwise page corners in raster edge coordinates: (0,0)..(width,height), Y down. */
data class PageQuad(
    val topLeft: Vec2,
    val topRight: Vec2,
    val bottomRight: Vec2,
    val bottomLeft: Vec2,
) {
    val corners: List<Vec2> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)

    init {
        val points = listOf(topLeft, topRight, bottomRight, bottomLeft)
        for (index in points.indices) {
            val start = points[index]
            val next = points[(index + 1) % points.size]
            val after = points[(index + 2) % points.size]
            val cross = (next.x - start.x) * (after.y - next.y) - (next.y - start.y) * (after.x - next.x)
            require(cross > GEOMETRY_EPSILON) { "Page corners must form a nondegenerate clockwise convex quad" }
        }
    }

    fun requireInside(image: ArgbImage) {
        require(corners.all { it.x in 0.0..image.width.toDouble() && it.y in 0.0..image.height.toDouble() }) {
            "Page corners are outside the image"
        }
    }

    companion object {
        fun fullImage(image: ArgbImage): PageQuad = PageQuad(
            Vec2(0.0, 0.0), Vec2(image.width.toDouble(), 0.0),
            Vec2(image.width.toDouble(), image.height.toDouble()), Vec2(0.0, image.height.toDouble()),
        )
    }
}

// Numerical degeneracy tolerance, not an experimental image-processing parameter.
internal const val GEOMETRY_EPSILON = 1e-10
