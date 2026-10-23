package com.whatsup.automation.presentation.screens.notifications

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.domain.model.AppNotification
import com.whatsup.automation.domain.model.NotificationType
import com.whatsup.automation.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class NotificationFilter(val title: String, val icon: ImageVector, val targetType: NotificationType?) {
    ALL("الكل", Icons.Filled.Notifications, null),
    ERROR("الأخطاء", Icons.Filled.ErrorOutline, NotificationType.ERROR),
    WARNING("التحذيرات", Icons.Filled.WarningAmber, NotificationType.WARNING),
    SUCCESS("العمليات الناجحة", Icons.Filled.CheckCircleOutline, NotificationType.SUCCESS),
    INFO("معلومات", Icons.Filled.Info, NotificationType.INFO)
}

data class NotificationStats(
    val total: Int = 0,
    val errors: Int = 0,
    val warnings: Int = 0,
    val successes: Int = 0,
    val infos: Int = 0
)

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val filteredItems: List<AppNotification> = emptyList(),
    val selectedFilter: NotificationFilter = NotificationFilter.ALL,
    val searchQuery: String = "",
    val stats: NotificationStats = NotificationStats(),
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(NotificationFilter.ALL)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<NotificationsUiState> = combine(
        notificationRepository.notifications,
        notificationRepository.unreadCount,
        _selectedFilter,
        _searchQuery
    ) { notifications, unread, filter, query ->
        val errorsCount = notifications.count { it.type == NotificationType.ERROR }
        val warningsCount = notifications.count { it.type == NotificationType.WARNING }
        val successesCount = notifications.count { it.type == NotificationType.SUCCESS }
        val infosCount = notifications.count { it.type == NotificationType.INFO }

        val stats = NotificationStats(
            total = notifications.size,
            errors = errorsCount,
            warnings = warningsCount,
            successes = successesCount,
            infos = infosCount
        )

        val filtered = notifications.filter { item ->
            val matchesFilter = when (filter.targetType) {
                null -> true
                else -> item.type == filter.targetType
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                item.title.contains(query, ignoreCase = true) ||
                        item.message.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }

        NotificationsUiState(
            items = notifications,
            filteredItems = filtered,
            selectedFilter = filter,
            searchQuery = query,
            stats = stats,
            unreadCount = unread
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationsUiState()
    )

    fun setFilter(filter: NotificationFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }
}
