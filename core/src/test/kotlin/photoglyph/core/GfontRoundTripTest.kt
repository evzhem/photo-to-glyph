package photoglyph.core

import org.junit.Test

class GfontRoundTripTest {
    @Test fun partialGlyphSetRoundTripsDocumentedCoordinatesAndFlags() = GfontCodecChecks.partialGlyphSetRoundTripsDocumentedCoordinatesAndFlags()

    @Test fun uppercaseAndLowercaseRemainDistinctGfontEntries() = GfontCodecChecks.uppercaseAndLowercaseRemainDistinctGfontEntries()

    @Test fun malformedRecordsAndUnsupportedEntriesAreRejected() = GfontCodecChecks.malformedRecordsAndUnsupportedEntriesAreRejected()

    @Test fun supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field() = GfontCodecChecks.supplementaryPlaneCharactersAreRejectedByTheDocumentedUint16Field()
}
