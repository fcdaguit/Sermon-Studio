package com.dlbc.sermonstudio.data.db.bible

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(
    entities = [VerseEntity::class, VerseFts::class, ModuleInfoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class BibleModuleDatabase : RoomDatabase() {
    abstract fun bibleDao(): BibleDao

    companion object {
        /**
         * Opens a translation module.
         *
         * [assetPath] is used for modules bundled in the APK (KJV ships this
         * way). [externalFile] is used for modules the user downloaded or
         * imported; it wins when both are supplied.
         *
         * Both paths open the database READ-ONLY in practice — nothing in the
         * app writes verse text — but Room is not told so, because
         * createFromAsset needs write access on first open to stamp its
         * identity hash.
         */
        fun open(
            context: Context,
            dbName: String,
            assetPath: String? = null,
            externalFile: File? = null
        ): BibleModuleDatabase {
            val builder = when {
                externalFile != null ->
                    Room.databaseBuilder(context, BibleModuleDatabase::class.java, dbName)
                        .createFromFile(externalFile)
                assetPath != null ->
                    Room.databaseBuilder(context, BibleModuleDatabase::class.java, dbName)
                        .createFromAsset(assetPath)
                else ->
                    Room.databaseBuilder(context, BibleModuleDatabase::class.java, dbName)
            }
            return builder
                // A corrupt or stale module should be re-fetched, not crash the
                // app — verse text is replaceable, user data is not.
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
