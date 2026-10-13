package photoglyph.cli.sandbox

import com.sun.net.httpserver.HttpExchange
import photoglyph.core.PipelineParams
import photoglyph.core.export.GridSheet
import photoglyph.core.export.cyrillicGridCharacters
import photoglyph.core.export.gridSheetSvg
import photoglyph.core.geom.Vec2
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.PageQuad
import photoglyph.core.img.cropImage
import photoglyph.core.img.detectPageQuad
import photoglyph.core.img.prepareImage
import photoglyph.core.img.rotateQuarterTurns
import photoglyph.core.img.suggestedRectifiedSize
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.concurrent.Semaphore
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.imageio.stream.MemoryCacheImageOutputStream

/** Independent M2 routes. Does not change accepted M1 diagnostic/glyph behavior. */
internal class PreparationAdapter(
    private val root: File,
) {
    private val gate = Semaphore(1)

    fun handle(request: HttpExchange): Boolean {
        val path = request.requestURI.path
        if (path !in ROUTES) return false
        try {
            if (path == "/api/prepare") {
                if (request.requestMethod != "POST") {
                    send(request, METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"method_not_allowed\"}".toByteArray())
                    return true
                }
                process(request)
            } else {
                if (request.requestMethod != "GET") {
                    send(request, METHOD_NOT_ALLOWED, "application/json", "{\"error\":\"method_not_allowed\"}".toByteArray())
                    return true
                }
                when (path) {
                    "/prepare" -> resource(request, "prepare.html", "text/html")
                    "/prepare.js" -> resource(request, "prepare.js", "application/javascript")
                    "/prepare.css" -> resource(request, "prepare.css", "text/css")
                    "/api/page-sample" -> send(request, OK, "image/png", File(root, "tools/fixtures/synthetic/m2/grid-yaw30-side-light.png").readBytes())
                    "/api/grid-sheet" -> send(request, OK, "image/svg+xml", gridSheetSvg(GridSheet(cyrillicGridCharacters())).toByteArray(StandardCharsets.UTF_8))
                }
            }
        } catch (failure: PreparationException) {
            send(request, failure.status, "application/json", "{\"error\":\"${failure.code}\"}".toByteArray())
        } catch (_: IllegalArgumentException) {
            send(request, BAD_REQUEST, "application/json", "{\"error\":\"invalid_parameters\"}".toByteArray())
        } catch (_: javax.imageio.IIOException) {
            send(request, UNSUPPORTED, "application/json", "{\"error\":\"unsupported_image\"}".toByteArray())
        }
        return true
    }

    private fun resource(
        request: HttpExchange,
        name: String,
        mime: String,
    ) {
        val bytes = checkNotNull(javaClass.getResourceAsStream("/sandbox/$name")).use { it.readBytes() }
        send(request, OK, mime, bytes)
    }

    private fun process(request: HttpExchange) {
        if (!gate.tryAcquire()) throw PreparationException(BUSY, "busy")
        try {
            val query = preparationQuery(request)
            require(query.keys.all { it in KEYS })
            val bytes = if (query["sample"] == "page") File(root, "tools/fixtures/synthetic/m2/grid-yaw30-side-light.png").readBytes() else request.requestBody.readNBytes(MAX_BYTES + 1)
            if (bytes.size > MAX_BYTES) throw PreparationException(TOO_LARGE, "too_large")
            if (bytes.isEmpty()) throw PreparationException(BAD_REQUEST, "empty_image")
            var image = decode(bytes)
            val operation = query["op"] ?: "detect"
            require(operation in OPERATIONS)
            if (operation == "rotate") image = rotateQuarterTurns(image, 1)
            if (operation == "crop") {
                val left = checkNotNull(query["left"]).toInt()
                val top = checkNotNull(query["top"]).toInt()
                image = cropImage(image, left, top, checkNotNull(query["width"]).toInt(), checkNotNull(query["height"]).toInt())
            }
            val detected = detectPageQuad(image)
            val supplied = query["quad"]?.split(',')?.map { it.toDouble() }
            val quad =
                if (supplied != null) {
                    require(supplied.size == QUAD_NUMBERS && supplied.all { it.isFinite() })
                    supplied
                        .chunked(2)
                        .map { Vec2(it[0], it[1]) }
                        .let { points -> PageQuad(points[0], points[1], points[2], points[BOTTOM_LEFT_INDEX]) }
                        .also { it.requireInside(image) }
                } else {
                    detected ?: PageQuad.fullImage(image)
                }
            val params = PipelineParams(adaptiveWindowSizePx = query["window"]?.toInt() ?: PipelineParams().adaptiveWindowSizePx, adaptiveOffset = query["offset"]?.toDouble() ?: PipelineParams().adaptiveOffset)
            require(params.adaptiveWindowSizePx <= MAX_WINDOW && params.adaptiveOffset in 0.0..MAX_OFFSET)
            val dimensions = if (query["a4"] == "true") GRID_WIDTH to GRID_HEIGHT else suggestedRectifiedSize(quad, OUTPUT_MAX_EDGE)
            val start = System.nanoTime()
            val prepared = if (operation == "prepare") prepareImage(image, quad, dimensions.first, dimensions.second, params) else null
            val elapsed = (System.nanoTime() - start) / NANOS_PER_MS
            val corners = quad.corners.joinToString(",", prefix = "[", postfix = "]") { "[${it.x},${it.y}]" }
            val encoder = Base64.getEncoder()
            val source = encoder.encodeToString(encodeArgb(image))
            val binary = prepared?.let { data -> encoder.encodeToString(encodePng(data.binary.width, data.binary.height) { x, y -> if (data.binary[x, y]) BLACK else WHITE }) }
            val rectified = prepared?.let { encoder.encodeToString(encodeArgb(it.rectified)) }
            val body = "{\"width\":${image.width},\"height\":${image.height},\"detected\":${detected != null},\"quad\":$corners,\"sourceBase64\":\"$source\",\"preparedWidth\":${prepared?.binary?.width ?: 0},\"preparedHeight\":${prepared?.binary?.height ?: 0},\"prepareMs\":${String.format(Locale.ROOT, "%.3f", elapsed)},\"binaryBase64\":${binary?.let { "\"$it\"" } ?: "null"},\"rectifiedBase64\":${rectified?.let { "\"$it\"" } ?: "null"},\"ocr\":false,\"outlines\":false}"
            send(request, OK, "application/json", body.toByteArray(StandardCharsets.UTF_8))
        } finally {
            gate.release()
        }
    }
}

