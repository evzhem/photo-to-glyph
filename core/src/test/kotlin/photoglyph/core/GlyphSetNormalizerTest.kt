package photoglyph.core

import org.junit.Test

class GlyphSetNormalizerTest {
    @Test fun descendersUseTheSharedBaselineAndReferenceScale() = GlyphSetNormalizerChecks.descendersUseTheSharedBaselineAndReferenceScale()

    @Test fun invalidLabelsAndMetricsAreRejected() = GlyphSetNormalizerChecks.invalidLabelsAndMetricsAreRejected()
}
