package com.whatsup.automation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

/**
 * كينونة رسالة طابور الإرسال المجدول أوفلاين (Outbox Queue).
 * تضمن عدم ضياع أي رسالة في حال انقطاع الإنترنت أو المقبس وإعادة إرسالها تلقائياً.
 */
@Entity(
    tableName = "outbox_messages",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class OutboxMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recipient: String,
    val messageText: String,
    val mediaUri: String? = null,
    val status: String = "PENDING", // PENDING, SENDING, SENT, FAILED
    val retryCount: Int = 0,
    val nextRetryTimestamp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val lastError: String? = null
)
