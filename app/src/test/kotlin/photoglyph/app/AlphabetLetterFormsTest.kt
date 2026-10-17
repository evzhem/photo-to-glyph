package photoglyph.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphabetLetterFormsTest {
    @Test
    fun cyrillicCapitalAndSmallLettersAreSeparateFormsIncludingYo() {
        val uppercase = AlphabetLetterForms.uppercase(HandwritingScript.CYRILLIC)
        val lowercase = AlphabetLetterForms.lowercase(HandwritingScript.CYRILLIC)

        assertEquals(33, uppercase.size)
        assertEquals(33, lowercase.size)
        assertTrue("Ё" in uppercase)
        assertTrue("ё" in lowercase)
        assertEquals(setOf("А", "а"), setOf(uppercase.first(), lowercase.first()))
        assertEquals("а", lowercase[uppercase.indexOf("А")])
    }

    @Test
    fun coverageCountsUppercaseAndLowercaseIndependently() {
        val progress = AlphabetLetterForms.progress(HandwritingScript.CYRILLIC, setOf("А", "ё", "я"))

        assertEquals(1, progress.uppercaseAdded)
        assertEquals(33, progress.uppercaseTotal)
        assertEquals(2, progress.lowercaseAdded)
        assertEquals(33, progress.lowercaseTotal)
    }

    @Test
    fun latinAlphabetProvidesSeparateTwentySixLetterCases() {
        val progress = AlphabetLetterForms.progress(HandwritingScript.LATIN, setOf("A", "a", "Z"))

        assertEquals(26, progress.uppercaseTotal)
        assertEquals(26, progress.lowercaseTotal)
        assertEquals(2, progress.uppercaseAdded)
        assertEquals(1, progress.lowercaseAdded)
    }
}