private class PreparationException(
    val status: Int,
    val code: String,
) : RuntimeException(code)

private fun preparationQuery(request: HttpExchange): Map<String, String> {
    val raw = request.requestURI.rawQuery ?: return emptyMap()
    val values = linkedMapOf<String, String>()
    for (entry in raw.split('&')) {
        val pair = entry.split('=', limit = 2)
        require(pair.size == 2)
        val key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8)
        require(values.put(key, URLDecoder.decode(pair[1], StandardCharsets.UTF_8)) == null)
    }
    return values
}

private fun decode(bytes: ByteArray): ArgbImage {
    MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { input ->
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) throw PreparationException(UNSUPPORTED, "unsupported_image")
        val reader = readers.next()
        try {
            if (reader.formatName.lowercase(Locale.ROOT) !in setOf("png", "jpeg", "jpg")) throw PreparationException(UNSUPPORTED, "unsupported_image")
            reader.input = input
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width <= 0 || height <= 0 || width.toLong() * height > MAX_PIXELS) throw PreparationException(TOO_LARGE, "too_large")
            val bitmap = reader.read(0)
            try {
                return ArgbImage(width, height, bitmap.getRGB(0, 0, width, height, null, 0, width))
            } finally {
                bitmap.flush()
            }
        } finally {
            reader.dispose()
        }
    }
}

private fun encodeArgb(image: ArgbImage): ByteArray = encodePng(image.width, image.height) { x, y -> image[y * image.width + x] }

private fun encodePng(
    width: Int,
    height: Int,
    pixel: (Int, Int) -> Int,
): ByteArray {
    val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    for (y in 0 until height) for (x in 0 until width) bitmap.setRGB(x, y, pixel(x, y))
    val bytes = ByteArrayOutputStream()
    MemoryCacheImageOutputStream(bytes).use { output ->
        val writer = ImageIO.getImageWritersByFormatName("png").next()
        try {
            writer.output = output
            writer.write(bitmap)
        } finally {
            writer.dispose()
            bitmap.flush()
        }
    }
    return bytes.toByteArray()
}

private fun send(
    request: HttpExchange,
    status: Int,
    mime: String,
    bytes: ByteArray,
) {
    request.responseHeaders.set("Content-Type", if (mime == "image/png") mime else "$mime; charset=utf-8")
    request.responseHeaders.set("Cache-Control", "no-store")
    request.responseHeaders.set("X-Content-Type-Options", "nosniff")
    request.responseHeaders.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'none'")
    if (mime == "image/svg+xml") request.responseHeaders.set("Content-Disposition", "attachment; filename=photoglyph-a4-grid.svg")
    request.sendResponseHeaders(status, bytes.size.toLong())
    request.responseBody.use { it.write(bytes) }
}

private val ROUTES = setOf("/prepare", "/prepare.js", "/prepare.css", "/api/prepare", "/api/page-sample", "/api/grid-sheet")
private val KEYS = setOf("sample", "op", "quad", "window", "offset", "a4", "left", "top", "width", "height")
private val OPERATIONS = setOf("detect", "rotate", "crop", "prepare")
private const val MAX_BYTES = 5 * 1024 * 1024
private const val MAX_PIXELS = 1_000_000L
private const val OUTPUT_MAX_EDGE = 800
private const val MAX_WINDOW = 101
private const val MAX_OFFSET = 50.0
private const val GRID_WIDTH = 420
private const val GRID_HEIGHT = 594
private const val QUAD_NUMBERS = 8
private const val NANOS_PER_MS = 1_000_000.0
private const val BLACK = 0xff000000.toInt()
private const val WHITE = 0xffffffff.toInt()
private const val OK = 200
private const val BAD_REQUEST = 400
private const val METHOD_NOT_ALLOWED = 405
private const val TOO_LARGE = 413
private const val UNSUPPORTED = 415
private const val BUSY = 429

private const val BOTTOM_LEFT_INDEX = 3
