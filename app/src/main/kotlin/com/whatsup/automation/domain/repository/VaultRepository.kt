package com.whatsup.automation.domain.repository

import com.whatsup.automation.domain.model.VaultItem
import kotlinx.coroutines.flow.Flow

/**
 * واجهة مستودع الخزينة السرية (Vault Repository Interface).
 */
interface VaultRepository {
    fun getAllVaultItems(): Flow<List<VaultItem>>
    fun getViewOnceItems(): Flow<List<VaultItem>>
    fun getDeletedItems(): Flow<List<VaultItem>>
    fun getFavoriteItems(): Flow<List<VaultItem>>
    fun getViewOnceCount(): Flow<Int>
    fun getDeletedCount(): Flow<Int>
    suspend fun saveVaultItem(item: VaultItem)
    suspend fun markMessageAsDeleted(id: String)
    suspend fun toggleFavorite(id: String)
    suspend fun deleteVaultItem(id: String)
}
