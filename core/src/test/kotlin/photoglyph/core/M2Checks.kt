package photoglyph.core

import photoglyph.core.export.GridSheet
import photoglyph.core.export.cyrillicGridCharacters
import photoglyph.core.export.gridSheetSvg
import photoglyph.core.geom.Vec2
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.GrayImage
import photoglyph.core.img.PageQuad
import photoglyph.core.img.cropImage
import photoglyph.core.img.detectPageQuad
import photoglyph.core.img.mirrorHorizontal
import photoglyph.core.img.suggestedRectifiedSize
import photoglyph.core.img.mapPagePoint
import photoglyph.core.img.otsuThreshold
import photoglyph.core.img.prepareImage
import photoglyph.core.img.rectifyPerspective
import photoglyph.core.img.resizeToMaxEdge
import photoglyph.core.img.rotateQuarterTurns
import photoglyph.core.img.whiteProjectionColumns
import photoglyph.core.img.whiteProjectionRows
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

/** Real production-function cases shared between JUnit and the restricted standalone runner. */
object M2Checks {
    fun quarterTurnsArePixelExact() {
        val image = ArgbImage(2, 3, IntArray(6) { it + 1 })
        val rotated = rotateQuarterTurns(image, 1)
        check(rotated.width == 3 && rotated.height == 2)
        check(rotated.copyPixels().contentEquals(intArrayOf(5, 3, 1, 6, 4, 2)))
        check(rotateQuarterTurns(rotated, -1).copyPixels().contentEquals(image.copyPixels()))
        check(image.copyPixels().contentEquals(intArrayOf(1, 2, 3, 4, 5, 6)))
    }

    fun mirrorIsPixelExactAndNonmutating() {
        val image = ArgbImage(3, 2, intArrayOf(1, 2, 3, 4, 5, 6))
        check(mirrorHorizontal(image).copyPixels().contentEquals(intArrayOf(3, 2, 1, 6, 5, 4)))
        check(image.copyPixels().contentEquals(intArrayOf(1, 2, 3, 4, 5, 6)))
    }

    fun suggestedSizeDoesNotGuessA4() {
        val quad = PageQuad(Vec2(0.0, 0.0), Vec2(300.0, 0.0), Vec2(300.0, 100.0), Vec2(0.0, 100.0))
        check(suggestedRectifiedSize(quad, 150) == (150 to 50))
    }

    fun cropIsPixelExactAndRejectsOutside() {
        val image = ArgbImage(4, 4, IntArray(16) { it })
        check(cropImage(image, 1, 1, 2, 2).copyPixels().contentEquals(intArrayOf(5, 6, 9, 10)))
        rejects { cropImage(image, 3, 0, 2, 1) }
        rejects { cropImage(image, -1, 0, 2, 1) }
    }

    fun resizeIdentityAndAverageAreIndependentExpectations() {
        val image = ArgbImage(2, 2, intArrayOf(0xff000000.toInt(), 0xff202020.toInt(), 0xff404040.toInt(), 0xff606060.toInt()))
        check(resizeToMaxEdge(image, 4).copyPixels().contentEquals(image.copyPixels()))
        val small = resizeToMaxEdge(image, 1)
        check(small.width == 1 && small.height == 1 && small[0] == 0xff303030.toInt())
    }

    fun perspectiveMapsKnownCornersAndCenter() {
        val quad = PageQuad(Vec2(1.0, 2.0), Vec2(7.0, 2.0), Vec2(7.0, 10.0), Vec2(1.0, 10.0))
        val unit = listOf(Vec2(0.0, 0.0), Vec2(1.0, 0.0), Vec2(1.0, 1.0), Vec2(0.0, 1.0))
        for (index in unit.indices) near(mapPagePoint(quad, unit[index]), quad.corners[index])
        near(mapPagePoint(quad, Vec2(0.5, 0.5)), Vec2(4.0, 6.0))
    }

    fun identityPerspectivePreservesArgb() {
        val image = ArgbImage(4, 3, IntArray(12) { 0xff000000.toInt() or (it * 0x010203) })
        check(rectifyPerspective(image, PageQuad.fullImage(image), 4, 3).copyPixels().contentEquals(image.copyPixels()))
    }

    fun rectangularPerspectiveMatchesHandSpecifiedCrop() {
        val image = ArgbImage(5, 4, IntArray(20) { 0xff000000.toInt() or it })
        val quad = PageQuad(Vec2(1.0, 1.0), Vec2(4.0, 1.0), Vec2(4.0, 3.0), Vec2(1.0, 3.0))
        val result = rectifyPerspective(image, quad, 3, 2)
        check(result.copyPixels().contentEquals(intArrayOf(6, 7, 8, 11, 12, 13).map { 0xff000000.toInt() or it }.toIntArray()))
    }

