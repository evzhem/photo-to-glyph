package photoglyph.cli

import photoglyph.cli.sandbox.runSandbox
import photoglyph.core.PipelineParams
import photoglyph.core.VectorizationResult
import photoglyph.core.export.requireStrokeOnlySvg
import photoglyph.core.export.strokesToSvg
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.BinaryGrid
import photoglyph.core.img.GrayImage
import photoglyph.core.vectorize
import java.awt.image.BufferedImage
import java.io.File
import java.util.Locale
import javax.imageio.ImageIO
import kotlin.math.roundToInt

fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "sandbox" -> {
            require(args.size == 3) { "Usage: sandbox <project-root> <port>" }
            runSandbox(File(args[1]), args[2].toInt())
        }
        "smoke" -> {
            require(args.size == 3) { "Usage: smoke <fixture-directory> <output-directory>" }
            smoke(File(args[1]), File(args[2]))
        }
        "vectorize" -> {
            require(args.size == 3) { "Usage: vectorize <cropped-input.png> <output.svg>" }
            val processed = process(File(args[1]), PipelineParams())
            val output = File(args[2])
            output.absoluteFile.parentFile.mkdirs()
            output.writeText(processed.svg)
            println(
                "strokes=${processed.result.strokes.polylines.size} " +
                    "closed=${processed.result.strokes.polylines.count { it.isClosed }} " +
                    "graphCycles=${processed.result.strokes.graphCycleRank} " +
                    "elapsedMs=${decimal(processed.elapsedMs)} output=$output",
            )
        }
        else -> error("Usage: smoke <fixtures> <output> | vectorize <cropped-input.png> <output.svg> | sandbox <root> <port>")
    }
}

private data class Processed(
    val result: VectorizationResult,
    val svg: String,
    val elapsedMs: Double,
)

/** I/O/timing adapter. The timed path includes PNG decode, core, SVG serialization/validation, not disk writes. */
private fun process(
    file: File,
    params: PipelineParams,
): Processed {
    val start = System.nanoTime()
    val image = readImage(file)
    val result = vectorize(image, params)
    val svg = strokesToSvg(result.strokes.polylines, image.width, image.height, params)
    check(requireStrokeOnlySvg(svg) == result.strokes.polylines.size)
    val elapsedMs = (System.nanoTime() - start) / NANOS_PER_MILLISECOND
    return Processed(result, svg, elapsedMs)
}

private fun smoke(
    fixtures: File,
    output: File,
) {
    val params = PipelineParams()
    check(output.isDirectory || output.mkdirs()) { "Cannot create output directory" }
    val csv =
        StringBuilder(
            "char,codepoint,strokes,closed_polylines,graph_cycle_rank,split_closed_chains,graph_components," +
                "source_pixels,cleaned_pixels,skeleton_pixels,graph_nodes,graph_edges,pruned_edges," +
                "corner_diagonals,rdp_points,raw_points,cold_ms,median_ms,max_ms\n",
        )
    println("INPUT: original synthetic 300x300 PNGs, 6 px strokes; not real handwriting")
    println(
        "TIMING: PNG decode + core + SVG serialization/validation; excludes JVM startup/output writes; " +
            "${params.benchmarkWarmupRuns} warmups + ${params.benchmarkMeasuredRuns} measured runs",
    )
    println("char | strokes | closed | graph cycles | split chains | cold ms | median ms | max ms")
    for (char in M0_CHARACTERS) {
        val codePoint = char.code
        val file = File(fixtures, "$codePoint.png")
        val cold = process(file, params)
        repeat(params.benchmarkWarmupRuns) { process(file, params) }
        val measured = List(params.benchmarkMeasuredRuns) { process(file, params) }
        val sortedTimes = measured.map { it.elapsedMs }.sorted()
        val median = sortedTimes[sortedTimes.size / 2]
        val max = maxOf(cold.elapsedMs, sortedTimes.last())
        val latest = measured.last()
        val result = latest.result
        val strokes = result.strokes
        val closed = strokes.polylines.count { it.isClosed }
        check(strokes.polylines.isNotEmpty()) { "Empty glyph: $char" }
        check(closed == 0) { "Closed output polyline: $char" }
        check(max <= params.processingBudgetMs) { "Processing budget exceeded: $char, $max ms" }
        File(output, "$codePoint.svg").writeText(latest.svg)
        writeGray(File(output, "$codePoint-grayscale.png"), result.grayscale)
        writeBinary(File(output, "$codePoint-threshold.png"), result.thresholded)
        writeBinary(File(output, "$codePoint-binary.png"), result.cleaned)
        writeBinary(File(output, "$codePoint-skeleton.png"), result.skeleton.grid)
        val row =
            listOf(
                char,
                codePoint,
                strokes.polylines.size,
                closed,
                strokes.graphCycleRank,
                strokes.splitClosedChains,
                strokes.graphComponents,
                result.thresholded.foregroundCount,
                result.cleaned.foregroundCount,
                result.skeleton.grid.foregroundCount,
                strokes.graphNodes,
                strokes.graphEdges,
                strokes.prunedEdges,
                strokes.suppressedCornerDiagonals,
                strokes.polylines.sumOf { it.points.size },
                strokes.rawPolylines.sumOf { it.points.size },
                decimal(cold.elapsedMs),
                decimal(median),
                decimal(max),
            )
        csv.appendLine(row.joinToString(","))
        println(
            "$char | ${strokes.polylines.size} | $closed | ${strokes.graphCycleRank} | " +
                "${strokes.splitClosedChains} | ${decimal(cold.elapsedMs)} | ${decimal(median)} | ${decimal(max)}",
        )
    }
    File(output, "metrics.csv").writeText(csv.toString())
    println("PASS smoke: 5/5 nonempty glyphs, zero closed polylines, explicit no-fill strokes, within budget")
    println("Outputs: ${output.path}/<codepoint>.svg, intermediate PNGs, metrics.csv")
}

private fun readImage(file: File): ArgbImage {
    require(file.isFile) { "Missing input: $file" }
    val bitmap = ImageIO.read(file) ?: error("Unsupported image: $file")
    val pixels = bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width)
    return ArgbImage(bitmap.width, bitmap.height, pixels)
}

private fun writeGray(
    file: File,
    image: GrayImage,
) {
    val bitmap = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val level = image[x, y].roundToInt().coerceIn(0, CHANNEL_MAX)
            bitmap.setRGB(x, y, (level shl RED_SHIFT) or (level shl GREEN_SHIFT) or level)
        }
    }
    check(ImageIO.write(bitmap, "png", file))
}

private fun writeBinary(
    file: File,
    image: BinaryGrid,
) {
    val bitmap = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) bitmap.setRGB(x, y, if (image[x, y]) BLACK_RGB else WHITE_RGB)
    }
    check(ImageIO.write(bitmap, "png", file))
}

private fun decimal(value: Double): String = String.format(Locale.ROOT, "%.3f", value)

private const val NANOS_PER_MILLISECOND = 1_000_000.0
private const val CHANNEL_MAX = 255
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLACK_RGB = 0x000000
private const val WHITE_RGB = 0xffffff
private val M0_CHARACTERS = listOf('Ж', 'Б', 'в', 'Ш', 'ф')
