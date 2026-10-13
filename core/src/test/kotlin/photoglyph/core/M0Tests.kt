package photoglyph.core

import photoglyph.core.export.requireStrokeOnlySvg
import photoglyph.core.export.strokesToSvg
import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.geom.simplifyRdp
import photoglyph.core.graph.skeletonToPolylines
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.BinaryGrid
import photoglyph.core.img.GrayImage
import photoglyph.core.img.adaptiveThreshold
import photoglyph.core.img.removeSmallComponents
import photoglyph.core.img.toGrayscale
import photoglyph.core.thin.SkeletonPixels
import photoglyph.core.thin.zhangSuen
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.abs

/** Standalone pre-M1 test runner. Tests compile AGAINST the production JAR, not copied logic. */
fun main() {
    GrayscaleTest.register()
    AdaptiveThresholdTest.register()
    DespeckleTest.register()
    ZhangSuenTest.register()
    SkeletonToPolylinesTest.register()
    RdpTest.register()
    SvgNoFillTest.register()
    PipelineTest.register()
    Tests.run()
}

private object Tests {
    private data class Case(
        val name: String,
        val functions: String,
        val body: () -> Unit,
    )

    private val cases = mutableListOf<Case>()

    fun add(
        name: String,
        functions: String,
        body: () -> Unit,
    ) {
        cases.add(Case(name, functions, body))
    }

    fun run() {
        var failures = 0
        for (case in cases) {
            try {
                case.body()
                println("PASS ${case.name} -> ${case.functions}")
            } catch (failure: Throwable) {
                failures++
                System.err.println("FAIL ${case.name} -> ${case.functions}")
                failure.printStackTrace()
            }
        }
        println("Tests: ${cases.size}, passed: ${cases.size - failures}, failed: $failures")
        check(failures == 0) { "Real core tests failed" }
    }
}

private object GrayscaleTest {
    fun register() {
        Tests.add("GrayscaleTest.bt601", "toGrayscale") {
            val image = ArgbImage(3, 1, intArrayOf(0xffff0000.toInt(), 0xff00ff00.toInt(), 0xff0000ff.toInt()))
            val gray = toGrayscale(image)
            near(gray[0, 0], 76.245)
            near(gray[1, 0], 149.685)
            near(gray[2, 0], 29.070)
        }
        Tests.add("GrayscaleTest.alphaOnWhite", "toGrayscale") {
            val gray = toGrayscale(ArgbImage(2, 1, intArrayOf(0x00000000, 0x80000000.toInt())))
            near(gray[0, 0], 255.0)
            near(gray[1, 0], 127.0)
        }
    }
}

private object AdaptiveThresholdTest {
    fun register() {
        Tests.add("AdaptiveThresholdTest.localMeanAndOffset", "adaptiveThreshold") {
            val values = DoubleArray(9) { 200.0 }.also { it[4] = 0.0 }
            val image = GrayImage(3, 3, values)
            val actual = adaptiveThreshold(image, PipelineParams(adaptiveWindowSizePx = 3))
            pixelsEqual(actual, grid(3, 3, listOf(1 to 1)))
            val highOffset = adaptiveThreshold(image, PipelineParams(adaptiveWindowSizePx = 3, adaptiveOffset = 200.0))
            assertThat(highOffset.foregroundCount == 0)
        }
        Tests.add("AdaptiveThresholdTest.clippedBorderWindow", "adaptiveThreshold") {
            val image = GrayImage(3, 1, doubleArrayOf(60.0, 120.0, 180.0))
            val actual = adaptiveThreshold(image, PipelineParams(adaptiveWindowSizePx = 3))
            pixelsEqual(actual, grid(3, 1, listOf(0 to 0)))
            assertThat(image.copyPixels().contentEquals(doubleArrayOf(60.0, 120.0, 180.0)))
        }
    }
}

private object DespeckleTest {
    fun register() {
        Tests.add("DespeckleTest.strict12pxBoundary", "removeSmallComponents") {
            val retained = (1..4).flatMap { y -> (1..3).map { x -> x to y } }
            val removed = (10..20).map { x -> x to 2 }
            val input = grid(24, 8, retained + removed)
            val snapshot = input.copyPixels()
            pixelsEqual(removeSmallComponents(input), grid(24, 8, retained))
            assertThat(input.copyPixels().contentEquals(snapshot))
        }
        Tests.add("DespeckleTest.eightConnectivity", "removeSmallComponents") {
            val input = grid(4, 4, listOf(1 to 1, 2 to 2))
            pixelsEqual(removeSmallComponents(input, PipelineParams(minComponentAreaPx = 2)), input)
        }
    }
}

