package photoglyph.cli.sandbox

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import photoglyph.core.PipelineParams
import photoglyph.core.VectorizationResult
import photoglyph.core.export.requireStrokeOnlySvg
import photoglyph.core.export.strokesToSvg
import photoglyph.core.img.ArgbImage
import photoglyph.core.vectorize
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream
import javax.imageio.stream.MemoryCacheImageOutputStream
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/** Developer diagnostic adapter only; no OCR, font storage or .gfont export. */
class SandboxServer(
    private val projectRoot: File,
    port: Int,
    host: String = "0.0.0.0",
) : AutoCloseable {
    private val executor =
        ThreadPoolExecutor(
            MIN_HTTP_THREADS,
            MAX_HTTP_THREADS,
            THREAD_IDLE_SECONDS,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(HTTP_QUEUE_LIMIT),
        )
    private val processing = Semaphore(MAX_CONCURRENT_PROCESSING)
    private val preparation = PreparationAdapter(projectRoot)
    private val http = HttpServer.create(InetSocketAddress(host, port), HTTP_BACKLOG)
    val port: Int get() = http.address.port

    init {
        http.executor = executor
        http.createContext("/", ::handle)
    }

    fun start() {
        http.start()
    }

    override fun close() {
        http.stop(0)
        executor.shutdownNow()
    }

    private fun handle(exchange: HttpExchange) {
        val request = exchange
        try {
            try {
                route(request)
            } catch (failure: SandboxRequestException) {
                respond(request, failure.status, "application/json", errorJson(failure.code).toByteArray())
            } catch (_: IllegalArgumentException) {
                respond(request, HTTP_BAD_REQUEST, "application/json", errorJson("invalid_parameters").toByteArray())
            } catch (failure: java.io.IOException) {
                // No original bytes, filenames or private filesystem paths are logged.
                System.err.println("Sandbox I/O failure: ${failure.javaClass.simpleName}")
            } catch (failure: IllegalStateException) {
                System.err.println("Sandbox invariant failure: ${failure.javaClass.simpleName}")
                respond(request, HTTP_SERVER_ERROR, "application/json", errorJson("processing_failed").toByteArray())
            }
        } finally {
            exchange.close()
        }
    }

    private fun route(request: HttpExchange) {
        if (preparation.handle(request)) return
        val path = request.requestURI.path
        if (path == "/api/diagnose") {
            requireMethod(request, "POST")
            diagnose(request)
            return
        }
        requireMethod(request, "GET")
        when {
            path == "/api/health" -> respondText(request, "application/json", "{\"status\":\"ok\",\"engine\":\"Kotlin/JVM\",\"milestone\":\"M1\",\"ocr\":false,\"gfont\":false,\"fontStorage\":false}")
            path == "/api/config" -> {
                val lang = query(request)["lang"] ?: "ru"
                if (lang !in LANGUAGES) throw SandboxRequestException(HTTP_BAD_REQUEST, "invalid_parameters")
                respondText(request, "application/json", configuration(lang))
            }
            path.startsWith("/api/fixtures/") -> {
                val code = path.removePrefix("/api/fixtures/").removeSuffix(".png")
                if (path != "/api/fixtures/$code.png" || code !in FIXTURES) throw SandboxRequestException(HTTP_NOT_FOUND, "not_found")
                respond(request, HTTP_OK, "image/png", fixture(code).readBytes())
            }
            path in STATIC_FILES -> {
                val (name, mime) = STATIC_FILES.getValue(path)
                val bytes = checkNotNull(javaClass.getResourceAsStream("/sandbox/$name")).use { it.readBytes() }
                respond(request, HTTP_OK, mime, bytes)
            }
            else -> throw SandboxRequestException(HTTP_NOT_FOUND, "not_found")
        }
    }

    private fun diagnose(request: HttpExchange) {
        if (!processing.tryAcquire()) throw SandboxRequestException(HTTP_BUSY, "busy")
        try {
            val query = query(request)
            require(query.keys.all { it in DIAGNOSTIC_KEYS })
            val params = parameters(query)
            val sample = query["sample"]
            val bytes =
                if (sample != null) {
                    if (sample !in FIXTURES) throw SandboxRequestException(HTTP_NOT_FOUND, "not_found")
                    fixture(sample).readBytes()
                } else {
                    val declared = request.requestHeaders.getFirst("Content-Length")?.toLongOrNull()
                    if (declared != null && declared > MAX_UPLOAD_BYTES) {
                        // Drain only the bounded prefix before closing. This avoids a TCP reset
                        // racing the 413 response for near-limit clients; no unbounded buffer.
                        request.requestBody.readNBytes(MAX_UPLOAD_BYTES + 1)
                        throw SandboxRequestException(HTTP_TOO_LARGE, "too_large")
                    }
                    val body = request.requestBody.readNBytes(MAX_UPLOAD_BYTES + 1)
                    if (body.size > MAX_UPLOAD_BYTES) throw SandboxRequestException(HTTP_TOO_LARGE, "too_large")
                    if (body.isEmpty()) throw SandboxRequestException(HTTP_BAD_REQUEST, "empty_image")
                    body
                }
            val image = decodeBoundedImage(bytes)
            val start = System.nanoTime()
            val result = vectorize(image, params)
            val coreMs = (System.nanoTime() - start) / NANOS_PER_MILLISECOND
            val svg = strokesToSvg(result.strokes.polylines, image.width, image.height, params)
            check(requireStrokeOnlySvg(svg) == result.strokes.polylines.size)
            val response = diagnosticJson(result, params, svg, coreMs, sample != null)
            respondText(request, "application/json", response)
        } finally {
            processing.release()
        }
    }

    private fun fixture(code: String): File = File(projectRoot, "tools/fixtures/synthetic/m0/$code.png")

    private fun configuration(language: String): String {
        val params = PipelineParams()
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        val input = checkNotNull(javaClass.getResourceAsStream("/sandbox/$language/strings.xml"))
        val entries = input.use { factory.newDocumentBuilder().parse(it).getElementsByTagName("string") }
        val strings =
            (0 until entries.length).joinToString(",") { index ->
                val element = entries.item(index) as org.w3c.dom.Element
                "${jsonString(element.getAttribute("name"))}:${jsonString(element.textContent)}"
            }
        return "{\"strings\":{$strings},\"maxUploadBytes\":$MAX_UPLOAD_BYTES,\"maxImagePixels\":$MAX_IMAGE_PIXELS,\"defaults\":${parametersJson(params)}}"
    }
}

