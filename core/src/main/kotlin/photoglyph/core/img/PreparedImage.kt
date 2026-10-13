package photoglyph.core.img

import photoglyph.core.PipelineParams

/** M2 output is a prepared raster, NOT recognized characters or vector outlines. */
data class PreparedImage(val rectified: ArgbImage, val grayscale: GrayImage, val binary: BinaryGrid)

fun prepareImage(image: ArgbImage, quad: PageQuad, width: Int, height: Int, params: PipelineParams = PipelineParams()): PreparedImage {
    val rectified = rectifyPerspective(image, quad, width, height, params)
    val grayscale = toGrayscale(rectified)
    val binary = removeSmallComponents(adaptiveThreshold(grayscale, params), params)
    return PreparedImage(rectified, grayscale, binary)
}

/** White counts: grid rules form valleys; no imaginary cell labels are assigned. */
fun whiteProjectionColumns(binary: BinaryGrid): List<Int> = List(binary.width) { x -> (0 until binary.height).count { y -> !binary[x, y] } }

fun whiteProjectionRows(binary: BinaryGrid): List<Int> = List(binary.height) { y -> (0 until binary.width).count { x -> !binary[x, y] } }
