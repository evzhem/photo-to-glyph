package photoglyph.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import photoglyph.core.geom.Polyline
import photoglyph.core.geom.Vec2
import photoglyph.core.model.Glyph
import photoglyph.core.model.GlyphSet
import photoglyph.core.model.Stroke
import java.io.File
import java.io.IOException
import java.nio.file.Files

class LocalGlyphSetStoreTest {
    @Test
    fun storesIndependentPartialSetsAcrossStoreInstances() = withTemporaryDirectory { directory ->
        val first = glyphSet(id = "first-set", name = "First", character = "А")
        val second = glyphSet(id = "second-set", name = "Second", character = "ё")
        LocalGlyphSetStore(directory).apply {
            save(first)
            save(second)
        }

        val reopened = LocalGlyphSetStore(directory)
        assertEquals(first, reopened.load(first.id))
        assertEquals(second, reopened.load(second.id))
        assertEquals(listOf("First", "Second"), reopened.list().map { it.name })
        assertEquals(setOf("А"), reopened.load(first.id)?.glyphs?.keys)
        assertEquals(setOf("ё"), reopened.load(second.id)?.glyphs?.keys)
    }

    @Test
    fun replacesAndDeletesOnlyTheRequestedSet() = withTemporaryDirectory { directory ->
        val store = LocalGlyphSetStore(directory)
        val firstVersion = glyphSet(id = "shared-id", name = "Before", character = "А")
        val replacement = glyphSet(id = "shared-id", name = "After", character = "Б")
        val independent = glyphSet(id = "other-id", name = "Other", character = "в")
        store.save(firstVersion)
        store.save(independent)
        store.save(replacement)

        assertEquals(replacement, store.load("shared-id"))
        assertEquals(independent, store.load("other-id"))
        assertEquals(2, store.list().size)
        assertTrue(store.delete("shared-id"))
        assertNull(store.load("shared-id"))
        assertEquals(listOf(independent), store.list())
        assertFalse(store.delete("shared-id"))
    }

    @Test
    fun hashesIdsBeforeUsingThemAsFileNames() = withTemporaryDirectory { parent ->
        val storage = File(parent, "sets")
        val id = "../outside"
        val store = LocalGlyphSetStore(storage)
        val set = glyphSet(id = id, name = "Safe", character = "Ж")
        store.save(set)

        val files = checkNotNull(storage.listFiles()).filter { it.isFile }
        assertEquals(1, files.size)
        assertTrue(files.single().name.matches(Regex("set-[0-9a-f]{64}\\.json")))
        assertFalse(File(parent, "outside.json").exists())
        assertEquals(set, store.load(id))
    }

    @Test
    fun malformedStoredJsonIsReportedInsteadOfSilentlyDropped() = withTemporaryDirectory { directory ->
        val store = LocalGlyphSetStore(directory)
        store.save(glyphSet(id = "broken", name = "Broken", character = "А"))
        checkNotNull(directory.listFiles()).single { it.name.endsWith(".json") }.writeText("{")

        val failure = runCatching { store.list() }.exceptionOrNull()
        assertTrue(failure is IOException)
    }

    private fun glyphSet(
        id: String,
        name: String,
        character: String,
    ): GlyphSet {
        val glyph =
            Glyph(
                char = character,
                strokes = listOf(Stroke(Polyline(listOf(Vec2(10.0, 20.0), Vec2(30.0, 40.0))), 0, 0.3)),
                advance = 620.0,
                baselineY = 0.0,
            )
        return GlyphSet(
            id = id,
            name = name,
            baselineY = 0.0,
            xHeight = 500.0,
            capHeight = 700.0,
            defaultStrokeWidthMm = 0.3,
            glyphs = mapOf(character to glyph),
            createdAt = CREATED_AT,
        )
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("photoglyph-local-sets").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private companion object {
        const val CREATED_AT = "2026-10-16T12:00:00Z"
    }
}