private object ZhangSuenTest {
    fun register() {
        Tests.add("ZhangSuenTest.golden20px", "zhangSuen") {
            val input = readBinary("tools/fixtures/synthetic/golden/bar-20-input.png")
            val expected = readBinary("tools/fixtures/synthetic/golden/bar-20-expected.png")
            assertThat(input.width == 20 && input.height == 20 && expected.foregroundCount == 9)
            val snapshot = input.copyPixels()
            val result = zhangSuen(input)
            pixelsEqual(result.grid, expected)
            assertThat(result.iterations > 0 && input.copyPixels().contentEquals(snapshot))
        }
        Tests.add("ZhangSuenTest.fixedPointAndBorder", "zhangSuen") {
            val borderLine = grid(5, 1, (0..4).map { it to 0 })
            pixelsEqual(zhangSuen(borderLine).grid, borderLine)
            val thick = readBinary("tools/fixtures/synthetic/golden/bar-20-input.png")
            val once = zhangSuen(thick).grid
            pixelsEqual(zhangSuen(once).grid, once)
        }
        Tests.add("ZhangSuenTest.preserveLoop", "zhangSuen, skeletonToPolylines") {
            val ring =
                (3..16).flatMap { y ->
                    (3..16).mapNotNull { x ->
                        if (x in 7..12 && y in 7..12) null else x to y
                    }
                }
            val skeleton = zhangSuen(grid(20, 20, ring))
            assertThat(skeleton.grid.foregroundCount > 0 && !skeleton.grid[10, 10])
            assertThat(skeletonToPolylines(skeleton).graphCycleRank >= 1)
        }
    }
}

private object SkeletonToPolylinesTest {
    fun register() {
        Tests.add("SkeletonToPolylinesTest.zheAtLeast3Open", "vectorize, skeletonToPolylines") {
            val result = vectorize(readArgb("tools/fixtures/synthetic/m0/1046.png"))
            val strokes = result.strokes
            assertThat(strokes.polylines.size >= 3)
            assertThat(strokes.polylines.none { it.isClosed })
            assertThat(strokes.rawPolylines.sumOf { it.points.size - 1 } == strokes.graphEdges)
        }
        Tests.add("SkeletonToPolylinesTest.cornerAndDiagonal", "skeletonToPolylines") {
            val corner = extraction(grid(3, 3, listOf(0 to 0, 1 to 0, 1 to 1)))
            assertThat(corner.graphEdges == 2 && corner.graphCycleRank == 0 && corner.suppressedCornerDiagonals == 1)
            val diagonal = extraction(grid(3, 3, listOf(0 to 0, 1 to 1, 2 to 2)))
            assertThat(diagonal.graphEdges == 2 && diagonal.polylines.size == 1 && diagonal.graphCycleRank == 0)
        }
        Tests.add("SkeletonToPolylinesTest.cycleEdgeCoverage", "skeletonToPolylines") {
            val cycle = listOf(3 to 1, 4 to 2, 5 to 3, 4 to 4, 3 to 5, 2 to 4, 1 to 3, 2 to 2)
            val result = extraction(grid(7, 7, cycle))
            assertThat(result.graphCycleRank == 1 && result.splitClosedChains == 1)
            assertThat(result.polylines.size == 2 && result.polylines.none { it.isClosed })
            val centers = cycle.map { (x, y) -> Vec2(x + 0.5, y + 0.5) }
            val expected = (centers + centers.first()).zipWithNext().map { (a, b) -> edge(a, b) }.toSet()
            val actual = result.rawPolylines.flatMap { it.points.zipWithNext().map { (a, b) -> edge(a, b) } }
            assertThat(actual.size == 8 && actual.toSet().size == actual.size && actual.toSet() == expected)
        }
        Tests.add("SkeletonToPolylinesTest.pruneStrictlyBelow2px", "skeletonToPolylines") {
            val horizontal = (0..6).map { it to 3 }
            val short = extraction(grid(7, 7, horizontal + listOf(3 to 4)))
            assertThat(short.prunedEdges == 1 && short.graphEdges == 6 && short.polylines.size == 1)
            val exact = extraction(grid(7, 7, horizontal + listOf(3 to 4, 3 to 5)))
            assertThat(exact.prunedEdges == 0 && exact.graphEdges == 8 && exact.polylines.size == 3)
        }
        Tests.add("SkeletonToPolylinesTest.dotAndEmpty", "skeletonToPolylines, strokesToSvg, requireStrokeOnlySvg") {
            val dot = extraction(grid(3, 3, listOf(1 to 1)))
            assertThat(
                dot.polylines
                    .single()
                    .points.size == 1 &&
                    dot.graphEdges == 0 &&
                    dot.graphComponents == 1,
            )
            assertThat(requireStrokeOnlySvg(strokesToSvg(dot.polylines, 3, 3)) == 1)
            val empty = extraction(grid(3, 3, emptyList()))
            assertThat(empty.polylines.isEmpty() && empty.graphCycleRank == 0 && empty.graphComponents == 0)
        }
    }
}

