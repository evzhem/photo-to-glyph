package photoglyph.core.geom

import kotlin.math.hypot

/** Coordinates are doubles. Image stages use Y down; normalized glyphs will use Y up. */
data class Vec2(
    val x: Double,
    val y: Double,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Coordinates must be finite" }
    }

    fun distanceTo(other: Vec2): Double = hypot(x - other.x, y - other.y)
}

/** A one-point path represents an isolated pen dot, not a closed contour. */
data class Polyline(
    val points: List<Vec2>,
) {
    init {
        require(points.isNotEmpty()) { "A path needs at least one point" }
    }

    val isClosed: Boolean get() = points.size > 1 && points.first() == points.last()
}

/** Iterative Ramer-Douglas-Peucker: no recursion depth depends on input length. */
fun simplifyRdp(
    path: Polyline,
    epsilonPx: Double,
): Polyline {
    require(epsilonPx.isFinite() && epsilonPx >= 0.0)
    val points = path.points
    if (points.size <= 2) return path
    val keep = BooleanArray(points.size)
    keep[0] = true
    keep[points.lastIndex] = true
    val pending = ArrayDeque<Pair<Int, Int>>()
    pending.addLast(0 to points.lastIndex)
    val epsilonSquared = epsilonPx * epsilonPx
    while (pending.isNotEmpty()) {
        val (first, last) = pending.removeLast()
        var furthest = -1
        var greatestDistance = epsilonSquared
        for (index in first + 1 until last) {
            val distance = distanceSquaredToSegment(points[index], points[first], points[last])
            if (distance > greatestDistance) {
                greatestDistance = distance
                furthest = index
            }
        }
        if (furthest >= 0) {
            keep[furthest] = true
            pending.addLast(first to furthest)
            pending.addLast(furthest to last)
        }
    }
    return Polyline(points.filterIndexed { index, _ -> keep[index] })
}

private fun distanceSquaredToSegment(
    point: Vec2,
    start: Vec2,
    end: Vec2,
): Double {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val lengthSquared = dx * dx + dy * dy
    val fraction =
        if (lengthSquared == 0.0) {
            0.0
        } else {
            (((point.x - start.x) * dx + (point.y - start.y) * dy) / lengthSquared).coerceIn(0.0, 1.0)
        }
    val errorX = point.x - start.x - fraction * dx
    val errorY = point.y - start.y - fraction * dy
    return errorX * errorX + errorY * errorY
}
