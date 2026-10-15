package photoglyph.core

/** Algorithm configuration; image-stage distances are in raster pixels. */
data class PipelineParams(
    val adaptiveWindowSizePx: Int = 25,
    val adaptiveOffset: Double = 10.0,
    val minComponentAreaPx: Int = 12,
    val pruningIterations: Int = 2,
    val pruneBranchShorterThanPx: Double = 2.0,
    val rdpEpsilonPx: Double = 0.6,
    val svgStrokeWidthPx: Double = 1.0,
    val svgDecimalPlaces: Int = 4,
    // Used by the CLI benchmark only, not by any pure processing stage.
    val benchmarkWarmupRuns: Int = 2,
    val benchmarkMeasuredRuns: Int = 5,
    val processingBudgetMs: Double = 2_000.0,
    // M2 raster preparation and conservative bright-paper detection.
    val pageDetectionMaxEdgePx: Int = 480,
    val pageMinBrightFraction: Double = 0.15,
    val pageMaxBrightFraction: Double = 0.95,
    val pageMinQuadFraction: Double = 0.20,
    // Preserve small marks such as dots and accents in handwriting pages by default.
    val pageMinComponentAreaPx: Int = 1,
    val maxPreparedPixels: Int = 1_000_000,
    // Separate photo-to-black-contour mode; normalized Sobel magnitude is 0..255.
    val contourEdgeThreshold: Double = 48.0,
) {
    init {
        require(adaptiveWindowSizePx > 0 && adaptiveWindowSizePx % 2 == 1) {
            "Adaptive window must be positive and odd"
        }
        require(adaptiveOffset.isFinite())
        require(minComponentAreaPx >= 0)
        require(pruningIterations >= 0)
        require(pruneBranchShorterThanPx.isFinite() && pruneBranchShorterThanPx >= 0.0)
        require(rdpEpsilonPx.isFinite() && rdpEpsilonPx >= 0.0)
        require(svgStrokeWidthPx.isFinite() && svgStrokeWidthPx > 0.0)
        require(svgDecimalPlaces in 0..MAX_USEFUL_DOUBLE_DECIMALS)
        require(benchmarkWarmupRuns >= 0 && benchmarkMeasuredRuns > 0)
        require(processingBudgetMs.isFinite() && processingBudgetMs > 0.0)
        require(pageDetectionMaxEdgePx > 0 && maxPreparedPixels > 0 && pageMinComponentAreaPx >= 0)
        require(pageMinBrightFraction.isFinite() && pageMaxBrightFraction.isFinite())
        require(pageMinBrightFraction > 0.0 && pageMaxBrightFraction < 1.0 && pageMinBrightFraction < pageMaxBrightFraction)
        require(pageMinQuadFraction.isFinite() && pageMinQuadFraction in 0.0..1.0)
        require(contourEdgeThreshold.isFinite() && contourEdgeThreshold in 0.0..MAX_CONTOUR_EDGE_THRESHOLD)
    }
}

// Precision beyond this does not usefully describe a double coordinate in SVG.
private const val MAX_USEFUL_DOUBLE_DECIMALS = 15
private const val MAX_CONTOUR_EDGE_THRESHOLD = 255.0