private object RdpTest {
    fun register() {
        Tests.add("RdpTest.handSpecifiedSimplification", "simplifyRdp") {
            val path = Polyline(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.1), Vec2(2.0, 0.0), Vec2(3.0, 2.0), Vec2(4.0, 0.0)))
            val expected = listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0), Vec2(3.0, 2.0), Vec2(4.0, 0.0))
            assertThat(simplifyRdp(path, 0.2).points == expected)
            assertThat(simplifyRdp(path, 10.0).points == listOf(path.points.first(), path.points.last()))
        }
        Tests.add("RdpTest.zeroEpsilonAndShortPaths", "simplifyRdp") {
            val collinear = Polyline(listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(2.0, 0.0)))
            assertThat(simplifyRdp(collinear, 0.0).points == listOf(Vec2(0.0, 0.0), Vec2(2.0, 0.0)))
            val dot = Polyline(listOf(Vec2(1.0, 1.0)))
            assertThat(simplifyRdp(dot, 0.6) == dot)
            val line = Polyline(listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0)))
            assertThat(simplifyRdp(line, 0.6) == line)
        }
    }
}

private object SvgNoFillTest {
    fun register() {
        Tests.add("SvgNoFillTest.fiveActualExports", "vectorize, strokesToSvg, requireStrokeOnlySvg") {
            for (code in listOf(1046, 1041, 1074, 1064, 1092)) {
                val input = readArgb("tools/fixtures/synthetic/m0/$code.png")
                val result = vectorize(input)
                val svg = strokesToSvg(result.strokes.polylines, input.width, input.height)
                assertThat(requireStrokeOnlySvg(svg) == result.strokes.polylines.size)
                assertThat(result.strokes.polylines.isNotEmpty() && result.strokes.polylines.none { it.isClosed })
            }
        }
        Tests.add("SvgNoFillTest.rejectCorruptedExport", "strokesToSvg, requireStrokeOnlySvg") {
            val svg = strokesToSvg(listOf(Polyline(listOf(Vec2(1.0, 1.0), Vec2(2.0, 2.0)))), 20, 20)
            val corrupt =
                listOf(
                    svg.replaceFirst("fill=\"none\"", "fill=\"black\""),
                    svg.replace("fill=\"none\" stroke", "fill=\"black\" stroke"),
                    svg.replace("stroke=\"black\"", "stroke=\"none\""),
                    svg.replace("<path d=", "<path style=\"fill:black\" d="),
                    svg.replace("</svg>", "<circle cx=\"5\" cy=\"5\" r=\"2\" fill=\"black\"/></svg>"),
                    svg.replace(" L 2.0000 2.0000", " L 2.0000 2.0000 Z"),
                    svg.replace("<svg ", "<!DOCTYPE svg [<!ENTITY x SYSTEM \"file:///not-a-real-file\">]><svg "),
                )
            for (broken in corrupt) expectRejected { requireStrokeOnlySvg(broken) }
        }
        Tests.add("SvgNoFillTest.rejectClosureAndUseDecimalPoint", "strokesToSvg, requireStrokeOnlySvg") {
            val closed = Polyline(listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 0.0)))
            expectRejected { strokesToSvg(listOf(closed), 20, 20) }
            val previous = Locale.getDefault()
            try {
                Locale.setDefault(Locale.GERMANY)
                val open = Polyline(listOf(Vec2(1.5, 2.5), Vec2(3.5, 4.5)))
                val svg = strokesToSvg(listOf(open), 20, 20)
                assertThat("1.5000 2.5000" in svg && requireStrokeOnlySvg(svg) == 1)
            } finally {
                Locale.setDefault(previous)
            }
        }
    }
}

