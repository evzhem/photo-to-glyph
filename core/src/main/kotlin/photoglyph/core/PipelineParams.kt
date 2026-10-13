package photoglyph.core

/** All adjustable spike parameters; image-stage distances are in raster pixels. */
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
    }
}

// Precision beyond this does not usefully describe a double coordinate in SVG.
private const val MAX_USEFUL_DOUBLE_DECIMALS = 15
