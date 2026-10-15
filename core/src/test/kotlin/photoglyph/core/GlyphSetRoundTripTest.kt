package photoglyph.core

import org.junit.Test

class GlyphSetRoundTripTest {
    @Test fun partialSetJsonRoundTripPreservesModelAndPngBytes() = GlyphSetChecks.partialSetJsonRoundTripPreservesModelAndPngBytes()

    @Test fun closedGlyphStrokesAndMismatchedKeysAreRejected() = GlyphSetChecks.closedGlyphStrokesAndMismatchedKeysAreRejected()
}
