package com.dlbc.sermonstudio.data.db.sermon

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * User data. This database is the thing that must never be lost — verse text
 * is replaceable, a pastor's outlines are not. Every destructive path in the
 * app writes a backup first (see BackupManager).
 */

@Entity(
    tableName = "folder",
    foreignKeys = [ForeignKey(
        entity = FolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["parentId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("parentId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Null = top level. Nesting is unbounded; the UI flattens past depth 5. */
    val parentId: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "outline",
    foreignKeys = [ForeignKey(
        entity = FolderEntity::class,
        parentColumns = ["id"],
        childColumns = ["folderId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [Index("folderId"), Index("archived"), Index("updatedAt")]
)
data class OutlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** Serialised rich text. See core/text/RichDocument.kt for the format. */
    val body: String,
    /** Plain-text projection of [body], kept in sync for FTS and previews. */
    @ColumnInfo(name = "plainText") val plainText: String,
    val folderId: Long? = null,
    /** Primary preaching text, stored as a BibleReference key (e.g. "43.3.16-43.3.16"). */
    val mainTextKey: String? = null,
    val preachedAt: Long? = null,
    /**
     * Archive instead of delete. Nothing in the UI issues a hard DELETE on an
     * outline; "delete" sets this flag and the Trash screen is the only place
     * a real delete can happen, after a confirm.
     */
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Fts4(contentEntity = OutlineEntity::class)
@Entity(tableName = "outline_fts")
data class OutlineFts(
    val title: String,
    @ColumnInfo(name = "plainText") val plainText: String
)

@Entity(tableName = "tag", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB int. Null = use the theme's default chip colour. */
    val color: Int? = null
)

/**
 * One cross-entity tag join table, as the spec asks: tags apply to outlines,
 * notes and bookmarks alike. [entityType] discriminates; [entityId] is the row
 * id within that type.
 *
 * Trade-off: a polymorphic join cannot carry a real foreign key, so orphan rows
 * are possible. TagRepository.purgeOrphans() is run on app start to clean up.
 */
@Entity(
    tableName = "tag_link",
    primaryKeys = ["tagId", "entityType", "entityId"],
    indices = [Index("entityType", "entityId")]
)
data class TagLinkEntity(
    val tagId: Long,
    /** 0 = outline, 1 = note, 2 = bookmark. See TaggedType. */
    val entityType: Int,
    val entityId: Long
)

@Entity(tableName = "note", indices = [Index("verseKey"), Index("updatedAt")])
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    /** Null for a standalone commentary note; set when anchored to scripture. */
    val verseKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "note_fts")
data class NoteFts(val title: String, val body: String)

@Entity(tableName = "bookmark", indices = [Index(value = ["verseKey"], unique = true)])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val verseKey: String,
    /** Translation the bookmark was made in, for display. */
    val translationCode: String,
    val color: Int? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
