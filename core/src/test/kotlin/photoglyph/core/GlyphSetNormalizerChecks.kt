package photoglyph.core

import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.model.GlyphSetNormalizationOptions
import photoglyph.core.model.RasterFontMetrics
import photoglyph.core.model.RasterGlyphInput
import photoglyph.core.model.normalizeGlyphSet

/** Normalization checks call the production Y-down to Y-up model conversion. */
object GlyphSetNormalizerChecks {
    fun descendersUseTheSharedBaselineAndReferenceScale() {
        val sources =
            listOf(
                rasterGlyph(
                    char = "g",
                    originX = 5.0,
                    baselineY = 20.0,
                    advancePx = 12.0,
                    points = listOf(Vec2(5.0, 5.0), Vec2(9.0, 20.0), Vec2(11.0, 28.0)),
                ),
                rasterGlyph(
                    char = "p",
                    originX = 2.0,
                    baselineY = 30.0,
                    advancePx = 10.0,
                    points = listOf(Vec2(2.0, 10.0), Vec2(4.0, 30.0), Vec2(6.0, 40.0)),
                ),
                rasterGlyph(
                    char = "y",
                    originX = 1.0,
                    baselineY = 18.0,
                    advancePx = 9.0,
                    points = listOf(Vec2(1.0, 2.0), Vec2(5.0, 18.0), Vec2(7.0, 23.0)),
                ),
            )

        val normalized =
            normalizeGlyphSet(
                glyphs = sources,
                rasterMetrics = RasterFontMetrics(xHeightPx = 14.0, capHeightPx = 20.0),
                options =
                    GlyphSetNormalizationOptions(
                        id = "set-normalized",
                        name = "Reviewed descenders",
                        defaultStrokeWidthMm = 0.35,
                        createdAt = CREATED_AT,
                        targetCapHeightUnits = 700.0,
                    ),
            )

        check(normalized.unitsPerEm == 1_000)
        check(normalized.baselineY == 0.0 && normalized.xHeight == 490.0 && normalized.capHeight == 700.0)
        check(normalized.glyphs.keys == setOf("g", "p", "y"))
        check(normalized.glyphs.values.all { it.baselineY == normalized.baselineY })
        check(normalized.glyphs.getValue("g").advance == 420.0)
        check(
            normalized.glyphs.getValue("g").strokes.single().path.points ==
                listOf(
                    Vec2(0.0, 525.0),
                    Vec2(140.0, 0.0),
                    Vec2(210.0, -280.0),
                ),
        )
        check(normalized.glyphs.getValue("p").strokes.single().path.points.last().y == -350.0)
        check(normalized.glyphs.getValue("y").strokes.single().path.points.last().y == -175.0)
        check(normalized.glyphs.values.all { it.strokes.single().widthMm == 0.35 })
    }

    fun invalidLabelsAndMetricsAreRejected() {
        val glyph =
            rasterGlyph(
                char = "а",
                originX = 0.0,
                baselineY = 10.0,
                advancePx = 5.0,
                points = listOf(Vec2(0.0, 0.0), Vec2(1.0, 10.0)),
            )
        rejects {
            normalizeGlyphSet(
                glyphs = listOf(glyph, glyph),
                rasterMetrics = RasterFontMetrics(5.0, 10.0),
                options = normalizationOptions("duplicate", "Duplicate labels"),
            )
        }
        rejects { RasterFontMetrics(xHeightPx = 10.0, capHeightPx = 10.0) }
        rejects {
            normalizeGlyphSet(
                glyphs = listOf(glyph),
                rasterMetrics = RasterFontMetrics(5.0, 10.0),
                options = normalizationOptions("invalid-scale", "Invalid scale", targetCapHeightUnits = 1_000.0),
            )
        }
    }

    private fun normalizationOptions(
        id: String,
        name: String,
        targetCapHeightUnits: Double = 700.0,
    ): GlyphSetNormalizationOptions =
        GlyphSetNormalizationOptions(
            id = id,
            name = name,
            defaultStrokeWidthMm = 0.3,
            createdAt = CREATED_AT,
            targetCapHeightUnits = targetCapHeightUnits,
        )

    private fun rasterGlyph(
        char: String,
        originX: Double,
        baselineY: Double,
        advancePx: Double,
        points: List<Vec2>,
    ): RasterGlyphInput =
        RasterGlyphInput(
            char = char,
            paths = listOf(Polyline(points)),
            originX = originX,
            baselineY = baselineY,
            advancePx = advancePx,
        )

    private fun rejects(block: () -> Unit) {
        check(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }

    private const val CREATED_AT = "2026-10-16T12:00:00Z"
}
