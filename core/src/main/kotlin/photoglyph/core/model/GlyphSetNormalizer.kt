package photoglyph.core.model

import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2

/** One manually identified glyph whose paths are still in raster coordinates (Y down). */
data class RasterGlyphInput(
    val char: String,
    val paths: List<Polyline>,
    val originX: Double,
    val baselineY: Double,
    val advancePx: Double,
    val sourceBitmapPng: ByteArray? = null,
) {
    init {
        require(char.isNotBlank())
        require(paths.isNotEmpty() && paths.none { it.isClosed })
        require(originX.isFinite() && baselineY.isFinite())
        require(advancePx.isFinite() && advancePx > 0.0)
    }
}

/** Reference heights measured from the raster baseline, not inferred by this function. */
data class RasterFontMetrics(
    val xHeightPx: Double,
    val capHeightPx: Double,
) {
    init {
        require(xHeightPx.isFinite() && xHeightPx > 0.0)
        require(capHeightPx.isFinite() && capHeightPx > xHeightPx)
    }
}

/**
 * Maps reviewed raster glyph paths into a shared Y-up font coordinate system.
 * The caller supplies each glyph's baseline and the set's reference heights;
 * this function does not guess page layout, labels, or handwriting metrics.
 */
fun normalizeGlyphSet(
    id: String,
    name: String,
    glyphs: List<RasterGlyphInput>,
    rasterMetrics: RasterFontMetrics,
    defaultStrokeWidthMm: Double,
    createdAt: String,
    targetCapHeightUnits: Double,
    unitsPerEm: Int = GlyphSet.DEFAULT_UNITS_PER_EM,
): GlyphSet {
    require(glyphs.isNotEmpty())
    require(glyphs.map { it.char }.toSet().size == glyphs.size) { "Glyph labels must be unique" }
    require(targetCapHeightUnits.isFinite() && targetCapHeightUnits > 0.0 && targetCapHeightUnits < unitsPerEm)
    require(defaultStrokeWidthMm.isFinite() && defaultStrokeWidthMm > 0.0)

    val scale = targetCapHeightUnits / rasterMetrics.capHeightPx
    val normalizedGlyphs =
        glyphs.associateTo(linkedMapOf()) { input ->
            val normalizedStrokes =
                input.paths.mapIndexed { order, path ->
                    val normalizedPath =
                        Polyline(
                            path.points.map { point ->
                                Vec2(
                                    x = (point.x - input.originX) * scale,
                                    y = (input.baselineY - point.y) * scale,
                                )
                            },
                        )
                    Stroke(normalizedPath, order, defaultStrokeWidthMm)
                }
            input.char to
                Glyph(
                    char = input.char,
                    strokes = normalizedStrokes,
                    advance = input.advancePx * scale,
                    baselineY = NORMALIZED_BASELINE_Y,
                    sourceBitmapPng = input.sourceBitmapPng,
                )
        }

    return GlyphSet(
        id = id,
        name = name,
        unitsPerEm = unitsPerEm,
        baselineY = NORMALIZED_BASELINE_Y,
        xHeight = rasterMetrics.xHeightPx * scale,
        capHeight = targetCapHeightUnits,
        defaultStrokeWidthMm = defaultStrokeWidthMm,
        glyphs = normalizedGlyphs,
        createdAt = createdAt,
    )
}

private const val NORMALIZED_BASELINE_Y = 0.0
