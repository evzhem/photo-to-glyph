package photoglyph.core.model

import kotlinx.serialization.Serializable
import photoglyph.core.geom.Polyline

/** One pen-down path; handwriting glyph paths remain open, stroke-only polylines. */
@Serializable
data class Stroke(
    @kotlinx.serialization.Serializable(with = PolylineSerializer::class)
    val path: Polyline,
    val order: Int,
    val widthMm: Double,
) {
    init {
        require(order >= 0)
        require(widthMm.isFinite() && widthMm > 0.0)
        require(!path.isClosed) { "Glyph strokes must be open polylines" }
    }
}

@Serializable
data class Glyph(
    val char: String,
    val strokes: List<Stroke>,
    val advance: Double,
    val baselineY: Double,
    val sourceBitmapPng: ByteArray? = null,
) {
    init {
        require(isSingleNonWhitespaceCodePoint(char))
        require(strokes.isNotEmpty())
        require(strokes.map { it.order }.toSet().size == strokes.size)
        require(advance.isFinite() && advance > 0.0)
        require(baselineY.isFinite())
    }
}

/** A possibly incomplete, independently owned alphabet; missing glyphs are never synthesized. */
@Serializable
data class GlyphSet(
    val id: String,
    val name: String,
    val unitsPerEm: Int = DEFAULT_UNITS_PER_EM,
    val baselineY: Double,
    val xHeight: Double,
    val capHeight: Double,
    val defaultStrokeWidthMm: Double,
    val glyphs: Map<String, Glyph>,
    val createdAt: String,
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(unitsPerEm > 0)
        require(baselineY.isFinite())
        require(xHeight.isFinite() && xHeight > 0.0)
        require(capHeight.isFinite() && capHeight > xHeight)
        require(defaultStrokeWidthMm.isFinite() && defaultStrokeWidthMm > 0.0)
        require(glyphs.isNotEmpty())
        require(glyphs.all { (key, glyph) -> isSingleNonWhitespaceCodePoint(key) && key == glyph.char })
        require(createdAt.isNotBlank())
    }

    companion object {
        const val DEFAULT_UNITS_PER_EM = 1_000
    }
}

private fun isSingleNonWhitespaceCodePoint(value: String): Boolean =
    value.isNotEmpty() && value.codePointCount(0, value.length) == 1 && !value.first().isWhitespace()
