package photoglyph.core

import photoglyph.core.img.BinaryGrid
import photoglyph.core.img.InkComponent
import photoglyph.core.img.PixelBounds
import photoglyph.core.img.connectedInkComponents
import photoglyph.core.img.cropBinaryGrid

/** Tests for production page-ink candidate segmentation; labels are intentionally not inferred. */
object InkComponentsChecks {
    fun componentsAreEightConnectedAndReadingOrdered() {
        val pixels = BooleanArray(WIDTH * HEIGHT)
        pixels[0] = true
        pixels[1 * WIDTH + 1] = true
        pixels[4] = true
        pixels[5] = true
        pixels[1 * WIDTH + 5] = true
        pixels[4 * WIDTH + 1] = true
        val components = connectedInkComponents(BinaryGrid(WIDTH, HEIGHT, pixels))
        check(
            components ==
                listOf(
                    InkComponent(PixelBounds(0, 0, 2, 2), 2),
                    InkComponent(PixelBounds(4, 0, 6, 2), 3),
                    InkComponent(PixelBounds(1, 4, 2, 5), 1),
                ),
        )
    }

    fun componentCropIsPixelExactAndNonmutating() {
        val pixels = BooleanArray(WIDTH * HEIGHT)
        pixels[4] = true
        pixels[5] = true
        pixels[1 * WIDTH + 5] = true
        val source = BinaryGrid(WIDTH, HEIGHT, pixels)
        val crop = cropBinaryGrid(source, PixelBounds(4, 0, 6, 2))
        check(crop.width == 2 && crop.height == 2)
        check(crop.copyPixels().contentEquals(booleanArrayOf(true, true, false, true)))
        check(source.copyPixels().contentEquals(pixels))
    }

    fun blankPageHasNoInkComponents() {
        check(connectedInkComponents(BinaryGrid(WIDTH, HEIGHT, BooleanArray(WIDTH * HEIGHT))).isEmpty())
    }

    private const val WIDTH = 7
    private const val HEIGHT = 6
}
