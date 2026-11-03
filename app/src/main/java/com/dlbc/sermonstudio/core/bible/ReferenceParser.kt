package com.dlbc.sermonstudio.core.bible

/**
 * Detects scripture references in free text.
 *
 * Design notes
 * ------------
 * This is a two-stage scanner rather than one monster regex:
 *
 *   1. A generated alternation matches a *book token* (longest alias first, so
 *      "1 John" wins over "John" and "Song of Solomon" over "Song").
 *   2. From the end of that match, a hand-rolled cursor reads the numeric tail:
 *      chapter, optional `:verse`, optional `-end`, and optional `, n` repeats.
 *
 * Doing stage 2 by hand (instead of regex) is what makes `Gen 1:1, 3` and
 * `Matt 5:1-6:4` tractable without a combinatorial pattern, and it lets the
 * parser stop cleanly at the first thing it does not understand instead of
 * backtracking across the whole paragraph.
 *
 * Performance: the parser is allocation-light and runs over the visible text
 * only. In the editor it is called on a debounce (see EditorViewModel), not on
 * every keystroke, because on a long outline the cost is linear in text length.
 *
 * Known limitations, stated plainly:
 *  - A bare "Acts 2" inside a sentence like "the acts 2 men performed" will
 *    false-positive. The editor mitigates this by requiring the book token to
 *    start at a word boundary and by letting the user dismiss a chip.
 *  - Verse-level validity is not checked here (see BookCatalog docs). A
 *    reference to John 3:999 parses; the lookup simply returns no verses.
 *  - Roman-numeral book prefixes ("II Timothy") are handled via aliases only
 *    where listed; arbitrary roman numerals are not parsed.
 */
object ReferenceParser {

    /** Strip punctuation and collapse whitespace so alias lookup is forgiving. */
    fun normalizeToken(raw: String): String =
        raw.lowercase()
            .replace('.', ' ')
            .replace(',', ' ')
            .filter { it.isLetterOrDigit() || it == ' ' }
            .trim()
            .replace(Regex("\\s+"), " ")

    private val bookPattern: Regex by lazy {
        val alternatives = BookCatalog.books
            .flatMap { it.aliases }
            .distinct()
            // Longest first so the regex engine prefers "1 corinthians" to "1 cor".
            .sortedByDescending { it.length }
            .joinToString("|") { alias ->
                // Allow an optional period after each word (e.g. "1 Cor." / "Jn.")
                // and flexible whitespace between words of a multi-word name.
                alias.split(' ')
                    .joinToString("\\.?\\s*") { Regex.escape(it) }
            }
        // (?<![\p{L}]) keeps "John" from matching inside "Johnson".
        Regex("(?<![\\p{L}])($alternatives)\\.?(?![\\p{L}])", RegexOption.IGNORE_CASE)
    }

    /**
     * Scan [text] and return every reference found, in order of appearance.
     * Non-overlapping: once a reference consumes a span, scanning resumes after it.
     */
    fun detect(text: String): List<DetectedReference> {
        if (text.isEmpty()) return emptyList()

        val results = mutableListOf<DetectedReference>()
        var groupId = 0
        var searchFrom = 0

        while (searchFrom < text.length) {
            val bookMatch = bookPattern.find(text, searchFrom) ?: break
            val book = BookCatalog.find(bookMatch.groupValues[1])
            if (book == null) {
                searchFrom = bookMatch.range.last + 1
                continue
            }

            val tail = readNumericTail(text, bookMatch.range.last + 1, book)
            if (tail == null) {
                // A book name with no usable number after it. Single-chapter
                // books (Jude, Philemon, Obadiah, 2-3 John) are still worth
                // linking on their own.
                if (book.chapterCount == 1) {
                    results += DetectedReference(
                        reference = BibleReference.chapter(book.number, 1),
                        start = bookMatch.range.first,
                        end = bookMatch.range.last + 1,
                        matchedText = bookMatch.value,
                        groupId = groupId++
                    )
                }
                searchFrom = bookMatch.range.last + 1
                continue
            }

            val gid = groupId++
            tail.references.forEach { ref ->
                results += DetectedReference(
                    reference = ref,
                    start = bookMatch.range.first,
                    end = tail.endExclusive,
                    matchedText = text.substring(bookMatch.range.first, tail.endExclusive),
                    groupId = gid
                )
            }
            searchFrom = tail.endExclusive
        }
        return results
    }

    /** Convenience for a single reference typed on its own, e.g. a search box. */
    fun parseSingle(input: String): BibleReference? =
        detect(input).firstOrNull()?.reference

    // ---------------------------------------------------------------- internals

