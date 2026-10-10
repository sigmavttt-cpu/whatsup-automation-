package com.whatsup.automation.data.local.dao

import androidx.room.*
import com.whatsup.automation.data.local.entity.OutboxMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * واجهة الاستعلام لطابور الإرسال المجدول أوفلاين.
 */
@Dao
interface OutboxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: OutboxMessageEntity): Long

    @Update
    suspend fun updateMessage(message: OutboxMessageEntity)

    @Query("SELECT * FROM outbox_messages WHERE status = 'PENDING' AND nextRetryTimestamp <= :currentTime ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingMessages(currentTime: Long = System.currentTimeMillis(), limit: Int = 10): List<OutboxMessageEntity>

    @Query("SELECT COUNT(*) FROM outbox_messages WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM outbox_messages WHERE status = 'SENT' AND createdAt < :thresholdTime")
    suspend fun purgeSentMessages(thresholdTime: Long)

    @Query("DELETE FROM outbox_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE outbox_messages SET status = 'FAILED', lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, error: String)

    @Query("UPDATE outbox_messages SET status = 'SENT' WHERE id = :id")
    suspend fun markSent(id: Long)
}
