package photoglyph.core

import photoglyph.core.graph.StrokeExtraction
import photoglyph.core.graph.skeletonToPolylines
import photoglyph.core.img.ArgbImage
import photoglyph.core.img.BinaryGrid
import photoglyph.core.img.GrayImage
import photoglyph.core.img.adaptiveThreshold
import photoglyph.core.img.removeSmallComponents
import photoglyph.core.img.toGrayscale
import photoglyph.core.thin.SkeletonPixels
import photoglyph.core.thin.zhangSuen

/** Intermediate values are exposed for independent golden tests and honest diagnostics. */
data class VectorizationResult(
    val grayscale: GrayImage,
    val thresholded: BinaryGrid,
    val cleaned: BinaryGrid,
    val skeleton: SkeletonPixels,
    val strokes: StrokeExtraction,
)

/** Pure M0 spike: no perspective/deskew, smoothing, normalization or stroke-order recovery. */
fun vectorize(
    image: ArgbImage,
    params: PipelineParams = PipelineParams(),
): VectorizationResult {
    val grayscale = toGrayscale(image)
    val thresholded = adaptiveThreshold(grayscale, params)
    val cleaned = removeSmallComponents(thresholded, params)
    val skeleton = zhangSuen(cleaned)
    val strokes = skeletonToPolylines(skeleton, params)
    return VectorizationResult(grayscale, thresholded, cleaned, skeleton, strokes)
}
