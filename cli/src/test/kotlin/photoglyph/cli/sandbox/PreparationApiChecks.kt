package photoglyph.cli.sandbox

import photoglyph.core.PipelineParams
import photoglyph.core.geom.Vec2
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.PageQuad
import photoglyph.core.img.detectPageQuad
import photoglyph.core.img.prepareImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Base64
import javax.imageio.ImageIO

/** End-to-end adapters, never copies of perspective/threshold mathematics. */
object PreparationApiChecks {
    fun realPageDetectionAndPreparation() = withServer { base, client, root ->
        val response = post(client, "$base/api/prepare?sample=page&op=prepare&a4=true")
        check(response.statusCode() == 200)
        verifyBinary(response.body(), fixture(root), null)
        check(response.body().contains("\"outlines\":false") && response.body().contains("\"ocr\":false"))
    }

    fun manualCornersAndUploadedRasterReachCore() = withServer { base, client, root ->
        val file = File(root, "tools/fixtures/synthetic/m2/grid-yaw30-side-light.png")
        val image = fixture(root)
        val quad = PageQuad(Vec2(0.0, 0.0), Vec2(720.0, 0.0), Vec2(720.0, 840.0), Vec2(0.0, 840.0))
        val response = post(client, "$base/api/prepare?op=prepare&a4=true&quad=0,0,720,0,720,840,0,840", file.readBytes())
        check(response.statusCode() == 200)
        verifyBinary(response.body(), image, quad)
    }

    fun pixelExactRotationAndCrop() = withServer { base, client, root ->
        val rotated = post(client, "$base/api/prepare?sample=page&op=rotate")
        check(rotated.statusCode() == 200 && rotated.body().contains("\"width\":840") && rotated.body().contains("\"height\":720"))
        val cropped = post(client, "$base/api/prepare?sample=page&op=crop&left=10&top=20&width=40&height=50")
        check(cropped.statusCode() == 200 && cropped.body().contains("\"width\":40") && cropped.body().contains("\"height\":50"))
        // Recovery also runs the actual prepareImage path, not transport-only assertions.
        recovery(base, client, root)
    }

    fun badQuadAndParametersRecoverActualPreparation() = withServer { base, client, root ->
        for (suffix in listOf("quad=0,0,1,1,1,0,0,1", "window=24", "offset=NaN", "unknown=1", "quad=NaN,0,1,0,1,1,0,1")) {
            val response = post(client, "$base/api/prepare?sample=page&op=prepare&$suffix")
            check(response.statusCode() == 400)
            recovery(base, client, root)
        }
    }

    fun printableSheetAndStaticRoutesDoNotFakeAnOutline() = withServer { base, client, root ->
        val grid = get(client, "$base/api/grid-sheet")
        check(grid.statusCode() == 200 && grid.body().contains("width=\"210.0mm\""))
        check(Regex("<text ").findAll(grid.body()).count() == 66)
        check(get(client, "$base/prepare").statusCode() == 200)
        check(get(client, "$base/api/prepare").statusCode() == 405)
        check(get(client, "$base/.git/config").statusCode() == 404)
        recovery(base, client, root)
    }

    private fun recovery(base: String, client: HttpClient, root: File) {
        val response = post(client, "$base/api/prepare?sample=page&op=prepare&a4=true")
        check(response.statusCode() == 200)
        verifyBinary(response.body(), fixture(root), null)
    }

    private fun verifyBinary(body: String, image: ArgbImage, manual: PageQuad?) {
        val expected = prepareImage(image, manual ?: checkNotNull(detectPageQuad(image)), 420, 594, PipelineParams()).binary
        val data = checkNotNull(Regex("\"binaryBase64\":\"([A-Za-z0-9+/=]+)\"").find(body)).groupValues[1]
        val bitmap = checkNotNull(ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(data))))
        check(bitmap.width == expected.width && bitmap.height == expected.height)
        for (y in 0 until expected.height) for (x in 0 until expected.width) {
            check((bitmap.getRGB(x, y) and 0xffffff == 0) == expected[x, y])
        }
        bitmap.flush()
    }

    private fun fixture(root: File): ArgbImage {
        val bitmap = checkNotNull(ImageIO.read(File(root, "tools/fixtures/synthetic/m2/grid-yaw30-side-light.png")))
        return ArgbImage(bitmap.width, bitmap.height, bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width))
    }

    private fun withServer(run: (String, HttpClient, File) -> Unit) {
        val root = File(checkNotNull(System.getProperty("photoglyph.projectRoot")))
        val server = SandboxServer(root, 0, "127.0.0.1")
        try {
            server.start()
            run("http://127.0.0.1:${server.port}", HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), root)
        } finally { server.close() }
    }

    private fun post(client: HttpClient, url: String, bytes: ByteArray = byteArrayOf()): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(20)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(), HttpResponse.BodyHandlers.ofString(),
    )

    private fun get(client: HttpClient, url: String): HttpResponse<String> = client.send(HttpRequest.newBuilder(URI(url)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString())
}
