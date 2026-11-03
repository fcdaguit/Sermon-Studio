package com.dlbc.sermonstudio.data.db.sermon

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OutlineDao {

    @Query("SELECT * FROM outline WHERE archived = 0 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<OutlineEntity>>

    @Query("SELECT * FROM outline WHERE archived = 1 ORDER BY updatedAt DESC")
    fun observeArchived(): Flow<List<OutlineEntity>>

    @Query("SELECT * FROM outline WHERE folderId = :folderId AND archived = 0 ORDER BY updatedAt DESC")
    fun observeInFolder(folderId: Long?): Flow<List<OutlineEntity>>

    @Query("SELECT * FROM outline WHERE id = :id")
    suspend fun byId(id: Long): OutlineEntity?

    @Query("SELECT * FROM outline WHERE id = :id")
    fun observeById(id: Long): Flow<OutlineEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(outline: OutlineEntity): Long

    @Update
    suspend fun update(outline: OutlineEntity)

    @Query("UPDATE outline SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long = System.currentTimeMillis())

    /** Hard delete. Only reachable from the Trash screen after an explicit confirm. */
    @Query("DELETE FROM outline WHERE id = :id")
    suspend fun deleteForever(id: Long)

    @Transaction
    suspend fun duplicate(id: Long, titleSuffix: String): Long? {
        val original = byId(id) ?: return null
        return insert(
            original.copy(
                id = 0,
                title = original.title + titleSuffix,
                preachedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @Query(
        """
        SELECT o.* FROM outline o
        JOIN outline_fts f ON f.docid = o.id
        WHERE outline_fts MATCH :query AND o.archived = 0
        ORDER BY o.updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 100): List<OutlineEntity>

    @Query("SELECT * FROM outline")
    suspend fun allForBackup(): List<OutlineEntity>
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folder ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<FolderEntity>>

    @Insert suspend fun insert(folder: FolderEntity): Long
    @Update suspend fun update(folder: FolderEntity)

    @Query("DELETE FROM folder WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM folder")
    suspend fun allForBackup(): List<FolderEntity>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM note ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE verseKey = :verseKey ORDER BY updatedAt DESC")
    fun observeForVerse(verseKey: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun byId(id: Long): NoteEntity?

    @Insert suspend fun insert(note: NoteEntity): Long
    @Update suspend fun update(note: NoteEntity)

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT n.* FROM note n
        JOIN note_fts f ON f.docid = n.id
        WHERE note_fts MATCH :query
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int = 100): List<NoteEntity>

    @Query("SELECT * FROM note")
    suspend fun allForBackup(): List<NoteEntity>
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmark ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmark WHERE verseKey = :verseKey LIMIT 1")
    suspend fun byVerseKey(verseKey: String): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmark WHERE verseKey = :verseKey")
    suspend fun deleteByVerseKey(verseKey: String)

    @Query("SELECT * FROM bookmark")
    suspend fun allForBackup(): List<BookmarkEntity>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag ORDER BY name")
    fun observeAll(): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Query("SELECT * FROM tag WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun link(link: TagLinkEntity)

    @Query("DELETE FROM tag_link WHERE tagId = :tagId AND entityType = :type AND entityId = :id")
    suspend fun unlink(tagId: Long, type: Int, id: Long)

    @Query(
        """
        SELECT t.* FROM tag t
        JOIN tag_link l ON l.tagId = t.id
        WHERE l.entityType = :type AND l.entityId = :id
        ORDER BY t.name
        """
    )
    fun observeTagsFor(type: Int, id: Long): Flow<List<TagEntity>>

    /**
     * Removes tag links whose target row no longer exists. Needed because the
     * polymorphic join cannot declare a foreign key.
     */
    @Query(
        """
        DELETE FROM tag_link WHERE
          (entityType = 0 AND entityId NOT IN (SELECT id FROM outline)) OR
          (entityType = 1 AND entityId NOT IN (SELECT id FROM note)) OR
          (entityType = 2 AND entityId NOT IN (SELECT id FROM bookmark))
        """
    )
    suspend fun purgeOrphans()

    @Query("SELECT * FROM tag")
    suspend fun allTagsForBackup(): List<TagEntity>

    @Query("SELECT * FROM tag_link")
    suspend fun allLinksForBackup(): List<TagLinkEntity>
}
