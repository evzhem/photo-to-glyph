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

/** Binary input path for manually selected prepared glyphs; it does not detect labels or recover pen order. */
data class BinaryVectorizationResult(
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

/**
 * Thins a user-selected binary crop and extracts open centerline chains without thresholding it again.
 * Tiny disconnected marks are retained by default; this is not character recognition or pen-order recovery.
 */
fun vectorizeBinary(
    binary: BinaryGrid,
    params: PipelineParams = PipelineParams(minComponentAreaPx = MIN_BINARY_COMPONENT_AREA_PX),
): BinaryVectorizationResult {
    val cleaned = removeSmallComponents(binary, params)
    val skeleton = zhangSuen(cleaned)
    val strokes = skeletonToPolylines(skeleton, params)
    return BinaryVectorizationResult(cleaned, skeleton, strokes)
}

private const val MIN_BINARY_COMPONENT_AREA_PX = 1
