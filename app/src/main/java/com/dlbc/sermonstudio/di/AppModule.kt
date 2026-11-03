package com.dlbc.sermonstudio.di

import android.content.Context
import androidx.room.Room
import com.dlbc.sermonstudio.data.db.sermon.BookmarkDao
import com.dlbc.sermonstudio.data.db.sermon.FolderDao
import com.dlbc.sermonstudio.data.db.sermon.NoteDao
import com.dlbc.sermonstudio.data.db.sermon.OutlineDao
import com.dlbc.sermonstudio.data.db.sermon.SermonDatabase
import com.dlbc.sermonstudio.data.db.sermon.TagDao
import com.dlbc.sermonstudio.domain.assistant.SermonAssistant
import com.dlbc.sermonstudio.domain.assistant.TemplateAssistant
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideSermonDatabase(@ApplicationContext context: Context): SermonDatabase =
        Room.databaseBuilder(context, SermonDatabase::class.java, SermonDatabase.NAME)
            // No fallbackToDestructiveMigration here, deliberately. If a
            // migration is missing the app should fail loudly in development
            // rather than quietly wipe a pastor's outlines in production.
            .build()

    @Provides fun outlineDao(db: SermonDatabase): OutlineDao = db.outlineDao()
    @Provides fun folderDao(db: SermonDatabase): FolderDao = db.folderDao()
    @Provides fun noteDao(db: SermonDatabase): NoteDao = db.noteDao()
    @Provides fun bookmarkDao(db: SermonDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun tagDao(db: SermonDatabase): TagDao = db.tagDao()

    @Provides
    @Singleton
    fun provideAppContext(@ApplicationContext context: Context): Context = context
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AssistantModule {
    /**
     * v1 binds the deterministic template assistant. When an on-device LLM
     * implementation exists, bind it here behind a runtime capability check
     * rather than replacing this binding outright — the template path stays as
     * the fallback for low-RAM devices.
     */
    @Binds
    @Singleton
    abstract fun bindAssistant(impl: TemplateAssistant): SermonAssistant
}
