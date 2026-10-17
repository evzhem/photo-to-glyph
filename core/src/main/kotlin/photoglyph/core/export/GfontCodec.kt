package photoglyph.core.export

import photoglyph.core.model.GlyphSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** One point in the documented .gfont record; flag 0 starts a stroke and 1 continues it. */
data class GfontPoint(
    val x: Float,
    val y: Float,
    val flag: Int,
)

/** Geometry decoded from one decimal-Unicode ZIP entry. */
data class GfontGlyphRecord(
    val codePoint: Int,
    val points: List<GfontPoint>,
)

/** Parsed glyph entries only; unknown service records are rejected, not silently discarded. */
data class GfontArchive(
    val glyphs: Map<Int, GfontGlyphRecord>,
)

/**
 * Independent codec for the currently documented .gfont glyph records.
 * Coordinates are written in model units without scale conversion and Y is flipped
 * from the PhotoGlyph Y-up model to the format's Y-down coordinates. KDraw scale,
 * optional service entries, and companion-text compatibility remain unverified.
 */
object GfontCodec {
    fun encode(glyphSet: GlyphSet): ByteArray {
        require(glyphSet.glyphs.isNotEmpty()) { "Cannot export an empty glyph set" }
        require(glyphSet.glyphs.size <= MAX_GLYPHS)
        val output = ByteArrayOutputStream()
        var uncompressedBytes = 0L
        ZipOutputStream(output).use { archive ->
            glyphSet.glyphs.entries
                .sortedBy { entry -> codePointOf(entry.key) }
                .forEach { (key, glyph) ->
                    val codePoint = codePointOf(key)
                    val points = glyphPoints(glyph)
                    require(points.size <= MAX_POINTS_PER_GLYPH)
                    val record = encodeGlyphRecord(codePoint, points)
                    uncompressedBytes += record.size
                    require(uncompressedBytes <= MAX_UNCOMPRESSED_BYTES) { "Uncompressed .gfont is too large" }
                    archive.putNextEntry(ZipEntry(codePoint.toString()).apply { time = ZIP_ENTRY_TIME_MILLIS })
                    archive.write(record)
                    archive.closeEntry()
                }
        }
        val bytes = output.toByteArray()
        require(bytes.size <= MAX_ARCHIVE_BYTES) { "Encoded .gfont archive is too large" }
        return bytes
    }