fun runSandbox(
    projectRoot: File,
    port: Int,
) {
    val server = SandboxServer(projectRoot, port)
    Runtime.getRuntime().addShutdownHook(Thread { server.close() })
    server.start()
    println("PhotoGlyph diagnostic sandbox: http://0.0.0.0:${server.port}")
    println("Real Kotlin core; original synthetic examples; cropped-letter PNG/JPEG only; no OCR/gfont/storage")
    CountDownLatch(1).await()
}

private class SandboxRequestException(
    val status: Int,
    val code: String,
) : RuntimeException(code)

private fun parameters(query: Map<String, String>): PipelineParams {
    val defaults = PipelineParams()
    val window = query["window"]?.toInt() ?: defaults.adaptiveWindowSizePx
    val offset = query["offset"]?.toDouble() ?: defaults.adaptiveOffset
    val minArea = query["minArea"]?.toInt() ?: defaults.minComponentAreaPx
    val rdp = query["rdp"]?.toDouble() ?: defaults.rdpEpsilonPx
    val pruning = query["pruning"]?.toInt() ?: defaults.pruningIterations
    require(window in 1..MAX_WINDOW && window % 2 == 1)
    require(offset.isFinite() && offset in 0.0..MAX_OFFSET)
    require(minArea in 0..MAX_MIN_AREA)
    require(rdp.isFinite() && rdp in 0.0..MAX_RDP)
    require(pruning in 0..MAX_PRUNING_PASSES)
    return defaults.copy(
        adaptiveWindowSizePx = window,
        adaptiveOffset = offset,
        minComponentAreaPx = minArea,
        rdpEpsilonPx = rdp,
        pruningIterations = pruning,
    )
}

