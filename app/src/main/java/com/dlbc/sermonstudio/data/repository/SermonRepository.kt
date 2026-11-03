package com.dlbc.sermonstudio.data.repository

import com.dlbc.sermonstudio.core.text.RichDocument
import com.dlbc.sermonstudio.data.db.sermon.FolderDao
import com.dlbc.sermonstudio.data.db.sermon.OutlineDao
import com.dlbc.sermonstudio.data.db.sermon.OutlineEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SermonRepository @Inject constructor(
    private val outlineDao: OutlineDao,
    private val folderDao: FolderDao
) {
    fun observeActive(): Flow<List<OutlineEntity>> = outlineDao.observeActive()
    fun observeArchived(): Flow<List<OutlineEntity>> = outlineDao.observeArchived()
    fun observeFolders() = folderDao.observeAll()
    fun observe(id: Long) = outlineDao.observeById(id)

    suspend fun create(title: String, folderId: Long? = null): Long {
        val doc = RichDocument()
        return outlineDao.insert(
            OutlineEntity(
                title = title,
                body = RichDocument.encode(doc),
                plainText = "",
                folderId = folderId
            )
        )
    }

    /**
     * Saves the body and keeps [OutlineEntity.plainText] in sync.
     *
     * The projection is recomputed here rather than in the DAO so there is
     * exactly one place where body and plainText can drift apart. If they
     * drift, FTS silently returns stale results — a bug that is hard to notice
     * and easy to prevent.
     */
    suspend fun saveBody(id: Long, doc: RichDocument, title: String? = null) {
        val existing = outlineDao.byId(id) ?: return
        outlineDao.update(
            existing.copy(
                title = title ?: existing.title,
                body = RichDocument.encode(doc),
                plainText = doc.toPlainText(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun archive(id: Long) = outlineDao.setArchived(id, true)
    suspend fun restore(id: Long) = outlineDao.setArchived(id, false)
    suspend fun duplicate(id: Long) = outlineDao.duplicate(id, " (copy)")
    suspend fun deleteForever(id: Long) = outlineDao.deleteForever(id)
    suspend fun search(query: String) = outlineDao.search(query)
    suspend fun load(id: Long) = outlineDao.byId(id)
}
