package photoglyph.app

import photoglyph.core.model.GlyphSet
import photoglyph.core.model.GlyphSetJson
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest
import java.util.UUID

/** File-backed local persistence for independent partial glyph sets. */
internal class LocalGlyphSetStore(
    private val directory: File,
) {
    private val lock = Any()

    /** Stores one complete JSON snapshot without merging or filling any glyphs. */
    fun save(glyphSet: GlyphSet) {
        synchronized(lock) {
            ensureDirectory()
            val destination = fileForId(glyphSet.id)
            val temporary = File(directory, ".${destination.name}.${UUID.randomUUID()}.tmp")
            try {
                val bytes = GlyphSetJson.encode(glyphSet).toByteArray(StandardCharsets.UTF_8)
                FileOutputStream(temporary).use { output ->
                    output.write(bytes)
                    output.fd.sync()
                }
                moveIntoPlace(temporary, destination)
            } finally {
                temporary.delete()
            }
        }
    }

    /** Returns null only when the requested set does not exist. */
    fun load(id: String): GlyphSet? = synchronized(lock) {
        require(id.isNotBlank())
        val file = fileForId(id)
        if (!file.exists()) return@synchronized null
        if (!file.isFile) throw IOException("Local glyph set key is not a file")
        readSet(file, id)
    }

    /** Lists every stored set independently, in a stable name/id order. */
    fun list(): List<GlyphSet> = synchronized(lock) {
        val files = dataFiles()
        files
            .map { file -> readSet(file, expectedId = null) }
            .sortedWith(compareBy<GlyphSet, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
    }

    /** Deletes a set by identity; returns false if it was not present. */
    fun delete(id: String): Boolean = synchronized(lock) {
        require(id.isNotBlank())
        val file = fileForId(id)
        if (!file.exists()) return@synchronized false
        if (!file.isFile || !file.delete()) throw IOException("Could not delete local glyph set")
        true
    }

    private fun dataFiles(): List<File> {
        if (!directory.exists()) return emptyList()
        if (!directory.isDirectory) throw IOException("Glyph set storage path is not a directory")
        return (directory.listFiles() ?: throw IOException("Could not list local glyph sets"))
            .filter { it.isFile && STORAGE_FILE_PATTERN.matches(it.name) }
    }

    private fun ensureDirectory() {
        if (directory.isDirectory) return
        if (directory.exists() || !directory.mkdirs()) throw IOException("Could not create local glyph set storage")
    }

    private fun fileForId(id: String): File {
        require(id.isNotBlank())
        val digest = MessageDigest.getInstance("SHA-256").digest(id.toByteArray(StandardCharsets.UTF_8))
        val hash = digest.joinToString(separator = "") { byte ->
            (byte.toInt() and BYTE_MASK).toString(HEX_RADIX).padStart(HEX_WIDTH, '0')
        }
        return File(directory, "set-$hash.json")
    }

    private fun readSet(
        file: File,
        expectedId: String?,
    ): GlyphSet {
        val glyphSet =
            try {
                GlyphSetJson.decode(file.readText(StandardCharsets.UTF_8))
            } catch (exception: IllegalArgumentException) {
                throw IOException("Invalid local glyph set data", exception)
            }
        if (expectedId != null && glyphSet.id != expectedId) {
            throw IOException("Local glyph set identity does not match its requested key")
        }
        if (fileForId(glyphSet.id).name != file.name) {
            throw IOException("Local glyph set identity does not match its storage key")
        }
        return glyphSet
    }

    private fun moveIntoPlace(
        temporary: File,
        destination: File,
    ) {
        try {
            Files.move(temporary.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), destination.toPath(), REPLACE_EXISTING)
        }
    }

    private companion object {
        const val BYTE_MASK = 0xff
        const val HASH_WIDTH = 64
        const val HEX_RADIX = 16
        const val HEX_WIDTH = 2
        val STORAGE_FILE_PATTERN = Regex("set-[0-9a-f]{$HASH_WIDTH}\\.json")
    }
}
