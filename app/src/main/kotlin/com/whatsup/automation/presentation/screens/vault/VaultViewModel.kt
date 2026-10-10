package com.whatsup.automation.presentation.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * التبويبات الفرعية للخزينة السرية.
 */
enum class VaultSubTab {
    VIEW_ONCE,
    ANTI_DELETE,
    FAVORITES
}

/**
 * حالة واجهة المستخدم للخزينة السرية.
 */
data class VaultUiState(
    val activeTab: VaultSubTab = VaultSubTab.VIEW_ONCE,
    val searchQuery: String = "",
    val items: List<VaultItem> = emptyList(),
    val viewOnceCount: Int = 0,
    val deletedCount: Int = 0,
    val selectedItem: VaultItem? = null
)

/**
 * مدير حالة الخزينة السرية (Vault ViewModel).
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository
) : ViewModel() {

    private val _activeTab = MutableStateFlow(VaultSubTab.VIEW_ONCE)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedItem = MutableStateFlow<VaultItem?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val selectedItem: StateFlow<VaultItem?> = _selectedItem.asStateFlow()

    val uiState: StateFlow<VaultUiState> = combine(
        _activeTab,
        _searchQuery,
        vaultRepository.getAllVaultItems(),
        vaultRepository.getViewOnceCount(),
        vaultRepository.getDeletedCount()
    ) { tab, query, allItems, voCount, delCount ->
        val filteredByTab = when (tab) {
            VaultSubTab.VIEW_ONCE -> allItems.filter { it.isViewOnce }
            VaultSubTab.ANTI_DELETE -> allItems.filter { it.isDeleted }
            VaultSubTab.FAVORITES -> allItems.filter { it.isFavorite }
        }

        val filteredByQuery = if (query.isBlank()) {
            filteredByTab
        } else {
            val q = query.trim().lowercase()
            filteredByTab.filter {
                it.senderName.lowercase().contains(q) ||
                it.senderPhone.contains(q) ||
                (it.originalText?.lowercase()?.contains(q) == true)
            }
        }

        VaultUiState(
            activeTab = tab,
            searchQuery = query,
            items = filteredByQuery,
            viewOnceCount = voCount,
            deletedCount = delCount,
            selectedItem = _selectedItem.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState()
    )

    fun setActiveTab(tab: VaultSubTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectItem(item: VaultItem?) {
        _selectedItem.value = item
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            vaultRepository.toggleFavorite(id)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            vaultRepository.deleteVaultItem(id)
            if (_selectedItem.value?.id == id) {
                _selectedItem.value = null
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(450)
            _isRefreshing.value = false
        }
    }
}
