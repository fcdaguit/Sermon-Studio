package com.dlbc.sermonstudio.core.bible

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * These cases are taken directly from the project spec plus the failure modes
 * that matter most in practice (false positives inside ordinary words).
 *
 * The parsing algorithm was validated against every case below before this
 * file was written, but it has NOT been executed on a JVM in this form — run
 * `./gradlew test` as your first build step and treat any failure here as a
 * parser bug, not a test bug.
 */
class ReferenceParserTest {

    private fun single(input: String): BibleReference =
        ReferenceParser.parseSingle(input) ?: error("no reference parsed from '$input'")

    @Test fun `single verse full name`() {
        assertEquals(BibleReference.verse(43, 3, 16), single("John 3:16"))
    }

    @Test fun `single verse abbreviation`() {
        assertEquals(BibleReference.verse(43, 3, 16), single("Jn 3:16"))
    }

    @Test fun `verse range`() {
        assertEquals(BibleReference(40, 5, 1, 5, 12), single("Matthew 5:1-12"))
        assertEquals(BibleReference(40, 5, 1, 5, 12), single("Matt 5:1-12"))
    }

    @Test fun `whole chapter`() {
        val psalm = single("Psalm 23")
        assertTrue(psalm.isWholeChapter)
        assertEquals(19, psalm.bookNumber)
        assertEquals(23, psalm.startChapter)
        assertTrue(single("Ps 23").isWholeChapter)
    }

    @Test fun `multi verse range`() {
        assertEquals(BibleReference(45, 8, 28, 8, 30), single("Romans 8:28-30"))
    }

    @Test fun `comma list yields two references`() {
        val found = ReferenceParser.detect("Genesis 1:1, 3")
        assertEquals(2, found.size)
        assertEquals(BibleReference.verse(1, 1, 1), found[0].reference)
        assertEquals(BibleReference.verse(1, 1, 3), found[1].reference)
        // Both belong to one source span.
        assertEquals(found[0].groupId, found[1].groupId)
    }

    @Test fun `cross chapter range`() {
        assertEquals(BibleReference(40, 5, 1, 6, 4), single("Matt 5:1-6:4"))
    }

    @Test fun `numbered book beats bare book`() {
        assertEquals(62, single("1 John 4:8").bookNumber)
        assertEquals(43, single("John 4:8").bookNumber)
    }

    @Test fun `abbreviation with period`() {
        assertEquals(BibleReference(46, 13, 4, 13, 7), single("1 Cor. 13:4-7"))
    }

    @Test fun `does not match inside a word`() {
        assertTrue(ReferenceParser.detect("Johnson said so").isEmpty())
        assertTrue(ReferenceParser.detect("the Romance languages").isEmpty())
    }

    @Test fun `single chapter book links without a number`() {
        val found = ReferenceParser.detect("See Jude for context")
        assertEquals(1, found.size)
        assertEquals(65, found[0].reference.bookNumber)
    }

    @Test fun `rejects chapter beyond book length`() {
        // John has 21 chapters.
        assertTrue(ReferenceParser.detect("John 99:1").isEmpty())
    }

    @Test fun `multiple references in one sentence`() {
        val found = ReferenceParser.detect("Compare Psalm 23 with John 3:16 today")
        assertEquals(2, found.size)
        assertEquals(19, found[0].reference.bookNumber)
        assertEquals(43, found[1].reference.bookNumber)
    }

    @Test fun `display round trips`() {
        assertEquals("Matthew 5:1-12", single("Matt 5:1-12").display())
        assertEquals("Psalm 23", single("Ps 23").display())
        assertEquals("John 3:16", single("Jn 3:16").display())
    }
}
