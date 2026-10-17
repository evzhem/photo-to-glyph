package photoglyph.core

import photoglyph.core.export.GfontCodec
import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.model.Glyph
import photoglyph.core.model.GlyphSet
import photoglyph.core.model.Stroke
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Tests the real ZIP/big-endian codec with independently constructed partial sets. */
object GfontCodecChecks {
    fun partialGlyphSetRoundTripsDocumentedCoordinatesAndFlags() {
        val glyphSet = sampleGlyphSet()
        val encoded = GfontCodec.encode(glyphSet)
        val entries = unzip(encoded)

        check(entries.keys.toList() == listOf("1046", "1103"))
        val zheRecord = entries.getValue("1046")
        val header = ByteBuffer.wrap(zheRecord).order(ByteOrder.BIG_ENDIAN)
        check((header.short.toInt() and UINT16_MASK) == ZHE_CODE_POINT)
        check(header.int == 6)
        check(header.float == 10.25f && header.float == -500.0f)
        check(header.float == 20.5f && header.float == 25.0f)
        check(header.float == 5.0f && header.float == -100.0f)
        check(header.int == 3)
        check((header.get().toInt() and UINT8_MASK) == STROKE_START)
        check((header.get().toInt() and UINT8_MASK) == STROKE_CONTINUE)
        check((header.get().toInt() and UINT8_MASK) == STROKE_START)
        check(!header.hasRemaining())

        val decoded = GfontCodec.decode(encoded)
        check(decoded.glyphs.keys == setOf(ZHE_CODE_POINT, YA_CODE_POINT))
        check(
            decoded.glyphs.getValue(ZHE_CODE_POINT).points ==
                listOf(
                    photoglyph.core.export.GfontPoint(10.25f, -500.0f, STROKE_START),
                    photoglyph.core.export.GfontPoint(20.5f, 25.0f, STROKE_CONTINUE),
                    photoglyph.core.export.GfontPoint(5.0f, -100.0f, STROKE_START),
                ),
        )
        check(
            decoded.glyphs
                .getValue(YA_CODE_POINT)
                .points
                .single()
                .flag == STROKE_START,
        )
        check(GfontCodec.encode(glyphSet).contentEquals(encoded))
    }

    fun uppercaseAndLowercaseRemainDistinctGfontEntries() {
        val source = sampleGlyphSet()
        val uppercase = source.glyphs.getValue("Ж").copy(char = "А")
        val lowercase = source.glyphs.getValue("я").copy(char = "а")
        val caseSet = source.copy(glyphs = linkedMapOf("А" to uppercase, "а" to lowercase))

        val encoded = GfontCodec.encode(caseSet)
        val decoded = GfontCodec.decode(encoded)

        check(unzip(encoded).keys.toSet() == setOf("1040", "1072"))
        check(decoded.glyphs.keys == setOf("А".codePointAt(0), "а".codePointAt(0)))
        check(GfontCodec.encode(caseSet).contentEquals(encoded))
    }

    fun malformedRecordsAndUnsupportedEntriesAreRejected() {
        rejects { GfontCodec.decode(zipOf("65" to glyphRecord(codePoint = 65, floatCount = 1))) }
        rejects {
            GfontCodec.decode(
                zipOf(
                    "65" to
                        glyphRecord(
                            codePoint = 65,
                            floatCount = 2,
                            flagCount = 2,
                            flags = byteArrayOf(STROKE_START.toByte(), STROKE_CONTINUE.toByte()),
                        ),
                ),
            )
        }
        rejects {
            GfontCodec.decode(
                zipOf(
                    "65" to
                        glyphRecord(
                            codePoint = 65,
                            floatCount = 2,
                            flagCount = 1,
                            flags = byteArrayOf(2),
                        ),
                ),
            )
        }
        rejects {
            GfontCodec.decode(
                zipOf(
                    "65" to
                        glyphRecord(
                            codePoint = 65,
                            floatCount = 2,
                            x = Float.NaN,
                            flagCount = 1,
                            flags = byteArrayOf(STROKE_START.toByte()),
                        ),
                ),
            )
        }
        rejects { GfontCodec.decode(zipOf("065" to glyphRecord(codePoint = 65, floatCount = 2, flags = byteArrayOf(0)))) }
        rejects { GfontCodec.decode(zipOf("manifest" to byteArrayOf(1, 2, 3))) }
        rejects { GfontCodec.decode(zipOf("65" to glyphRecord(codePoint = 66, floatCount = 2, flags = byteArrayOf(0)))) }
        rejects { GfontCodec.decode(zipOf("65" to glyphRecord(codePoint = 65, floatCount = 2, flags = byteArrayOf(0), trailingByte = true))) }
    }

    fun supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field() {
        val glyphSet = sampleGlyphSet(character = "😀")
        check(runCatching { GfontCodec.encode(glyphSet) }.exceptionOrNull() is IllegalArgumentException)
    }

    private fun sampleGlyphSet(character: String = "Ж"): GlyphSet {
        val secondCharacter = if (character == "Ж") "я" else "😀"
        val glyphs =
            linkedMapOf(
                character to
                    Glyph(
                        char = character,
                        strokes =
                            listOf(
                                Stroke(Polyline(listOf(Vec2(10.25, 500.0), Vec2(20.5, -25.0))), 0, 0.35),
                                Stroke(Polyline(listOf(Vec2(5.0, 100.0))), 1, 0.35),
                            ),
                        advance = 620.0,
                        baselineY = 0.0,
                    ),
                secondCharacter to
                    Glyph(
                        char = secondCharacter,
                        strokes = listOf(Stroke(Polyline(listOf(Vec2(0.0, 0.0))), 0, 0.35)),
                        advance = 400.0,
                        baselineY = 0.0,
                    ),
            )
        return GlyphSet(
            id = "synthetic-gfont",
            name = "Synthetic partial",
            baselineY = 0.0,
            xHeight = 450.0,
            capHeight = 700.0,
            defaultStrokeWidthMm = 0.35,
            glyphs = glyphs,
            createdAt = "2026-10-17T10:00:00Z",
        )
    }

    private fun glyphRecord(
        codePoint: Int,
        floatCount: Int,
        x: Float = 1.0f,
        y: Float = -2.0f,
        flagCount: Int = 1,
        flags: ByteArray = byteArrayOf(STROKE_START.toByte()),
        trailingByte: Boolean = false,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { record ->
            record.writeShort(codePoint)
            record.writeInt(floatCount)
            if (floatCount >= 2) {
                record.writeFloat(x)
                record.writeFloat(y)
            }
            record.writeInt(flagCount)
            record.write(flags)
            if (trailingByte) record.writeByte(0)
        }
        return output.toByteArray()
    }

    private fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun rejects(block: () -> Unit) {
        check(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }

    private const val ZHE_CODE_POINT = 1_046
    private const val YA_CODE_POINT = 1_103
    private const val UINT16_MASK = 0xffff
    private const val UINT8_MASK = 0xff
    private const val STROKE_START = 0
    private const val STROKE_CONTINUE = 1
}
