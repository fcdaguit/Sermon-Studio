package com.dlbc.sermonstudio.data.db.bible

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BibleDao {

    @Query("SELECT * FROM module_info WHERE id = 1")
    suspend fun moduleInfo(): ModuleInfoEntity?

    @Query("SELECT * FROM verse WHERE book = :book AND chapter = :chapter ORDER BY verse")
    suspend fun chapter(book: Int, chapter: Int): List<VerseEntity>

    @Query("SELECT * FROM verse WHERE book = :book AND chapter = :chapter ORDER BY verse")
    fun chapterFlow(book: Int, chapter: Int): Flow<List<VerseEntity>>

    /**
     * Span query covering single verses, ranges, and cross-chapter ranges in one
     * statement. The composite comparison `(chapter, verse)` is expressed
     * arithmetically because SQLite has no row-value comparison before 3.15 on
     * every device in range.
     */
    @Query(
        """
        SELECT * FROM verse
        WHERE book = :book
          AND (chapter * 1000 + verse) >= (:startChapter * 1000 + :startVerse)
          AND (chapter * 1000 + verse) <= (:endChapter * 1000 + :endVerse)
        ORDER BY chapter, verse
        """
    )
    suspend fun span(
        book: Int,
        startChapter: Int,
        startVerse: Int,
        endChapter: Int,
        endVerse: Int
    ): List<VerseEntity>

    @Query(
        """
        SELECT v.* FROM verse v
        JOIN verse_fts f ON f.docid = v.id
        WHERE verse_fts MATCH :query
        ORDER BY v.book, v.chapter, v.verse
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 200): List<VerseEntity>

    @Query("SELECT COUNT(*) FROM verse")
    suspend fun verseCount(): Int
}
