package com.whatsup.automation.domain.model

import java.util.Locale

/**
 * أنواع وسائط الخزينة السرية.
 */
enum class VaultMediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    TEXT,
    DOCUMENT;

    companion object {
        fun fromString(value: String): VaultMediaType = try {
            valueOf(value.uppercase())
        } catch (_: Exception) {
            IMAGE
        }
    }
}

/**
 * نموذج الدومين لعنصر الخزينة السرية (وسائط العرض لمرة واحدة والمحذوفات).
 */
data class VaultItem(
    val id: String,
    val senderPhone: String,
    val senderName: String,
    val mediaType: VaultMediaType,
    val isViewOnce: Boolean,
    val isDeleted: Boolean,
    val originalText: String? = null,
    val localFilePath: String? = null,
    val mimeType: String? = null,
    val fileSizeBytes: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    fun getFormattedSize(): String {
        val mb = fileSizeBytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = fileSizeBytes / 1024.0
            String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
