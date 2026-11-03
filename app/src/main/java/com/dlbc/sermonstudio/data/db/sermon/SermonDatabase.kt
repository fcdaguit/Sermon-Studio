package com.dlbc.sermonstudio.data.db.sermon

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        OutlineEntity::class, OutlineFts::class,
        FolderEntity::class,
        NoteEntity::class, NoteFts::class,
        BookmarkEntity::class,
        TagEntity::class, TagLinkEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SermonDatabase : RoomDatabase() {
    abstract fun outlineDao(): OutlineDao
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tagDao(): TagDao

    companion object {
        const val NAME = "sermon_studio.db"

        /**
         * Migration policy for THIS database: never destructive.
         * Add a Migration object for every version bump and register it in
         * DatabaseModule. Losing a user's outlines to a schema change is the
         * one failure this app cannot recover from.
         *
         * Schema JSON is exported to app/schemas/ — commit it, and diff it in
         * code review when the version changes.
         */
    }
}

/** Discriminator values for [TagLinkEntity.entityType]. */
object TaggedType {
    const val OUTLINE = 0
    const val NOTE = 1
    const val BOOKMARK = 2
}
