package photoglyph.core

import photoglyph.core.img.BinaryGrid

object BinaryVectorizeChecks {
    fun preparedOnePixelLineProducesAnOpenCenterlineWithoutASecondThresholdPass() {
        val binary =
            BinaryGrid(
                width = 9,
                height = 9,
                pixels =
                    BooleanArray(81) { index ->
                        val x = index % 9
                        val y = index / 9
                        y == 4 && x in 1..7
                    },
            )
        val result = vectorizeBinary(binary, PipelineParams(minComponentAreaPx = 1, pruningIterations = 0, rdpEpsilonPx = 0.0))

        check(result.cleaned.foregroundCount == 7)
        check(result.skeleton.grid.foregroundCount == 7)
        check(result.strokes.polylines.size == 1)
        val line = result.strokes.rawPolylines.single()
        check(!line.isClosed)
        check(line.points.size == 7)
        check(line.points.map { it.x }.toSet() == (1..7).map { it + 0.5 }.toSet())
        check(line.points.all { it.y == 4.5 })
    }

    fun defaultBinaryPathPreservesSmallDetachedMarks() {
        val binary =
            BinaryGrid(
                width = 5,
                height = 3,
                pixels =
                    BooleanArray(15) { index ->
                        val x = index % 5
                        val y = index / 5
                        y == 1 && x == 0 || y == 1 && x in 2..4
                    },
            )
        val result = vectorizeBinary(binary)

        check(result.cleaned.foregroundCount == 4)
        check(result.strokes.polylines.size == 2)
        check(result.strokes.polylines.all { !it.isClosed })
    }
}