private fun decodeBoundedImage(bytes: ByteArray): ArgbImage {
    MemoryCacheImageInputStream(ByteArrayInputStream(bytes)).use { input ->
        val readers = ImageIO.getImageReaders(input)
        if (!readers.hasNext()) throw SandboxRequestException(HTTP_UNSUPPORTED, "unsupported_image")
        val reader = readers.next()
        try {
            if (reader.formatName.lowercase(Locale.ROOT) !in IMAGE_FORMATS) throw SandboxRequestException(HTTP_UNSUPPORTED, "unsupported_image")
            reader.input = input
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width <= 0 || height <= 0 || width.toLong() * height > MAX_IMAGE_PIXELS) {
                throw SandboxRequestException(HTTP_TOO_LARGE, "too_large")
            }
            val bitmap = reader.read(0)
            try {
                return ArgbImage(width, height, bitmap.getRGB(0, 0, width, height, null, 0, width))
            } finally {
                bitmap.flush()
            }
        } catch (_: javax.imageio.IIOException) {
            throw SandboxRequestException(HTTP_UNSUPPORTED, "unsupported_image")
        } finally {
            reader.dispose()
        }
    }
}

private fun diagnosticJson(
    result: VectorizationResult,
    params: PipelineParams,
    svg: String,
    coreMs: Double,
    synthetic: Boolean,
): String {
    val encoder = Base64.getEncoder()
    val width = result.cleaned.width
    val height = result.cleaned.height
    val grayscale = png(width, height) { x, y -> result.grayscale[x, y].toInt().coerceIn(0, CHANNEL_MAX) }
    val binary = png(width, height) { x, y -> if (result.cleaned[x, y]) 0 else CHANNEL_MAX }
    val skeleton = png(width, height) { x, y -> if (result.skeleton.grid[x, y]) 0 else CHANNEL_MAX }
    val strokes = result.strokes
    return "{" +
        "\"width\":$width,\"height\":$height,\"synthetic\":$synthetic," +
        "\"strokes\":${strokes.polylines.size},\"closedPolylines\":${strokes.polylines.count { it.isClosed }}," +
        "\"graphCycles\":${strokes.graphCycleRank},\"splitClosedChains\":${strokes.splitClosedChains}," +
        "\"skeletonPixels\":${result.skeleton.grid.foregroundCount},\"coreMs\":${String.format(Locale.ROOT, "%.3f", coreMs)}," +
        "\"params\":${parametersJson(params)}," +
        "\"svgBase64\":${jsonString(encoder.encodeToString(svg.toByteArray(StandardCharsets.UTF_8)))}," +
        "\"grayscaleBase64\":${jsonString(encoder.encodeToString(grayscale))}," +
        "\"binaryBase64\":${jsonString(encoder.encodeToString(binary))}," +
        "\"skeletonBase64\":${jsonString(encoder.encodeToString(skeleton))}}"
}

private fun png(
    width: Int,
    height: Int,
    level: (Int, Int) -> Int,
): ByteArray {
    val bitmap = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val value = level(x, y)
            bitmap.setRGB(x, y, (value shl RED_SHIFT) or (value shl GREEN_SHIFT) or value)
        }
    }
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

private fun query(request: HttpExchange): Map<String, String> {
    val raw = request.requestURI.rawQuery ?: return emptyMap()
    val values = linkedMapOf<String, String>()
    for (entry in raw.split('&')) {
        val pair = entry.split('=', limit = 2)
        require(pair.size == 2)
        val key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8)
        val value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8)
        require(values.put(key, value) == null) { "Duplicate parameters" }
    }
    return values
}

