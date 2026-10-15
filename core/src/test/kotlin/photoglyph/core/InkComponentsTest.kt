package photoglyph.core

import org.junit.Test

class InkComponentsTest {
    @Test fun componentsAreEightConnectedAndReadingOrdered() = InkComponentsChecks.componentsAreEightConnectedAndReadingOrdered()

    @Test fun componentCropIsPixelExactAndNonmutating() = InkComponentsChecks.componentCropIsPixelExactAndNonmutating()

    @Test fun blankPageHasNoInkComponents() = InkComponentsChecks.blankPageHasNoInkComponents()
}