    fun degenerateQuadsAndAllocationBoundsAreRejected() {
        rejects { PageQuad(Vec2(0.0, 0.0), Vec2(1.0, 1.0), Vec2(1.0, 0.0), Vec2(0.0, 1.0)) }
        val image = ArgbImage(2, 2, IntArray(4))
        rejects { rectifyPerspective(image, PageQuad.fullImage(image), 2000, 2000) }
        rejects { rectifyPerspective(image, PageQuad(Vec2(0.0, 0.0), Vec2(3.0, 0.0), Vec2(3.0, 2.0), Vec2(0.0, 2.0)), 3, 2) }
    }

    fun otsuHasHandDefinedBimodalThreshold() {
        check(otsuThreshold(GrayImage(4, 1, doubleArrayOf(10.0, 10.0, 230.0, 230.0))) == 10.0)
    }

    fun ambiguousBlankPageDoesNotInventDetection() {
        val image = ArgbImage(20, 20, IntArray(400) { 0xffffffff.toInt() })
        check(detectPageQuad(image) == null)
    }

    fun detectAnalyticYaw30SideLightCorners() {
        val photo = fixture("grid-yaw30-side-light.png")
        val detected = checkNotNull(detectPageQuad(photo)) { "Paper candidate not found in original analytic scene" }
        for ((actual, expected) in detected.corners.zip(expectedQuad().corners)) {
            check(actual.distanceTo(expected) <= CORNER_TOLERANCE_PX) { "Corner delta ${actual.distanceTo(expected)}: $actual vs $expected" }
        }
    }

    fun rectifyAnalyticPhotoAndAlignKnownGridProfiles() {
        val photo = fixture("grid-yaw30-side-light.png")
        val prepared = prepareImage(photo, expectedQuad(), 420, 594)
        verifyGridProfiles(prepared.binary)
        check(prepared.binary.foregroundCount > 0)
        check(photo.width == 720 && photo.height == 840)
    }

    fun autoDetectedRectificationAlsoAlignsKnownGrid() {
        val photo = fixture("grid-yaw30-side-light.png")
        val prepared = prepareImage(photo, checkNotNull(detectPageQuad(photo)), 420, 594)
        verifyGridProfiles(prepared.binary)
    }

    fun gridSheetHasOwnLabelsMarksAndEscaping() {
        val svg = gridSheetSvg(GridSheet(cyrillicGridCharacters()))
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(svg.byteInputStream())
        check(document.documentElement.getAttribute("width") == "210.0mm")
        check(document.getElementsByTagName("text").length == 66)
        check(document.getElementsByTagName("rect").length == 4)
        check(svg.contains("Ё") && svg.contains("ё"))
        val escaped = gridSheetSvg(GridSheet(listOf("&", "<", ">", "\"", "'")))
        check(escaped.contains("&amp;") && escaped.contains("&lt;") && escaped.contains("&quot;"))
    }

    private fun verifyGridProfiles(binary: photoglyph.core.img.BinaryGrid) {
        val columns = whiteProjectionColumns(binary)
        val rows = whiteProjectionRows(binary)
        for (expected in List(7) { 24 + it * 62 }) {
            val valley = (expected - PROFILE_TOLERANCE_PX..expected + PROFILE_TOLERANCE_PX).minBy { columns[it] }
            check(columns[valley] < binary.height / 3) { "No vertical grid valley near $expected: ${columns[valley]}" }
        }
        for (expected in List(12) { 56 + it * 46 }) {
            val valley = (expected - PROFILE_TOLERANCE_PX..expected + PROFILE_TOLERANCE_PX).minBy { rows[it] }
            check(rows[valley] < binary.width / 3) { "No horizontal grid valley near $expected: ${rows[valley]}" }
        }
    }

    private fun expectedQuad(): PageQuad {
        val text = File(root(), "tools/fixtures/synthetic/m2/manifest.json").readText()
        val part = text.substringAfter("\"quad\":").substringBefore("\"gridColumns\"")
        val numbers = Regex("-?[0-9]+(?:\\.[0-9]+)?").findAll(part).map { it.value.toDouble() }.toList()
        check(numbers.size == 8)
        val points = numbers.chunked(2).map { Vec2(it[0], it[1]) }
        return PageQuad(points[0], points[1], points[2], points[3])
    }

    private fun fixture(name: String): ArgbImage {
        val bitmap = checkNotNull(ImageIO.read(File(root(), "tools/fixtures/synthetic/m2/$name")))
        return ArgbImage(bitmap.width, bitmap.height, bitmap.getRGB(0, 0, bitmap.width, bitmap.height, null, 0, bitmap.width))
    }

    private fun root(): File = File(checkNotNull(System.getProperty("photoglyph.projectRoot")))

    private fun near(
        actual: Vec2,
        expected: Vec2,
    ) = check(actual.distanceTo(expected) < 1e-7)

    private fun rejects(run: () -> Unit) {
        var rejected = false
        try {
            run()
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        check(rejected) { "Invalid input was not rejected" }
    }

    private const val CORNER_TOLERANCE_PX = 6.0
    private const val PROFILE_TOLERANCE_PX = 3
}
