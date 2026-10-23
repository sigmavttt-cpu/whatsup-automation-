package com.whatsup.automation

import com.whatsup.automation.data.local.dao.VaultDao
import com.whatsup.automation.data.local.entity.VaultMediaEntity
import com.whatsup.automation.data.repository.VaultRepositoryImpl
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class ViewOnceVaultResilienceTest {

    private lateinit var fakeVaultDao: FakeVaultDao
    private lateinit var vaultRepository: VaultRepositoryImpl

    class FakeVaultDao : VaultDao {
        val database = mutableMapOf<String, VaultMediaEntity>()

        override fun getAllVaultItems(): Flow<List<VaultMediaEntity>> {
            return flowOf(database.values.toList().sortedByDescending { it.timestamp })
        }

        override fun getViewOnceItems(): Flow<List<VaultMediaEntity>> {
            return flowOf(database.values.filter { it.isViewOnce }.sortedByDescending { it.timestamp })
        }

        override fun getDeletedItems(): Flow<List<VaultMediaEntity>> {
            return flowOf(database.values.filter { it.isDeleted }.sortedByDescending { it.timestamp })
        }

        override fun getFavoriteItems(): Flow<List<VaultMediaEntity>> {
            return flowOf(database.values.filter { it.isFavorite }.sortedByDescending { it.timestamp })
        }

        override suspend fun getItemById(id: String): VaultMediaEntity? {
            return database[id]
        }

        override suspend fun insertItem(item: VaultMediaEntity) {
            database[item.id] = item
        }

        override suspend fun updateItem(item: VaultMediaEntity) {
            database[item.id] = item
        }

        override suspend fun markAsDeleted(id: String) {
            val existing = database[id]
            if (existing != null) {
                database[id] = existing.copy(isDeleted = true)
            }
        }

        override suspend fun toggleFavorite(id: String) {
            val existing = database[id]
            if (existing != null) {
                database[id] = existing.copy(isFavorite = !existing.isFavorite)
            }
        }

        override suspend fun deleteItem(id: String) {
            database.remove(id)
        }

        override fun getViewOnceCount(): Flow<Int> {
            return flowOf(database.values.count { it.isViewOnce })
        }

        override fun getDeletedCount(): Flow<Int> {
            return flowOf(database.values.count { it.isDeleted })
        }
    }

    @Before
    fun setUp() {
        fakeVaultDao = FakeVaultDao()
        vaultRepository = VaultRepositoryImpl(fakeVaultDao)
    }

    @Test
    fun `saveVaultItem preserves View-Once tag and correctly indexes item`() = runBlocking {
        val viewOnceItem = VaultItem(
            id = "MSG_VO_001",
            senderPhone = "967771234567",
            senderName = "Test Contact",
            mediaType = VaultMediaType.IMAGE,
            isViewOnce = true,
            isDeleted = false,
            originalText = "صورة لمرة واحدة تجريبية",
            localFilePath = "/data/user/0/com.whatsup.automation/files/vault_media/viewonce_MSG_VO_001.jpg",
            mimeType = "image/jpeg",
            fileSizeBytes = 2048L,
            timestamp = 1700000000L
        )

        vaultRepository.saveVaultItem(viewOnceItem)

        val allItems = vaultRepository.getAllVaultItems().first()
        val viewOnceItems = vaultRepository.getViewOnceItems().first()
        val viewOnceCount = vaultRepository.getViewOnceCount().first()

        assertEquals(1, allItems.size)
        assertEquals(1, viewOnceItems.size)
        assertEquals(1, viewOnceCount)
        assertTrue(viewOnceItems.first().isViewOnce)
        assertEquals("MSG_VO_001", viewOnceItems.first().id)
    }

    @Test
    fun `Regular non-viewOnce messages are NOT returned in getViewOnceItems`() = runBlocking {
        val regularItem = VaultItem(
            id = "MSG_REG_002",
            senderPhone = "967771234567",
            senderName = "Regular Sender",
            mediaType = VaultMediaType.IMAGE,
            isViewOnce = false,
            isDeleted = false,
            originalText = "صورة عادية",
            localFilePath = null,
            mimeType = "image/jpeg",
            fileSizeBytes = 1024L,
            timestamp = 1700000010L
        )

        vaultRepository.saveVaultItem(regularItem)

        val allItems = vaultRepository.getAllVaultItems().first()
        val viewOnceItems = vaultRepository.getViewOnceItems().first()
        val viewOnceCount = vaultRepository.getViewOnceCount().first()

        assertEquals(1, allItems.size)
        assertEquals(0, viewOnceItems.size)
        assertEquals(0, viewOnceCount)
    }

    @Test
    fun `Message revocation preserves View-Once flag and marks as deleted simultaneously`() = runBlocking {
        // Step 1: View-Once image captured
        val voItem = VaultItem(
            id = "MSG_VO_REVOKED_003",
            senderPhone = "967779876543",
            senderName = "Revoker",
            mediaType = VaultMediaType.IMAGE,
            isViewOnce = true,
            isDeleted = false,
            originalText = "صورة ستحترق ثم تحذف",
            localFilePath = "/vault_media/viewonce_003.jpg",
            mimeType = "image/jpeg",
            fileSizeBytes = 4096L,
            timestamp = 1700000020L
        )
        vaultRepository.saveVaultItem(voItem)

        // Step 2: Sender deletes message for everyone (MESSAGE_REVOKED)
        vaultRepository.markMessageAsDeleted("MSG_VO_REVOKED_003")

        val stored = fakeVaultDao.getItemById("MSG_VO_REVOKED_003")
        assertNotNull(stored)
        assertTrue("Item must retain isViewOnce=true after deletion", stored!!.isViewOnce)
        assertTrue("Item must also be flagged isDeleted=true", stored.isDeleted)

        val voItems = vaultRepository.getViewOnceItems().first()
        val delItems = vaultRepository.getDeletedItems().first()
        assertEquals(1, voItems.size)
        assertEquals(1, delItems.size)
    }

    @Test
    fun `Toggle favorite and delete operations function correctly on View-Once media`() = runBlocking {
        val tempFile = File.createTempFile("test_vo_media", ".jpg")
        tempFile.writeText("DUMMY_IMAGE_BYTES")
        assertTrue(tempFile.exists())

        val voItem = VaultItem(
            id = "MSG_VO_FAV_004",
            senderPhone = "967770001111",
            senderName = "Fav User",
            mediaType = VaultMediaType.IMAGE,
            isViewOnce = true,
            isDeleted = false,
            originalText = "صورة مفضلة",
            localFilePath = tempFile.absolutePath,
            mimeType = "image/jpeg",
            fileSizeBytes = tempFile.length(),
            timestamp = 1700000030L
        )
        vaultRepository.saveVaultItem(voItem)

        // Favorite toggle
        vaultRepository.toggleFavorite("MSG_VO_FAV_004")
        val favs = vaultRepository.getFavoriteItems().first()
        assertEquals(1, favs.size)
        assertTrue(favs.first().isFavorite)

        // Delete cleans up disk file and database record
        vaultRepository.deleteVaultItem("MSG_VO_FAV_004")
        assertNull(fakeVaultDao.getItemById("MSG_VO_FAV_004"))
        assertFalse("Physical file must be purged from disk on vault delete", tempFile.exists())
    }
}
