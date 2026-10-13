package photoglyph.core.graph

import photoglyph.core.PipelineParams
import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.geom.simplifyRdp
import photoglyph.core.thin.SkeletonPixels

/** Raw chains are retained so tests/diagnostics can check edge coverage before RDP. */
data class StrokeExtraction(
    val polylines: List<Polyline>,
    val rawPolylines: List<Polyline>,
    val graphNodes: Int,
    val graphEdges: Int,
    val graphComponents: Int,
    val graphCycleRank: Int,
    val splitClosedChains: Int,
    val prunedEdges: Int,
    val suppressedCornerDiagonals: Int,
)

/**
 * Raster centers, Y down. This is an M0 edge partition, NOT recovery of pen motion.
 *
 * A diagonal is kept when it is the only 8-connected link. If an orthogonal
 * two-edge route already exists, omit that redundant diagonal: otherwise each
 * L corner creates a spurious triangle. This preserves pixel connectivity,
 * but is an explicit spike convention, not a claim about original pen strokes.
 * Every remaining graph edge is visited exactly once. Cyclic chains are split
 * into two open chains sharing endpoints; their loop geometry is not deleted.
 */
fun skeletonToPolylines(
    skeleton: SkeletonPixels,
    params: PipelineParams = PipelineParams(),
): StrokeExtraction {
    val grid = skeleton.grid
    val adjacency = linkedMapOf<Int, MutableSet<Int>>()
    for (y in 0 until grid.height) {
        for (x in 0 until grid.width) {
            if (grid[x, y]) adjacency[y * grid.width + x] = sortedSetOf()
        }
    }
    var suppressedDiagonals = 0
    for (index in adjacency.keys) {
        val x = index % grid.width
        val y = index / grid.width
        for ((dx, dy) in FORWARD_NEIGHBOURS_8) {
            if (!grid[x + dx, y + dy]) continue
            if (dx != 0 && dy != 0 && (grid[x + dx, y] || grid[x, y + dy])) {
                suppressedDiagonals++
                continue
            }
            val neighbour = (y + dy) * grid.width + x + dx
            adjacency.getValue(index).add(neighbour)
            adjacency.getValue(neighbour).add(index)
        }
    }

    fun point(index: Int): Vec2 =
        Vec2(
            index % grid.width + PIXEL_CENTER_OFFSET,
            index / grid.width + PIXEL_CENTER_OFFSET,
        )
    val initialEdgeCount = edgeCount(adjacency)
    repeat(params.pruningIterations) {
        val remove = linkedSetOf<Pair<Int, Int>>()
        for (start in adjacency.keys) {
            if (adjacency.getValue(start).size != 1) continue
            var previous = start
            var current = adjacency.getValue(start).first()
            var length = point(previous).distanceTo(point(current))
            val branch = mutableListOf(start to current)
            while (adjacency.getValue(current).size == 2) {
                val next = adjacency.getValue(current).first { it != previous }
                branch.add(current to next)
                length += point(current).distanceTo(point(next))
                previous = current
                current = next
            }
            // Do not erase short isolated strokes or punctuation components.
            if (adjacency.getValue(current).size > 2 && length < params.pruneBranchShorterThanPx) {
                remove.addAll(branch)
            }
        }
        for ((first, second) in remove) {
            adjacency.getValue(first).remove(second)
            adjacency.getValue(second).remove(first)
        }
        val touched = remove.flatMap { listOf(it.first, it.second) }.toSet()
        for (index in touched) {
            if (adjacency.getValue(index).isEmpty()) adjacency.remove(index)
        }
    }
    val finalEdgeCount = edgeCount(adjacency)
    val components = componentCount(adjacency)
    val visited = hashSetOf<Long>()
    val chains = mutableListOf<List<Int>>()

    fun walk(
        start: Int,
        neighbour: Int,
    ): List<Int> {
        val chain = mutableListOf(start, neighbour)
        visited.add(edgeKey(start, neighbour))
        var previous = start
        var current = neighbour
        while (adjacency.getValue(current).size == 2) {
            val next = adjacency.getValue(current).first { it != previous }
            if (!visited.add(edgeKey(current, next))) break
            chain.add(next)
            previous = current
            current = next
        }
        return chain
    }

    // Endpoints and junctions first; connected degree-two chains terminate there.
    for ((start, neighbours) in adjacency) {
        if (neighbours.size == 2) continue
        if (neighbours.isEmpty()) chains.add(listOf(start))
        for (neighbour in neighbours) {
            if (edgeKey(start, neighbour) !in visited) chains.add(walk(start, neighbour))
        }
    }
    // Remaining edges belong to components that are pure cycles.
    for ((start, neighbours) in adjacency) {
        for (neighbour in neighbours) {
            if (edgeKey(start, neighbour) !in visited) chains.add(walk(start, neighbour))
        }
    }
    check(visited.size == finalEdgeCount) { "Graph traversal must cover every edge exactly once" }
    val raw = mutableListOf<Polyline>()
    var splitCycles = 0
    for (chain in chains) {
        if (chain.size > 1 && chain.first() == chain.last()) {
            val middle = chain.size / 2
            raw.add(Polyline(chain.take(middle + 1).map(::point)))
            raw.add(Polyline(chain.drop(middle).map(::point)))
            splitCycles++
        } else {
            raw.add(Polyline(chain.map(::point)))
        }
    }
    check(raw.none { it.isClosed }) { "Cycle splitting must preserve open output chains" }
    return StrokeExtraction(
        polylines = raw.map { simplifyRdp(it, params.rdpEpsilonPx) },
        rawPolylines = raw.toList(),
        graphNodes = adjacency.size,
        graphEdges = finalEdgeCount,
        graphComponents = components,
        graphCycleRank = finalEdgeCount - adjacency.size + components,
        splitClosedChains = splitCycles,
        prunedEdges = initialEdgeCount - finalEdgeCount,
        suppressedCornerDiagonals = suppressedDiagonals,
    )
}

private fun edgeCount(adjacency: Map<Int, Set<Int>>): Int = adjacency.values.sumOf { it.size } / 2

private fun componentCount(adjacency: Map<Int, Set<Int>>): Int {
    val visited = hashSetOf<Int>()
    val pending = ArrayDeque<Int>()
    var count = 0
    for (seed in adjacency.keys) {
        if (!visited.add(seed)) continue
        count++
        pending.addLast(seed)
        while (pending.isNotEmpty()) {
            for (neighbour in adjacency.getValue(pending.removeFirst())) {
                if (visited.add(neighbour)) pending.addLast(neighbour)
            }
        }
    }
    return count
}

private fun edgeKey(
    first: Int,
    second: Int,
): Long = (minOf(first, second).toLong() shl INT_BITS) or maxOf(first, second).toLong()

private const val INT_BITS = 32
private const val PIXEL_CENTER_OFFSET = 0.5
private val FORWARD_NEIGHBOURS_8 = listOf(1 to 0, 0 to 1, 1 to 1, -1 to 1)
