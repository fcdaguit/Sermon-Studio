package com.dlbc.sermonstudio.data.bible

import com.dlbc.sermonstudio.core.bible.BibleReference
import com.dlbc.sermonstudio.data.db.bible.BibleDao

/**
 * Offline provider backed by one SQLite module. This is the path that makes the
 * app genuinely offline-first: no network call is possible from here.
 */
class LocalModuleProvider(
    override val descriptor: TranslationDescriptor,
    private val dao: BibleDao
) : TranslationProvider {

    override val isOffline: Boolean = true

    override suspend fun passage(reference: BibleReference): List<Verse> {
        val endVerse = if (reference.endVerse == Int.MAX_VALUE) 999 else reference.endVerse
        val startVerse =
            if (reference.startVerse == BibleReference.WHOLE_CHAPTER) 1 else reference.startVerse
        return dao.span(
            book = reference.bookNumber,
            startChapter = reference.startChapter,
            startVerse = startVerse,
            endChapter = reference.endChapter,
            endVerse = endVerse
        ).map { it.toVerse() }
    }

    override suspend fun chapter(book: Int, chapter: Int): List<Verse> =
        dao.chapter(book, chapter).map { it.toVerse() }

    override suspend fun search(query: String, limit: Int): List<Verse> =
        dao.search(FtsQuery.sanitize(query), limit).map { it.toVerse() }

    private fun com.dlbc.sermonstudio.data.db.bible.VerseEntity.toVerse() =
        Verse(book, chapter, verse, text)
}

/**
 * FTS MATCH input is a query language, not a literal string. Unescaped user
 * input such as `faith "hope` or `love*` either throws or silently changes
 * meaning, so everything is quoted as phrases.
 */
object FtsQuery {
    fun sanitize(raw: String): String {
        val tokens = raw.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { it.replace("\"", "") }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return "\"\""
        // Each token quoted; last one gets a prefix wildcard for type-ahead.
        return tokens.mapIndexed { index, token ->
            if (index == tokens.lastIndex) "\"$token\"*" else "\"$token\""
        }.joinToString(" ")
    }
}