private fun parametersJson(params: PipelineParams): String = "{\"window\":${params.adaptiveWindowSizePx},\"offset\":${params.adaptiveOffset},\"minArea\":${params.minComponentAreaPx},\"rdp\":${params.rdpEpsilonPx},\"pruning\":${params.pruningIterations}}"

private fun requireMethod(
    request: HttpExchange,
    method: String,
) {
    if (request.requestMethod != method) {
        request.responseHeaders.set("Allow", method)
        throw SandboxRequestException(HTTP_METHOD_NOT_ALLOWED, "method_not_allowed")
    }
}

private fun jsonString(value: String): String =
    buildString {
        append('"')
        for (character in value) {
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < SPACE_CODE) append(String.format(Locale.ROOT, "\\u%04x", character.code)) else append(character)
            }
        }
        append('"')
    }

private fun errorJson(code: String): String = "{\"error\":${jsonString(code)}}"

private fun respondText(
    request: HttpExchange,
    mime: String,
    text: String,
) = respond(request, HTTP_OK, mime, text.toByteArray(StandardCharsets.UTF_8))

private fun respond(
    request: HttpExchange,
    status: Int,
    mime: String,
    bytes: ByteArray,
) {
    request.responseHeaders.set("Content-Type", if (mime.startsWith("image/")) mime else "$mime; charset=utf-8")
    request.responseHeaders.set("Cache-Control", "no-store")
    request.responseHeaders.set("X-Content-Type-Options", "nosniff")
    // No frame-ancestors/X-Frame-Options: Arena embeds this development preview.
    request.responseHeaders.set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data: blob:; connect-src 'self'; object-src 'none'; base-uri 'none'")
    request.sendResponseHeaders(status, bytes.size.toLong())
    request.responseBody.use { it.write(bytes) }
}

private val FIXTURES = setOf("1046", "1041", "1074", "1064", "1092")
private val LANGUAGES = setOf("ru", "en")
private val IMAGE_FORMATS = setOf("png", "jpeg", "jpg")
private val DIAGNOSTIC_KEYS = setOf("sample", "window", "offset", "minArea", "rdp", "pruning")
private val STATIC_FILES =
    mapOf(
        "/" to ("index.html" to "text/html"),
        "/index.html" to ("index.html" to "text/html"),
        "/app.css" to ("app.css" to "text/css"),
        "/app.js" to ("app.js" to "application/javascript"),
    )

// Adapter safety limits, not algorithm defaults. All algorithm tuning is PipelineParams.
private const val MAX_UPLOAD_BYTES = 5 * 1024 * 1024
private const val MAX_IMAGE_PIXELS = 1_000_000L
private const val MAX_WINDOW = 101
private const val MAX_OFFSET = 50.0
private const val MAX_MIN_AREA = 200
private const val MAX_RDP = 5.0
private const val MAX_PRUNING_PASSES = 10
private const val MAX_CONCURRENT_PROCESSING = 1
private const val MIN_HTTP_THREADS = 2
private const val MAX_HTTP_THREADS = 4
private const val HTTP_QUEUE_LIMIT = 32
private const val HTTP_BACKLOG = 16
private const val THREAD_IDLE_SECONDS = 30L
private const val NANOS_PER_MILLISECOND = 1_000_000.0
private const val CHANNEL_MAX = 255
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val SPACE_CODE = 32
private const val HTTP_OK = 200
private const val HTTP_BAD_REQUEST = 400
private const val HTTP_NOT_FOUND = 404
private const val HTTP_METHOD_NOT_ALLOWED = 405
private const val HTTP_TOO_LARGE = 413
private const val HTTP_UNSUPPORTED = 415
private const val HTTP_BUSY = 429
private const val HTTP_SERVER_ERROR = 500