private object PipelineTest {
    fun register() {
        Tests.add("PipelineTest.deterministicAndImmutable", "vectorize") {
            val source = readArgb("tools/fixtures/synthetic/m0/1046.png")
            val callerPixels = source.copyPixels()
            val image = ArgbImage(source.width, source.height, callerPixels)
            val snapshot = image.copyPixels()
            callerPixels.fill(-1)
            val first = vectorize(image)
            image.copyPixels().fill(-1)
            val second = vectorize(image)
            assertThat(image.copyPixels().contentEquals(snapshot))
            pixelsEqual(first.thresholded, second.thresholded)
            pixelsEqual(first.cleaned, second.cleaned)
            pixelsEqual(first.skeleton.grid, second.skeleton.grid)
            assertThat(first.strokes == second.strokes)
            for (index in first.cleaned.copyPixels().indices) {
                assertThat(!first.skeleton.grid[index] || first.cleaned[index])
                assertThat(!first.cleaned[index] || first.thresholded[index])
            }
        }
        Tests.add("PipelineTest.blankImage", "vectorize, strokesToSvg, requireStrokeOnlySvg") {
            val image = ArgbImage(30, 30, IntArray(900) { -1 })
            val result = vectorize(image)
            assertThat(result.cleaned.foregroundCount == 0 && result.skeleton.grid.foregroundCount == 0)
            assertThat(result.strokes.polylines.isEmpty())
            assertThat(requireStrokeOnlySvg(strokesToSvg(result.strokes.polylines, 30, 30)) == 0)
        }
    }
}

// Test adapters only decode controlled PNGs and construct hand-specified inputs/answers.
// None implements a threshold, thinning, tracing, simplification or export algorithm.
private fun readArgb(path: String): ArgbImage {
    val bitmap = ImageIO.read(File(path)) ?: error("Missing/invalid test PNG: $path")
    return ArgbImage(bitmap.width, bitmap.height, bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width))
}

private fun readBinary(path: String): BinaryGrid {
    val image = readArgb(path)
    return BinaryGrid(
        image.width,
        image.height,
        BooleanArray(image.width * image.height) { index ->
            (image[index] and 0xffffff) == 0
        },
    )
}

private fun grid(
    width: Int,
    height: Int,
    foreground: List<Pair<Int, Int>>,
): BinaryGrid {
    val pixels = BooleanArray(width * height)
    for ((x, y) in foreground) pixels[y * width + x] = true
    return BinaryGrid(width, height, pixels)
}

private fun extraction(grid: BinaryGrid) = skeletonToPolylines(SkeletonPixels(grid, 0))

private fun edge(
    first: Vec2,
    second: Vec2,
): Pair<Vec2, Vec2> = if (first.x < second.x || (first.x == second.x && first.y < second.y)) first to second else second to first

private fun pixelsEqual(
    actual: BinaryGrid,
    expected: BinaryGrid,
) {
    assertThat(actual.width == expected.width && actual.height == expected.height)
    for (y in 0 until actual.height) {
        for (x in 0 until actual.width) {
            assertThat(actual[x, y] == expected[x, y], "Pixel mismatch at ($x, $y)")
        }
    }
}

private fun near(
    actual: Double,
    expected: Double,
) = assertThat(abs(actual - expected) < 1e-9, "$actual != $expected")

private fun assertThat(
    condition: Boolean,
    message: String = "Assertion failed",
) {
    if (!condition) throw AssertionError(message)
}

private fun expectRejected(body: () -> Unit) {
    try {
        body()
    } catch (_: Exception) {
        return
    }
    throw AssertionError("Production function accepted corrupted/invalid input")
}
