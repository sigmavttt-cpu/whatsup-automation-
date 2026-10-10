package com.whatsup.automation.data.repository

import com.whatsup.automation.data.local.dao.VaultDao
import com.whatsup.automation.data.local.entity.VaultMediaEntity
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import com.whatsup.automation.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val vaultDao: VaultDao
) : VaultRepository {

    override fun getAllVaultItems(): Flow<List<VaultItem>> {
        return vaultDao.getAllVaultItems().map { list -> list.map { it.toDomain() } }
    }

    override fun getViewOnceItems(): Flow<List<VaultItem>> {
        return vaultDao.getViewOnceItems().map { list -> list.map { it.toDomain() } }
    }

    override fun getDeletedItems(): Flow<List<VaultItem>> {
        return vaultDao.getDeletedItems().map { list -> list.map { it.toDomain() } }
    }

    override fun getFavoriteItems(): Flow<List<VaultItem>> {
        return vaultDao.getFavoriteItems().map { list -> list.map { it.toDomain() } }
    }

    override fun getViewOnceCount(): Flow<Int> = vaultDao.getViewOnceCount()

    override fun getDeletedCount(): Flow<Int> = vaultDao.getDeletedCount()

    override suspend fun saveVaultItem(item: VaultItem) {
        vaultDao.insertItem(item.toEntity())
    }

    override suspend fun markMessageAsDeleted(id: String) {
        vaultDao.markAsDeleted(id)
    }

    override suspend fun toggleFavorite(id: String) {
        vaultDao.toggleFavorite(id)
    }

    override suspend fun deleteVaultItem(id: String) {
        val existing = vaultDao.getItemById(id)
        if (existing?.localFilePath != null) {
            try {
                val file = java.io.File(existing.localFilePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        vaultDao.deleteItem(id)
    }

    private fun VaultMediaEntity.toDomain(): VaultItem {
        return VaultItem(
            id = id,
            senderPhone = senderPhone,
            senderName = senderName,
            mediaType = VaultMediaType.fromString(mediaType),
            isViewOnce = isViewOnce,
            isDeleted = isDeleted,
            originalText = originalText,
            localFilePath = localFilePath,
            mimeType = mimeType,
            fileSizeBytes = fileSizeBytes,
            timestamp = timestamp,
            isFavorite = isFavorite
        )
    }

    private fun VaultItem.toEntity(): VaultMediaEntity {
        return VaultMediaEntity(
            id = id,
            senderPhone = senderPhone,
            senderName = senderName,
            mediaType = mediaType.name,
            isViewOnce = isViewOnce,
            isDeleted = isDeleted,
            originalText = originalText,
            localFilePath = localFilePath,
            mimeType = mimeType,
            fileSizeBytes = fileSizeBytes,
            timestamp = timestamp,
            isFavorite = isFavorite
        )
    }
}
