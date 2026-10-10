package com.whatsup.automation

import android.content.Context
import com.whatsup.automation.data.local.dao.StatusDao
import com.whatsup.automation.data.local.entity.StatusEntity
import com.whatsup.automation.data.repository.StatusRepositoryImpl
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.domain.model.StatusMediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.time.Instant

class StatusAutomationShieldTest {

    private lateinit var fakeStatusDao: FakeStatusDao
    private lateinit var statusRepository: StatusRepositoryImpl
    private lateinit var mockContext: Context

    class FakeStatusDao : StatusDao {
        val database = mutableMapOf<String, StatusEntity>()

        override fun getAllStatuses(): Flow<List<StatusEntity>> {
            return flowOf(database.values.toList().sortedByDescending { it.timestamp })
        }

        override suspend fun getStatusById(statusId: String): StatusEntity? {
            return database[statusId]
        }

        override suspend fun insertStatus(status: StatusEntity) {
            database[status.id] = status
        }

        override suspend fun markAsViewed(statusId: String, timestamp: Long) {
            val existing = database[statusId]
            if (existing != null) {
                database[statusId] = existing.copy(isViewed = true, viewedAt = timestamp)
            }
        }

        override suspend fun markStatusViewed(statusId: String) {
            markAsViewed(statusId, System.currentTimeMillis())
        }

        override suspend fun updateReaction(statusId: String, emoji: String, timestamp: Long) {
            val existing = database[statusId]
            if (existing != null) {
                database[statusId] = existing.copy(
                    isReacted = true,
                    reactionEmoji = emoji,
                    reactionSentAt = timestamp,
                    isViewed = true,
                    viewedAt = existing.viewedAt ?: timestamp
                )
            }
        }

        override suspend fun reactToStatus(statusId: String, emoji: String) {
            updateReaction(statusId, emoji, System.currentTimeMillis())
        }

        override suspend fun deleteExpiredStatuses(olderThanTimestamp: Long) {
            database.entries.removeIf { it.value.timestamp < olderThanTimestamp }
        }

        override suspend fun updateFavorite(statusId: String, isFavorite: Boolean) {
            val existing = database[statusId]
            if (existing != null) {
                database[statusId] = existing.copy(isFavorite = isFavorite)
            }
        }

        override suspend fun updateMuted(statusId: String, isMuted: Boolean) {
            val existing = database[statusId]
            if (existing != null) {
                database[statusId] = existing.copy(isMuted = isMuted)
            }
        }

        override suspend fun updateFavoriteBySender(senderPhone: String, isFavorite: Boolean) {
            database.values.filter { it.senderPhone == senderPhone }.forEach {
                database[it.id] = it.copy(isFavorite = isFavorite)
            }
        }

        override suspend fun updateMutedBySender(senderPhone: String, isMuted: Boolean) {
            database.values.filter { it.senderPhone == senderPhone }.forEach {
                database[it.id] = it.copy(isMuted = isMuted)
            }
        }

        override suspend fun markAsDeleted(statusId: String, deletedAt: Long) {
            val existing = database[statusId]
            if (existing != null) {
                database[statusId] = existing.copy(isDeleted = true, deletedAt = deletedAt)
            }
        }

        override suspend fun deleteExpiredDeletedStatuses(olderThanTimestamp: Long) {
            database.entries.removeIf { it.value.isDeleted && (it.value.deletedAt ?: 0L) < olderThanTimestamp }
        }
    }

    @Before
    fun setup() {
        mockContext = mock(Context::class.java)
        val mockPrefs = mock(android.content.SharedPreferences::class.java)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.getBoolean(anyString(), anyBoolean())).thenReturn(true)
        `when`(mockPrefs.getString(anyString(), anyString())).thenReturn("💚")

        fakeStatusDao = FakeStatusDao()
        statusRepository = StatusRepositoryImpl(fakeStatusDao, mockContext)
    }

    @Test
    fun testInsertStatusPreservesPreviousViewedAndReactedState() = runBlocking {
        val statusId = "STATUS_1001"
        val initialStory = StatusStory(
            id = statusId,
            senderPhone = "+967771234567",
            senderName = "H",
            mediaType = StatusMediaType.TEXT,
            textContent = "Hello world",
            timestamp = Instant.now()
        )

        // 1. Initial insert
        statusRepository.insertStatus(initialStory)
        assertFalse(statusRepository.getStatusById(statusId)!!.isViewed)
        assertFalse(statusRepository.getStatusById(statusId)!!.isReacted)

        // 2. Mark viewed and reacted
        statusRepository.markStatusViewed(statusId)
        statusRepository.reactToStatus(statusId, "💚")
        assertTrue(statusRepository.getStatusById(statusId)!!.isViewed)
        assertTrue(statusRepository.getStatusById(statusId)!!.isReacted)
        assertEquals("💚", statusRepository.getStatusById(statusId)!!.reactionEmoji)

        // 3. Re-inserting the same status with default (false) flags MUST NOT overwrite or reset viewed/reacted states
        val reEmittedStory = StatusStory(
            id = statusId,
            senderPhone = "+967771234567",
            senderName = "H",
            mediaType = StatusMediaType.TEXT,
            textContent = "Hello world",
            timestamp = Instant.now(),
            isViewed = false,
            isReacted = false
        )
        statusRepository.insertStatus(reEmittedStory)

        val retrieved = statusRepository.getStatusById(statusId)
        assertNotNull(retrieved)
        assertTrue("isViewed must remain TRUE after re-insertion", retrieved!!.isViewed)
        assertTrue("isReacted must remain TRUE after re-insertion", retrieved.isReacted)
        assertEquals("💚", retrieved.reactionEmoji)
    }

    @Test
    fun testUnregisteredContactStatusProcessingGuard() {
        val registeredPhone = "+967771234567"
        val unregisteredPhone = "+967779999999"

        val registeredContacts = mapOf(registeredPhone to "علي سعيد")

        fun isSenderRegistered(phone: String): Boolean {
            return registeredContacts.containsKey(phone)
        }

        assertTrue(isSenderRegistered(registeredPhone))
        assertFalse(isSenderRegistered(unregisteredPhone))
    }
}