    /** Validates ZIP entry names, binary lengths, finite coordinates, and stroke flags. */
    fun decode(bytes: ByteArray): GfontArchive {
        require(bytes.isNotEmpty() && bytes.size <= MAX_ARCHIVE_BYTES) { "Invalid .gfont archive size" }
        val glyphs = linkedMapOf<Int, GfontGlyphRecord>()
        var uncompressedBytes = 0L
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { archive ->
                while (true) {
                    val entry = archive.nextEntry ?: break
                    require(!entry.isDirectory) { "Directory entries are not glyph records" }
                    val codePoint = parseEntryCodePoint(entry.name)
                    require(codePoint !in glyphs) { "Duplicate glyph entry" }
                    val recordBytes = readEntryBounded(archive)
                    uncompressedBytes += recordBytes.size
                    require(uncompressedBytes <= MAX_UNCOMPRESSED_BYTES) { "Uncompressed .gfont is too large" }
                    glyphs[codePoint] = decodeGlyphRecord(codePoint, recordBytes)
                    archive.closeEntry()
                    require(glyphs.size <= MAX_GLYPHS) { "Too many glyph entries" }
                }
            }
        } catch (exception: IOException) {
            throw IllegalArgumentException("Invalid .gfont ZIP archive", exception)
        }
        require(glyphs.isNotEmpty()) { "The archive contains no glyph entries" }
        return GfontArchive(glyphs)
    }

    private fun glyphPoints(glyph: photoglyph.core.model.Glyph): List<GfontPoint> {
        val orderedStrokes = glyph.strokes.sortedBy { it.order }
        val pointCount = orderedStrokes.sumOf { it.path.points.size.toLong() }
        require(pointCount in 1..MAX_POINTS_PER_GLYPH.toLong()) { "Invalid glyph point count" }
        return buildList(pointCount.toInt()) {
            orderedStrokes.forEach { stroke ->
                stroke.path.points.forEachIndexed { index, point ->
                    val x = point.x.toFloat()
                    val y = (glyph.baselineY - point.y).toFloat()
                    require(x.isFinite() && y.isFinite()) { "Coordinates exceed .gfont float range" }
                    add(GfontPoint(x, y, if (index == 0) STROKE_START_FLAG else STROKE_CONTINUE_FLAG))
                }
            }
        }
    }

    private fun encodeGlyphRecord(
        codePoint: Int,
        points: List<GfontPoint>,
    ): ByteArray {
        val floatCount = Math.multiplyExact(points.size, COORDINATES_PER_POINT)
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { record ->
            record.writeShort(codePoint)
            record.writeInt(floatCount)
            points.forEach { point ->
                record.writeFloat(point.x)
                record.writeFloat(point.y)
            }
            record.writeInt(points.size)
            points.forEach { point -> record.writeByte(point.flag) }
        }
        return output.toByteArray()
    }

    private fun decodeGlyphRecord(
        expectedCodePoint: Int,
        bytes: ByteArray,
    ): GfontGlyphRecord {
        require(bytes.size >= MINIMUM_RECORD_BYTES) { "Truncated .gfont glyph record" }
        val record = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
        val codePoint = record.short.toInt() and UINT16_MASK
        require(codePoint == expectedCodePoint) { "ZIP entry and binary character code differ" }
        val floatCount = record.int.toLong() and UINT32_MASK
        require(floatCount > 0 && floatCount % COORDINATES_PER_POINT == 0L) { "Invalid coordinate count" }
        val pointCount = floatCount / COORDINATES_PER_POINT
        require(pointCount <= MAX_POINTS_PER_GLYPH) { "Too many points in glyph" }
        val coordinateBytes = pointCount * COORDINATES_PER_POINT * FLOAT_BYTES
        require(record.remaining().toLong() >= coordinateBytes + UINT32_BYTES) { "Truncated .gfont coordinates" }
        val coordinates = ArrayList<Pair<Float, Float>>(pointCount.toInt())
        repeat(pointCount.toInt()) {
            val x = record.float
            val y = record.float
            require(x.isFinite() && y.isFinite()) { "Non-finite .gfont coordinate" }
            coordinates.add(x to y)
        }
        val flagCount = record.int.toLong() and UINT32_MASK
        require(flagCount == pointCount) { "Flag count must match point count" }
        require(record.remaining().toLong() == flagCount) { "Invalid .gfont flag bytes" }
        val points =
            coordinates.mapIndexed { index, (x, y) ->
                val flag = record.get().toInt() and UINT8_MASK
                require(flag == STROKE_START_FLAG || flag == STROKE_CONTINUE_FLAG) { "Invalid stroke flag" }
                require(index != 0 || flag == STROKE_START_FLAG) { "The first point must start a stroke" }
                GfontPoint(x, y, flag)
            }
        require(!record.hasRemaining()) { "Trailing bytes in .gfont glyph record" }
        return GfontGlyphRecord(codePoint, points)
    }

    private fun readEntryBounded(archive: ZipInputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(READ_BUFFER_BYTES)
        while (true) {
            val count = archive.read(buffer)
            if (count < 0) break
            require(output.size().toLong() + count <= MAX_ENTRY_BYTES) { "Glyph entry is too large" }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun parseEntryCodePoint(name: String): Int {
        require(name.isNotEmpty() && name.all(Char::isDigit)) { "Unsupported .gfont ZIP entry: $name" }
        val parsed = name.toIntOrNull() ?: throw IllegalArgumentException("Invalid Unicode ZIP entry")
        require(parsed in 1..MAX_BMP_CODE_POINT && parsed.toString() == name) { "Invalid Unicode ZIP entry" }
        require(!Character.isSurrogate(parsed.toChar())) { "Surrogate code points are unsupported" }
        return parsed
    }

    private fun codePointOf(character: String): Int {
        val values = character.codePoints().toArray()
        require(values.size == 1) { "Each .gfont entry must represent one Unicode code point" }
        val codePoint = values.single()
        require(codePoint in 1..MAX_BMP_CODE_POINT && !Character.isSurrogate(codePoint.toChar())) {
            "The documented .gfont record supports only non-surrogate BMP code points"
        }
        return codePoint
    }

    private const val COORDINATES_PER_POINT = 2
    private const val FLOAT_BYTES = 4
    private const val UINT32_BYTES = 4
    private const val MINIMUM_RECORD_BYTES = 2 + UINT32_BYTES + COORDINATES_PER_POINT * FLOAT_BYTES + UINT32_BYTES + 1
    private const val UINT16_MASK = 0xffff
    private const val UINT32_MASK = 0xffff_ffffL
    private const val UINT8_MASK = 0xff
    private const val MAX_BMP_CODE_POINT = 0xffff
    private const val MAX_GLYPHS = 0xffff
    private const val MAX_POINTS_PER_GLYPH = 1_000_000
    private const val MAX_ENTRY_BYTES = 9_000_010
    private const val MAX_ARCHIVE_BYTES = 64_000_000
    private const val MAX_UNCOMPRESSED_BYTES = 64_000_000L
    private const val READ_BUFFER_BYTES = 8192
    private const val STROKE_START_FLAG = 0
    private const val STROKE_CONTINUE_FLAG = 1
    private const val ZIP_ENTRY_TIME_MILLIS = 0L
}
