package photoglyph.core

import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.model.Glyph
import photoglyph.core.model.GlyphSet
import photoglyph.core.model.GlyphSetJson
import photoglyph.core.model.Stroke
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Actual JSON/model checks for independently stored partial handwriting sets. */
object GlyphSetChecks {
    fun partialSetJsonRoundTripPreservesModelAndPngBytes() {
        val source = sampleSet()
        val encoded = GlyphSetJson.encode(source)
        val decoded = GlyphSetJson.decode(encoded)
        check(decoded.id == source.id && decoded.name == source.name)
        check(decoded.unitsPerEm == source.unitsPerEm)
        check(decoded.baselineY == source.baselineY && decoded.xHeight == source.xHeight && decoded.capHeight == source.capHeight)
        check(decoded.defaultStrokeWidthMm == source.defaultStrokeWidthMm && decoded.createdAt == source.createdAt)
        check(decoded.glyphs.keys == setOf("А", "ё"))
        for (character in source.glyphs.keys) {
            val expected = checkNotNull(source.glyphs[character])
            val actual = checkNotNull(decoded.glyphs[character])
            check(actual.char == expected.char && actual.advance == expected.advance && actual.baselineY == expected.baselineY)
            check(actual.strokes == expected.strokes)
            check(actual.sourceBitmapPng.contentEqualsNullable(expected.sourceBitmapPng))
        }
    }

    fun closedGlyphStrokesAndMismatchedKeysAreRejected() {
        val closed = Polyline(listOf(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 0.0)))
        rejects { Stroke(closed, 0, 0.3) }
        val glyph = sampleSet().glyphs.getValue("А")
        rejects {
            GlyphSet(
                id = "set-2",
                name = "Partial",
                baselineY = 0.0,
                xHeight = 500.0,
                capHeight = 700.0,
                defaultStrokeWidthMm = 0.3,
                glyphs = mapOf("Б" to glyph),
                createdAt = CREATED_AT,
            )
        }
    }

    private fun sampleSet(): GlyphSet {
        val png =
            ByteArrayOutputStream().use { output ->
                val bitmap = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
                try {
                    check(ImageIO.write(bitmap, "png", output))
                } finally {
                    bitmap.flush()
                }
                output.toByteArray()
            }
        val uppercase =
            Glyph(
                char = "А",
                strokes = listOf(Stroke(Polyline(listOf(Vec2(10.0, 20.0), Vec2(30.0, 40.0))), 0, 0.3)),
                advance = 620.0,
                baselineY = 0.0,
                sourceBitmapPng = png,
            )
        val lowercase =
            Glyph(
                char = "ё",
                strokes =
                    listOf(
                        Stroke(Polyline(listOf(Vec2(12.0, 18.0), Vec2(32.0, 35.0))), 0, 0.25),
                        Stroke(Polyline(listOf(Vec2(16.0, 45.0), Vec2(18.0, 46.0))), 1, 0.25),
                    ),
                advance = 570.0,
                baselineY = 0.0,
            )
        return GlyphSet(
            id = "handwriting-set-1",
            name = "Мой неполный набор",
            baselineY = 0.0,
            xHeight = 500.0,
            capHeight = 720.0,
            defaultStrokeWidthMm = 0.3,
            glyphs = linkedMapOf("А" to uppercase, "ё" to lowercase),
            createdAt = CREATED_AT,
        )
    }

    private fun rejects(run: () -> Unit) {
        var rejected = false
        try {
            run()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        check(rejected) { "Invalid glyph data was not rejected" }
    }

    private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean = if (this == null || other == null) {
        this == null && other == null
    } else {
        contentEquals(other)
    }

    private const val CREATED_AT = "2026-10-15T20:00:00Z"
}
