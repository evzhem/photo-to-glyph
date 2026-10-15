package photoglyph.core.model

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Stable local interchange for the partial GlyphSet model; not the KDraw .gfont format. */
object GlyphSetJson {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            prettyPrint = false
        }

    fun encode(glyphSet: GlyphSet): String = json.encodeToString(glyphSet)

    fun decode(source: String): GlyphSet = json.decodeFromString(source)
}
