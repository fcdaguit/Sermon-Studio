package com.dlbc.sermonstudio.core.bible

/**
 * A resolved, normalised scripture reference.
 *
 * The model is a *span*: everything from ([startChapter], [startVerse]) to
 * ([endChapter], [endVerse]) inclusive. This single shape covers all the forms
 * the spec asks for:
 *
 *  - `John 3:16`        -> 3:16 .. 3:16
 *  - `Matthew 5:1-12`   -> 5:1  .. 5:12
 *  - `Psalm 23`         -> 23:ALL (startVerse = [WHOLE_CHAPTER])
 *  - `Matthew 5:1-6:4`  -> 5:1  .. 6:4   (cross-chapter)
 *
 * A comma list such as `Genesis 1:1, 3` is parsed into *two* references sharing
 * one [sourceRange] group id, not one discontinuous reference. Discontinuous
 * spans are a display concern, not a storage concern.
 */
data class BibleReference(
    val bookNumber: Int,
    val startChapter: Int,
    val startVerse: Int,
    val endChapter: Int,
    val endVerse: Int
) {
    val isWholeChapter: Boolean get() = startVerse == WHOLE_CHAPTER

    val isSingleVerse: Boolean
        get() = !isWholeChapter && startChapter == endChapter && startVerse == endVerse

    val bookName: String get() = BookCatalog.name(bookNumber)

    /** Stable, locale-independent key: `43.3.16-43.3.16`. Safe as a map key or tag. */
    fun key(): String = "$bookNumber.$startChapter.$startVerse-$endChapter.$endVerse"

    /** Human display form in English, e.g. `Matthew 5:1-12`. */
    fun display(): String = buildString {
        append(bookName)
        append(' ')
        append(startChapter)
        if (isWholeChapter) return@buildString
        append(':')
        append(startVerse)
        when {
            startChapter == endChapter && startVerse == endVerse -> Unit
            startChapter == endChapter -> append("-").append(endVerse)
            else -> append("-").append(endChapter).append(':').append(endVerse)
        }
    }

    companion object {
        /** Sentinel for "the whole chapter", used in [startVerse]. */
        const val WHOLE_CHAPTER = 0

        fun verse(book: Int, chapter: Int, verse: Int) =
            BibleReference(book, chapter, verse, chapter, verse)

        fun chapter(book: Int, chapter: Int) =
            BibleReference(book, chapter, WHOLE_CHAPTER, chapter, Int.MAX_VALUE)
    }
}

/** A reference together with where it was found in the source text. */
data class DetectedReference(
    val reference: BibleReference,
    /** Inclusive start index into the scanned string. */
    val start: Int,
    /** Exclusive end index into the scanned string. */
    val end: Int,
    /** The literal text matched, so the editor can keep the author's own wording. */
    val matchedText: String,
    /**
     * References produced from one comma list (`Gen 1:1, 3`) share a group id, so
     * the UI can render them as one chip if it wants to.
     */
    val groupId: Int
)
