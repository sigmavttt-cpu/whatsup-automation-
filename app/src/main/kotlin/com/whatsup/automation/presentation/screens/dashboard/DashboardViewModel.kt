package com.whatsup.automation.presentation.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.data.engine.WhatsAppEngine
import com.whatsup.automation.domain.model.ActivityLog
import com.whatsup.automation.domain.model.ConnectionState
import com.whatsup.automation.domain.model.DashboardStats
import com.whatsup.automation.data.local.entity.SyncedContactDao
import com.whatsup.automation.domain.repository.LogRepository
import com.whatsup.automation.service.ServicePermissionManager
import com.whatsup.automation.service.WhatsAppForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val isAssistantActive: Boolean = false,
    val isAutomationPaused: Boolean = false,
    val stats: DashboardStats = DashboardStats(),
    val recentLogs: List<ActivityLog> = emptyList(),
    val isRefreshing: Boolean = false,
    val isWritePermissionGranted: Boolean = true,
    val rulesCount: Int = 0,
    val isPreviewHidden: Boolean = true,
    val isTerminalExpanded: Boolean = false,
    val terminalLines: List<String> = listOf(
        "[$] طھظ‡ظٹط¦ط© ظ…ط­ط±ظƒ ط§ظ„ط£طھظ…طھط© ط§ظ„ظ…ط­ظ„ظٹ...",
        "[$] ط§ظ„طھط­ظ‚ظ‚ ظ…ظ† ط§ظ„ط¬ظ„ط³ط§طھ ط§ظ„ط¢ظ…ظ†ط© ظپظٹ Android KeyStore...",
        "[$] ظ†ط¸ط§ظ… Room ط¬ط§ظ‡ط² ظ„ظ„ط§ط³طھظ‚ط¨ط§ظ„ ظˆط§ظ„ظپط±ط² ط§ظ„ط¢ظ„ظٹ."
    )
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val whatsAppEngine: WhatsAppEngine,
    private val logRepository: LogRepository,
    private val ruleRepository: com.whatsup.automation.domain.repository.RuleRepository,
    private val syncedContactDao: SyncedContactDao,
    private val deviceContactsManager: com.whatsup.automation.data.local.contacts.DeviceContactsManager,
    private val notificationRepository: com.whatsup.automation.domain.repository.NotificationRepository,
    private val serviceHealthMonitor: com.whatsup.automation.service.ServiceHealthMonitor
) : ViewModel() {

    val healthState = serviceHealthMonitor.healthState
    val notifications: StateFlow<List<com.whatsup.automation.domain.model.AppNotification>> = notificationRepository.notifications
    val unreadNotificationsCount: StateFlow<Int> = notificationRepository.unreadCount

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }

    init {
        checkPermissions()
        observeConnectionState()
        observeAssistantState()
        observeAutomationPauseState()
        observeContacts()
        observeLogs()
        observeEngineDiagnostics()
        observeRules()
        refreshStats()
        startAutoRefresh()
    }

    private fun observeAutomationPauseState() {
        viewModelScope.launch {
            whatsAppEngine.isAutomationPaused.collect { isPaused ->
                _uiState.update { it.copy(isAutomationPaused = isPaused) }
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                refreshDashboard()
            }
        }
    }

    private fun checkPermissions() {
        val hasWrite = ServicePermissionManager.hasWriteContactsPermission(context)
        val hasRead = ServicePermissionManager.hasReadContactsPermission(context)
        _uiState.update { it.copy(isWritePermissionGranted = hasWrite && hasRead) }
    }

    private fun observeAssistantState() {
        viewModelScope.launch {
            whatsAppEngine.isDirectAutomationActive.collect { active ->
                _uiState.update { it.copy(isAssistantActive = active) }
            }
        }
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            checkPermissions()
            whatsAppEngine.refreshRealConnectionState()
            refreshStats()
            kotlinx.coroutines.delay(400)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            whatsAppEngine.connectionState.collect { state ->
                _uiState.update { current ->
                    val line = when (state) {
                        is ConnectionState.Connected -> "[$] âœ… طھظ… ط§ظ„ط§طھطµط§ظ„ ط¨ظˆط§طھط³ط§ط¨ ط¨ظ†ط¬ط§ط­ â€” ط§ظ„ظ…ط­ط±ظƒ ظ†ط´ط·."
                        is ConnectionState.AwaitingPairing -> "[$] âڈ³ ظپظٹ ط§ظ†طھط¸ط§ط± ط§ظ„ط§ظ‚طھط±ط§ظ† (QR ط£ظˆ Pairing Code)..."
                        is ConnectionState.Disconnected -> "[$] âڈ¸ï¸ڈ ط§ظ„ظ…ط­ط±ظƒ ظپظٹ ظˆط¶ط¹ ط§ظ„طھظˆظ‚ظپ."
                        is ConnectionState.Reconnecting -> "[$] ًں”„ ط¬ط§ط±ظچ ط¥ط¹ط§ط¯ط© ظ…ط­ط§ظˆظ„ط© ط§ظ„ط§طھطµط§ظ„..."
                        is ConnectionState.Error -> "[$] â‌Œ ط®ط·ط£: ${state.message}"
                    }
                    current.copy(
                        connectionState = state,
                        terminalLines = (current.terminalLines + line).takeLast(10)
                    )
                }
            }
        }
    }

    private fun observeEngineDiagnostics() {
        viewModelScope.launch {
            whatsAppEngine.engineDiagnostics.collect { diagnostics ->
                val contactEvents = diagnostics.filter { it.contains("[Contacts]") }.takeLast(3)
                if (contactEvents.isNotEmpty()) {
                    val formattedLines = contactEvents.map { "[$] ${it.substringAfter("] ")}" }
                    _uiState.update { current ->
                        current.copy(
                            terminalLines = (current.terminalLines + formattedLines).distinct().takeLast(10)
                        )
                    }
                }
            }
        }
    }

    
    private fun observeContacts() {
        viewModelScope.launch {
            try {
                val count = syncedContactDao.getCount()
                _uiState.update { it.copy(stats = it.stats.copy(totalSavedContacts = count)) }
            } catch (e: Exception) {}
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            logRepository.getRecentLogs(10).collect { logs ->
                _uiState.update { current ->
                    val newLines = logs.take(3).map { log ->
                        val cleanDigits = log.senderPhone.replace("[^0-9]".toRegex(), "")
                        val deviceName = if (cleanDigits.length >= 7) {
                            deviceContactsManager.getContactDisplayName(log.senderPhone)
                                ?: deviceContactsManager.getContactDisplayName(cleanDigits)
                        } else null
                        val displayIdentifier = deviceName ?: if (cleanDigits.length >= 7) "+$cleanDigits" else "ظ…ط­ط§ط¯ط«ط©"
                        "[$] [$displayIdentifier] -> ${log.actionExecuted}"
                    }
                    current.copy(
                        recentLogs = logs,
                        terminalLines = (current.terminalLines + newLines).distinct().takeLast(10)
                    )
                }
                refreshStats()
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            val stats = logRepository.getStats()
            _uiState.update { it.copy(stats = stats) }
        }
    }

    private fun observeRules() {
        viewModelScope.launch {
            ruleRepository.getAllRules().collect { rules ->
                _uiState.update { it.copy(rulesCount = rules.size) }
            }
        }
    }

    fun togglePreviewPrivacy() {
        _uiState.update { it.copy(isPreviewHidden = !it.isPreviewHidden) }
    }

    fun toggleTerminalExpanded() {
        _uiState.update { it.copy(isTerminalExpanded = !it.isTerminalExpanded) }
    }

    fun toggleService() {
        if (_uiState.value.connectionState is ConnectionState.Connected) {
            whatsAppEngine.toggleAutomationPause()
        } else {
            startAllServices()
        }
    }

    /**
     * ط¥ظٹظ‚ط§ظپ ط´ط§ظ…ظ„ ظ„ظ„ظ…ط­ط±ظƒ ظˆط§ظ„ط®ط¯ظ…ط© ط§ظ„ط£ظ…ط§ظ…ظٹط© ظˆظƒطھظ… ط§ظ„ط±ط¯ظˆط¯ ط¨ط§ظ„ظƒط§ظ…ظ„ (Master Kill / Deep Sleep).
     * ظٹط­ط±ط± ظƒط§ظپط© ظ…ظˆط§ط±ط¯ ط§ظ„ظ…ط¹ط§ظ„ط¬ ظˆط§ظ„ط°ط§ظƒط±ط© ظˆط´ط±ظٹط· ط§ظ„ط¥ط´ط¹ط§ط±ط§طھ ظپظˆط±ظٹط§ظ‹.
     */
    fun stopAllServices() {
        viewModelScope.launch {
            WhatsAppForegroundService.stop(context)
            whatsAppEngine.stopEngine()
            whatsAppEngine.setAutomationPaused(true)
        }
    }

    /**
     * ط¨ط¯ط، ظˆطھط´ط؛ظٹظ„ ط§ظ„ط®ط¯ظ…ط© ط§ظ„ط£ظ…ط§ظ…ظٹط© ظˆظ…ط­ط±ظƒ ط§ظ„ط£طھظ…طھط© ط¨ط§ظ„ظƒط§ظ…ظ„ ط¨ط¶ط؛ط·ط© ظˆط§ط­ط¯ط©.
     */
    fun startAllServices() {
        viewModelScope.launch {
            WhatsAppForegroundService.start(context)
            whatsAppEngine.startEngine()
            whatsAppEngine.setAutomationPaused(false)
        }

    }
}