    private class Tail(val references: List<BibleReference>, val endExclusive: Int)

    /**
     * Reads the `3:16`, `5:1-12`, `23`, `8:28-30`, `1:1, 3` or `5:1-6:4` part
     * that follows a book token. Returns null if nothing numeric follows.
     */
    private fun readNumericTail(text: String, from: Int, book: Book): Tail? {
        var i = skipSpace(text, from)
        val firstNumber = readInt(text, i) ?: return null
        i = firstNumber.end

        val refs = mutableListOf<BibleReference>()
        var lastValidEnd: Int

        if (!peekIs(text, i, ':') && !peekIs(text, i, '.')) {
            // No verse separator: this is a whole-chapter reference like "Psalm 23".
            val chapter = firstNumber.value
            if (chapter < 1 || chapter > book.chapterCount) return null
            // Possible chapter range: "Psalm 23-24"
            val afterDash = tryReadRangeTail(text, i)
            return if (afterDash != null && afterDash.value in 1..book.chapterCount) {
                Tail(
                    listOf(
                        BibleReference(
                            book.number, chapter, BibleReference.WHOLE_CHAPTER,
                            afterDash.value, Int.MAX_VALUE
                        )
                    ),
                    afterDash.end
                )
            } else {
                Tail(listOf(BibleReference.chapter(book.number, chapter)), i)
            }
        }

        // We have `chapter:` — read the verse side.
        var chapter = firstNumber.value
        if (chapter < 1 || chapter > book.chapterCount) return null
        i += 1 // consume ':' or '.'
        i = skipSpace(text, i)

        val firstVerse = readInt(text, i) ?: return null
        i = firstVerse.end
        lastValidEnd = i

        var startChapter = chapter
        var startVerse = firstVerse.value
        var endChapter = chapter
        var endVerse = firstVerse.value

        // Optional range: "-12" or "-6:4"
        val rangeEnd = tryReadRangeTail(text, i)
        if (rangeEnd != null) {
            i = rangeEnd.end
            if (peekIs(text, i, ':') || peekIs(text, i, '.')) {
                // Cross-chapter: "5:1-6:4"
                val crossVerse = readInt(text, skipSpace(text, i + 1))
                if (crossVerse != null && rangeEnd.value in 1..book.chapterCount) {
                    endChapter = rangeEnd.value
                    endVerse = crossVerse.value
                    i = crossVerse.end
                }
            } else {
                endVerse = rangeEnd.value
            }
            lastValidEnd = i
        }

        refs += BibleReference(book.number, startChapter, startVerse, endChapter, endVerse)

        // Optional comma list: ", 3" / ", 5-7" — same chapter unless a new `c:v`.
        while (true) {
            var j = skipSpace(text, lastValidEnd)
            if (!peekIs(text, j, ',')) break
            j = skipSpace(text, j + 1)
            val n = readInt(text, j) ?: break
            j = n.end

            if (peekIs(text, j, ':') || peekIs(text, j, '.')) {
                val v = readInt(text, skipSpace(text, j + 1)) ?: break
                if (n.value !in 1..book.chapterCount) break
                chapter = n.value
                refs += BibleReference(book.number, chapter, v.value, chapter, v.value)
                j = v.end
            } else {
                var vEnd = n.value
                val r = tryReadRangeTail(text, j)
                if (r != null) { vEnd = r.value; j = r.end }
                refs += BibleReference(book.number, chapter, n.value, chapter, vEnd)
            }
            lastValidEnd = j
        }

        return Tail(refs, lastValidEnd)
    }

    private class IntRead(val value: Int, val end: Int)

    private fun readInt(text: String, from: Int): IntRead? {
        var i = from
        var value = 0
        var digits = 0
        while (i < text.length && text[i].isDigit() && digits < 4) {
            value = value * 10 + (text[i] - '0')
            i++; digits++
        }
        return if (digits == 0) null else IntRead(value, i)
    }

    /** Reads `-12` / `–12` / ` - 12` after the current position. */
    private fun tryReadRangeTail(text: String, from: Int): IntRead? {
        var i = skipSpace(text, from)
        if (i >= text.length) return null
        val c = text[i]
        if (c != '-' && c != '–' && c != '—') return null
        i = skipSpace(text, i + 1)
        return readInt(text, i)
    }

    private fun skipSpace(text: String, from: Int): Int {
        var i = from
        while (i < text.length && (text[i] == ' ' || text[i] == ' ')) i++
        return i
    }

    private fun peekIs(text: String, index: Int, ch: Char): Boolean =
        index < text.length && text[index] == ch
}
