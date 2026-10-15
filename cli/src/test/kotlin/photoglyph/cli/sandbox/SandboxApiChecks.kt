package photoglyph.cli.sandbox

import photoglyph.core.PipelineParams
import photoglyph.core.export.requireStrokeOnlySvg
import photoglyph.core.export.strokesToSvg
import photoglyph.core.img.ArgbImage
import photoglyph.core.vectorize
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import javax.imageio.ImageIO

/** Shared JUnit/standalone cases. Every case executes the real core via HTTP, including recovery checks. */
object SandboxApiChecks {
    fun fiveFixturesExecuteActualCore() =
        withServer { base, client, root ->
            for (code in listOf(1046, 1041, 1074, 1064, 1092)) {
                val bytes = fixture(root, code).readBytes()
                val response = post(client, "$base/api/diagnose?sample=$code")
                check(response.statusCode() == 200)
                verifyActualCoreResult(response.body(), bytes, PipelineParams())
                check(response.body().contains("\"synthetic\":true"))
            }
        }

    fun parameterChangesReachActualCore() =
        withServer { base, client, root ->
            val params = PipelineParams(adaptiveWindowSizePx = 35, adaptiveOffset = 8.0, minComponentAreaPx = 4, rdpEpsilonPx = 1.2, pruningIterations = 1)
            val response = post(client, "$base/api/diagnose?sample=1046&window=35&offset=8&minArea=4&rdp=1.2&pruning=1")
            check(response.statusCode() == 200)
            verifyActualCoreResult(response.body(), fixture(root, 1046).readBytes(), params)
            check(response.body().contains("\"rdp\":1.2"))
        }

    fun uploadedPngExecutesActualCore() =
        withServer { base, client, root ->
            val bytes = fixture(root, 1041).readBytes()
            val response = post(client, "$base/api/diagnose", bytes)
            check(response.statusCode() == 200)
            check(response.body().contains("\"synthetic\":false")) // Upload origin, not a handwriting classifier.
            verifyActualCoreResult(response.body(), bytes, PipelineParams())
        }

    fun uploadedJpegExecutesActualCore() =
        withServer { base, client, root ->
            val bitmap = ImageIO.read(fixture(root, 1064))
            val encoded = ByteArrayOutputStream()
            check(ImageIO.write(bitmap, "jpeg", encoded))
            bitmap.flush()
            val bytes = encoded.toByteArray()
            val response = post(client, "$base/api/diagnose", bytes)
            check(response.statusCode() == 200)
            verifyActualCoreResult(response.body(), bytes, PipelineParams())
        }

    fun invalidParametersReleaseTheCoreForRecovery() =
        withServer { base, client, root ->
            for (suffix in listOf("window=24", "offset=NaN", "rdp=Infinity", "window=25&window=35", "unknown=1")) {
                val response = post(client, "$base/api/diagnose?sample=1046&$suffix")
                check(response.statusCode() == 400 && response.body().contains("invalid_parameters"))
                verifyRecovery(base, client, root)
            }
        }

    fun badImagesReleaseTheCoreForRecovery() =
        withServer { base, client, root ->
            val bad = post(client, "$base/api/diagnose", "Not an image".toByteArray())
            check(bad.statusCode() == 415 && bad.body().contains("unsupported_image"))
            verifyRecovery(base, client, root)
            val empty = post(client, "$base/api/diagnose")
            check(empty.statusCode() == 400 && empty.body().contains("empty_image"))
            verifyRecovery(base, client, root)
        }

    fun oversizedInputsReleaseTheCoreForRecovery() =
        withServer { base, client, root ->
            val hugeBody = post(client, "$base/api/diagnose", ByteArray(5 * 1024 * 1024 + 1), expectContinue = true)
            check(hugeBody.statusCode() == 413)
            verifyRecovery(base, client, root)
            // Controlled synthetic raster, generated in memory, not a committed dataset.
            val bitmap = BufferedImage(1100, 1000, BufferedImage.TYPE_INT_RGB)
            val encoded = ByteArrayOutputStream()
            check(ImageIO.write(bitmap, "png", encoded))
            bitmap.flush()
            val hugePixels = post(client, "$base/api/diagnose", encoded.toByteArray())
            check(hugePixels.statusCode() == 413)
            verifyRecovery(base, client, root)
        }

    fun routesAndLocalesPreserveAWorkingCore() =
        withServer { base, client, root ->
            for (path in listOf("/.git/config", "/api/fixtures/../../.git/config", "/api/fixtures/9999.png")) {
                check(get(client, base + path).statusCode() == 404)
            }
            check(get(client, "$base/api/diagnose").statusCode() == 405)
            val ru = get(client, "$base/api/config?lang=ru")
            val en = get(client, "$base/api/config?lang=en")
            check(ru.statusCode() == 200 && ru.body().contains("Сначала проверяем штрихи."))
            check(en.statusCode() == 200 && en.body().contains("First, test the strokes."))
            verifyRecovery(base, client, root)
        }

    private fun verifyRecovery(
        base: String,
        client: HttpClient,
        root: File,
    ) {
        val response = post(client, "$base/api/diagnose?sample=1046")
        check(response.statusCode() == 200) { "Core did not recover after rejected input" }
        verifyActualCoreResult(response.body(), fixture(root, 1046).readBytes(), PipelineParams())
    }

    private fun verifyActualCoreResult(
        body: String,
        bytes: ByteArray,
        params: PipelineParams,
    ) {
        val bitmap = checkNotNull(ImageIO.read(ByteArrayInputStream(bytes)))
        val image = ArgbImage(bitmap.width, bitmap.height, bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width))
        bitmap.flush()
        // Expected values invoke the production pipeline too; no copy of its mathematics.
        val result = vectorize(image, params)
        val expectedSvg = strokesToSvg(result.strokes.polylines, image.width, image.height, params)
        val encodedSvg = checkNotNull(Regex("\"svgBase64\":\"([A-Za-z0-9+/=]+)\"").find(body)).groupValues[1]
        val actualSvg = String(Base64.getDecoder().decode(encodedSvg), Charsets.UTF_8)
        check(actualSvg == expectedSvg)
        check(requireStrokeOnlySvg(actualSvg) == result.strokes.polylines.size)
        check(body.contains("\"closedPolylines\":0"))
        check(body.contains("\"strokes\":${result.strokes.polylines.size}"))
    }

    private fun fixture(
        root: File,
        code: Int,
    ) = File(root, "tools/fixtures/synthetic/m0/$code.png")

    private fun withServer(body: (String, HttpClient, File) -> Unit) {
        val root = File(checkNotNull(System.getProperty("photoglyph.projectRoot")))
        val server = SandboxServer(root, 0, "127.0.0.1")
        try {
            server.start()
            val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
            body("http://127.0.0.1:${server.port}", client, root)
        } finally {
            server.close()
        }
    }

    private fun post(
        client: HttpClient,
        url: String,
        body: ByteArray = byteArrayOf(),
        expectContinue: Boolean = false,
    ): HttpResponse<String> {
        val request =
            HttpRequest
                .newBuilder(URI(url))
                .timeout(Duration.ofSeconds(20))
                .expectContinue(expectContinue)
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun get(
        client: HttpClient,
        url: String,
    ): HttpResponse<String> =
        client.send(
            HttpRequest
                .newBuilder(URI(url))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
}
