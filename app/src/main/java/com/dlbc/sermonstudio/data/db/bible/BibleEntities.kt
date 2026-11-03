package com.dlbc.sermonstudio.data.db.bible

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per verse, per translation module.
 *
 * Each translation lives in its OWN database file (see [BibleModuleDatabase]),
 * so there is no translation column here — the file identity is the translation.
 * That keeps install/remove of a module a single file delete, and keeps the FTS
 * index per-translation, which is what you want for ranking.
 */
@Entity(
    tableName = "verse",
    indices = [Index(value = ["book", "chapter", "verse"], unique = true)]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "book") val book: Int,
    @ColumnInfo(name = "chapter") val chapter: Int,
    @ColumnInfo(name = "verse") val verse: Int,
    @ColumnInfo(name = "text") val text: String
)

/**
 * Full-text index over [VerseEntity].
 *
 * IMPORTANT — FTS4, not FTS5.
 * Room ships `@Fts3` and `@Fts4` annotations only; there is no `@Fts5`
 * annotation. FTS5 is reachable only by writing the virtual-table DDL by hand
 * in a migration or database callback, and FTS5 availability also depends on
 * the device's bundled SQLite build, which varies across the minSdk 26..34
 * range this app targets. FTS4 is supported everywhere in that range and gives
 * us the prefix/phrase queries the search screen needs.
 *
 * If you later decide you need FTS5 ranking (bm25()), do it as a hand-written
 * virtual table in the asset-DB build script rather than through Room.
 */
@Fts4(contentEntity = VerseEntity::class)
@Entity(tableName = "verse_fts")
data class VerseFts(
    @ColumnInfo(name = "text") val text: String
)

/** Metadata row written once by the asset build script. */
@Entity(tableName = "module_info")
data class ModuleInfoEntity(
    @PrimaryKey val id: Int = 1,
    val code: String,
    val name: String,
    val language: String,
    val copyright: String,
    val verseCount: Int,
    val schemaVersion: Int
)
