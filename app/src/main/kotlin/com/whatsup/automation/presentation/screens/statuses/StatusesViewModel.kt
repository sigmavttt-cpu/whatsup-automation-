package com.whatsup.automation.presentation.screens.statuses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.domain.model.StatusAutomationSettings
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.domain.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

enum class StatusSubTab {
    NEW,
    VIEWED,
    SAVED,
    FAVORITES,
    MUTED,
    DELETED_TEMP
}

enum class StatusSortBy {
    DATE_DESC,
    DATE_ASC,
    NAME_ASC,
    FAVORITES
}

data class StatusesUiState(
    val statuses: List<StatusStory> = emptyList(),
    val settings: StatusAutomationSettings = StatusAutomationSettings(),
    val selectedStatus: StatusStory? = null,
    val selectedSubTab: StatusSubTab = StatusSubTab.NEW,
    val searchQuery: String = "",
    val sortBy: StatusSortBy = StatusSortBy.DATE_DESC,
    val selectedIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = true
) {
    val allCount: Int
        get() = statuses.size
    val newCount: Int
        get() = statuses.count { !it.isViewed && !it.isReacted && !it.isMuted && !it.isDeleted && !settings.mutedSenders.contains(it.senderPhone) }
    val viewedCount: Int
        get() = statuses.count { (it.isViewed || it.isReacted) && !it.isMuted && !it.isDeleted && !settings.mutedSenders.contains(it.senderPhone) }
    val savedCount: Int
        get() = statuses.count { it.isSaved }
    val favoritesCount: Int
        get() = statuses.count { (it.isFavorite || settings.favoriteSenders.contains(it.senderPhone)) && !it.isDeleted }
    val mutedCount: Int
        get() = statuses.count { (it.isMuted || settings.mutedSenders.contains(it.senderPhone)) && !it.isDeleted }
    val deletedTempCount: Int
        get() = statuses.count { it.isDeleted }
    val totalViewedCount: Int
        get() = viewedCount
    val viewedUniquePersonsCount: Int
        get() = statuses.filter { (it.isViewed || it.isReacted) && !it.isDeleted }.map { it.senderPhone }.distinct().size

    val displayedStatuses: List<StatusStory>
        get() {
            var list = when (selectedSubTab) {
                StatusSubTab.NEW -> statuses.filter { !it.isViewed && !it.isReacted && !it.isMuted && !it.isDeleted && !settings.mutedSenders.contains(it.senderPhone) }
                StatusSubTab.VIEWED -> statuses.filter { (it.isViewed || it.isReacted) && !it.isMuted && !it.isDeleted && !settings.mutedSenders.contains(it.senderPhone) }
                StatusSubTab.SAVED -> statuses.filter { it.isSaved }
                StatusSubTab.FAVORITES -> statuses.filter { (it.isFavorite || settings.favoriteSenders.contains(it.senderPhone)) && !it.isDeleted }
                StatusSubTab.MUTED -> statuses.filter { (it.isMuted || settings.mutedSenders.contains(it.senderPhone)) && !it.isDeleted }
                StatusSubTab.DELETED_TEMP -> statuses.filter { it.isDeleted }
            }

            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.senderName.lowercase().contains(q) ||
                    it.senderPhone.contains(q) ||
                    (it.textContent?.lowercase()?.contains(q) == true)
                }
            }

            return when (sortBy) {
                StatusSortBy.DATE_DESC -> list.sortedByDescending { it.timestamp.toEpochMilli() }
                StatusSortBy.DATE_ASC -> list.sortedBy { it.timestamp.toEpochMilli() }
                StatusSortBy.NAME_ASC -> list.sortedBy { it.senderName }
                StatusSortBy.FAVORITES -> list.sortedWith(
                    compareByDescending<StatusStory> { it.isFavorite || settings.favoriteSenders.contains(it.senderPhone) }
                        .thenByDescending { it.timestamp.toEpochMilli() }
                )
            }
        }
}
@HiltViewModel
class StatusesViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val whatsAppEngine: com.whatsup.automation.data.engine.WhatsAppEngine,
    private val deviceContactsManager: com.whatsup.automation.data.local.contacts.DeviceContactsManager,
    ) : ViewModel() {

    private val _uiState = MutableStateFlow(StatusesUiState())
    val uiState: StateFlow<StatusesUiState> = _uiState.asStateFlow()

    fun selectSubTab(tab: StatusSubTab) {
        _uiState.update { it.copy(selectedSubTab = tab) }
    }

    private val forceRefreshTrigger = kotlinx.coroutines.flow.MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            try {
                statusRepository.deleteExpiredStatuses()
            } catch (_: Exception) {}
        }
        observeStatuses()
        observeSettings()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                try {
                    statusRepository.deleteExpiredStatuses()
                    statusRepository.deleteExpiredDeletedStatuses()
                } catch (_: Exception) {}
                kotlinx.coroutines.delay(30_000)
                forceRefreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    fun excludeSelectedSenders() {
        val selected = _uiState.value.selectedIds
        val senders = _uiState.value.statuses.filter { it.id in selected }.map { it.senderPhone }.distinct()
        if (senders.isNotEmpty()) {
            viewModelScope.launch {
                statusRepository.muteMultipleSenders(senders, true)
                _uiState.update { it.copy(selectedIds = emptySet(), isMultiSelectMode = false) }
            }
        }
    }

    private fun observeStatuses() {
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                statusRepository.getAllStatuses(),
                forceRefreshTrigger,
                deviceContactsManager.contactsUpdateTrigger
            ) { list, _, _ ->
                list.map { status ->
                    val cleanDigits = status.senderPhone.replace("[^0-9]".toRegex(), "")
                    val isLid = status.senderPhone.contains("lid") || cleanDigits.length > 13
                    val rawSender = if (status.senderName.isNotBlank() &&
                        status.senderName != "جهة اتصال غير مسجلة" &&
                        status.senderName != "حالة واتساب" &&
                        status.senderName.lowercase() != "windows" &&
                        !status.senderName.contains("@") &&
                        !status.senderName.contains("lid") &&
                        !status.senderName.all { it.isDigit() || it == '+' || it == '-' || it == ' ' }
                    ) {
                        status.senderName
                    } else null

                    val resolved = deviceContactsManager.resolveContactNameWithPriority(status.senderPhone, rawSender)
                    val resolvedName = resolved?.name
                        ?: if (!isLid && cleanDigits.length in 7..15) {
                            "+$cleanDigits"
                        } else {
                            "جهة اتصال غير مسجلة"
                        }

                    status.copy(
                        senderName = resolvedName
                    )
                }
            }.collect { enriched ->
                _uiState.update { it.copy(statuses = enriched, isLoading = false) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            statusRepository.getSettings().collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    fun toggleAutoView(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(autoViewEnabled = enabled)
            statusRepository.updateSettings(updated)
        }
    }

    fun toggleAutoReact(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(autoReactEnabled = enabled)
            statusRepository.updateSettings(updated)
        }
    }

    fun setDefaultEmoji(emoji: String) {
        viewModelScope.launch {
            val updated = _uiState.value.settings.copy(defaultEmoji = emoji)
            statusRepository.updateSettings(updated)
        }
    }

    fun markStatusViewed(statusId: String) {
        viewModelScope.launch {
            val status = _uiState.value.statuses.find { it.id == statusId }
            val phone = status?.senderPhone ?: ""
            val participant = status?.participant
            whatsAppEngine.markStatusViewed(statusId, phone, participant)
            statusRepository.markAsViewed(statusId)
        }
    }

    fun reactToStatus(statusId: String, emoji: String) {
        viewModelScope.launch {
            val status = _uiState.value.statuses.find { it.id == statusId }
            val phone = status?.senderPhone ?: ""
            val participant = status?.participant
            whatsAppEngine.reactToStatus(statusId, emoji, phone, participant)
            statusRepository.updateReaction(statusId, emoji)
            statusRepository.markAsViewed(statusId)
        }
    }

    fun viewAndReact(statusId: String, emoji: String? = null) {
        viewModelScope.launch {
            val status = _uiState.value.statuses.find { it.id == statusId }
            val phone = status?.senderPhone ?: ""
            val participant = status?.participant
            val targetEmoji = emoji ?: _uiState.value.settings.defaultEmoji.ifBlank { "💚" }
            whatsAppEngine.markStatusViewed(statusId, phone, participant)
            whatsAppEngine.reactToStatus(statusId, targetEmoji, phone, participant)
            statusRepository.updateReaction(statusId, targetEmoji)
            statusRepository.markAsViewed(statusId)
        }
    }

    fun viewAllStatusesAndReact(emoji: String? = "💚") {
        viewModelScope.launch {
            val list = _uiState.value.statuses
            val targetEmoji = emoji ?: _uiState.value.settings.defaultEmoji.ifBlank { "💚" }
            for (status in list) {
                val phone = status.senderPhone
                val participant = status.participant
                whatsAppEngine.markStatusViewed(status.id, phone, participant)
                whatsAppEngine.reactToStatus(status.id, targetEmoji, phone, participant)
                statusRepository.updateReaction(status.id, targetEmoji)
                statusRepository.markAsViewed(status.id)
                kotlinx.coroutines.delay(350L) // تأخير بسيط لضمان الإرسال السليم عبر واتساب
            }
        }
    }

    fun selectStatus(status: StatusStory?) {
        _uiState.update { it.copy(selectedStatus = status) }
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            deviceContactsManager.notifyContactsChanged()
            forceRefreshTrigger.value = System.currentTimeMillis()
            try {
                whatsAppEngine.fetchAllStatuses()
            } catch (_: Exception) {}
            delay(1200L)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun fetchLatestStatuses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                whatsAppEngine.fetchAllStatuses()
            } catch (_: Exception) {}
            delay(6000L)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSortBy(sortBy: StatusSortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
    }

    fun toggleFavorite(status: StatusStory) {
        viewModelScope.launch {
            val isFav = !(status.isFavorite || _uiState.value.settings.favoriteSenders.contains(status.senderPhone))
            statusRepository.toggleFavoriteBySender(status.senderPhone, isFav)
            statusRepository.toggleFavorite(status.id, isFav)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun toggleMuted(status: StatusStory) {
        viewModelScope.launch {
            val isMuted = !(status.isMuted || _uiState.value.settings.mutedSenders.contains(status.senderPhone))
            statusRepository.toggleMutedBySender(status.senderPhone, isMuted)
            statusRepository.toggleMuted(status.id, isMuted)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun setMultiSelectMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = enabled,
                selectedIds = if (enabled) it.selectedIds else emptySet()
            )
        }
    }

    fun toggleSelection(statusId: String) {
        _uiState.update { state ->
            val updated = if (state.selectedIds.contains(statusId)) {
                state.selectedIds - statusId
            } else {
                state.selectedIds + statusId
            }
            state.copy(selectedIds = updated)
        }
    }

    fun selectAllDisplayed() {
        val allIds = _uiState.value.displayedStatuses.map { it.id }.toSet()
        _uiState.update { it.copy(selectedIds = allIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun muteSelected() {
        viewModelScope.launch {
            val selectedStatuses = _uiState.value.statuses.filter { it.id in _uiState.value.selectedIds }
            val phones = selectedStatuses.map { it.senderPhone }.distinct()
            statusRepository.muteMultipleSenders(phones, true)
            setMultiSelectMode(false)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun unmuteSelected() {
        viewModelScope.launch {
            val selectedStatuses = _uiState.value.statuses.filter { it.id in _uiState.value.selectedIds }
            val phones = selectedStatuses.map { it.senderPhone }.distinct()
            statusRepository.muteMultipleSenders(phones, false)
            setMultiSelectMode(false)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun favoriteSelected() {
        viewModelScope.launch {
            val selectedStatuses = _uiState.value.statuses.filter { it.id in _uiState.value.selectedIds }
            for (status in selectedStatuses) {
                statusRepository.toggleFavoriteBySender(status.senderPhone, true)
                statusRepository.toggleFavorite(status.id, true)
            }
            setMultiSelectMode(false)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    fun deleteSelectedStatuses() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedIds.toList()
            if (ids.isNotEmpty()) {
                statusRepository.deleteStatuses(ids)
                clearSelection()
                setMultiSelectMode(false)
                forceRefreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            statusRepository.deleteStatus(statusId)
            forceRefreshTrigger.value = System.currentTimeMillis()
        }
    }

    /**
     * حفظ وتنزيل وسائط الحالة أو نسخ نصها إلى الحافظة.
     */
    fun downloadStatus(
        context: android.content.Context,
        status: StatusStory,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cleanSenderName = (status.senderName.ifBlank { status.senderPhone })
                    .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                    .trim()
                    .ifBlank { "WhatsApp_Status" }
                val dateStr = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

                val targetDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
                val statusFolder = java.io.File(targetDir, "WhatsUp_Statuses")
                if (!statusFolder.exists()) statusFolder.mkdirs()

                val mediaPath = status.savedFilePath ?: status.mediaUrl

                if (!mediaPath.isNullOrBlank() && java.io.File(mediaPath).exists()) {
                    val srcFile = java.io.File(mediaPath)
                    val ext = srcFile.extension.ifBlank {
                        if (status.mediaType == StatusMediaType.VIDEO) "mp4" else "jpg"
                    }
                    val destFile = java.io.File(statusFolder, "${cleanSenderName}_$dateStr.$ext")
                    srcFile.copyTo(destFile, overwrite = true)

                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        null,
                        null
                    )
                    statusRepository.markStatusSaved(status.id, destFile.absolutePath, true)
                    forceRefreshTrigger.value = System.currentTimeMillis()

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تم حفظ وسائط الحالة باسم ($cleanSenderName) في المعرض 📥")
                    }
                } else if (status.mediaType == StatusMediaType.TEXT && !status.textContent.isNullOrBlank()) {
                    // إذا نصية: نسخ للحافظة وحفظ ملف نصي باسم الشخص بالمعرض
                    val destFile = java.io.File(statusFolder, "${cleanSenderName}_$dateStr.txt")
                    destFile.writeText("حالة من: ${status.senderName} (${status.senderPhone})\nالوقت: ${status.timestamp}\n\n${status.textContent}")
                    android.media.MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        null,
                        null
                    )
                    statusRepository.markStatusSaved(status.id, destFile.absolutePath, true)
                    forceRefreshTrigger.value = System.currentTimeMillis()

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        try {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("WhatsApp Status", status.textContent)
                            clipboard.setPrimaryClip(clip)
                            onResult(true, "تم نسخ النص وحفظ الحالة باسم ($cleanSenderName) 📋")
                        } catch (_: Exception) {
                            onResult(true, "تم حفظ نص الحالة باسم ($cleanSenderName) 📋")
                        }
                    }
                } else {
                    // الوسائط لم تكتمل بعد:
                    // 1. أولاً: فحص مجلد واتساب المخفي على الجهاز (.Statuses)
                    val localStatusFile = findInLocalWhatsAppStatuses(status.mediaType, status.timestamp.toEpochMilli())
                    if (localStatusFile != null && localStatusFile.exists() && localStatusFile.length() > 0) {
                        val ext = localStatusFile.extension.ifBlank {
                            if (status.mediaType == StatusMediaType.VIDEO) "mp4" else "jpg"
                        }
                        val destFile = java.io.File(statusFolder, "${cleanSenderName}_$dateStr.$ext")
                        localStatusFile.copyTo(destFile, overwrite = true)
                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destFile.absolutePath),
                            null,
                            null
                        )
                        statusRepository.updateMediaUrl(status.id, localStatusFile.absolutePath)
                        statusRepository.markStatusSaved(status.id, destFile.absolutePath, true)
                        forceRefreshTrigger.value = System.currentTimeMillis()

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "تم استخراج وحفظ وسائط الحالة فوراً من جهازك باسم ($cleanSenderName) 📥")
                        }
                        return@launch
                    }

                    // 2. ثانياً: استدعاء التنزيل الفوري من المحرك والانتظار حتى اكتمال التنزيل
                    whatsAppEngine.downloadStatusMedia(status.id)
                    var downloadedFile: java.io.File? = null
                    for (attempt in 1..15) {
                        kotlinx.coroutines.delay(400L)
                        val updated = statusRepository.getStatusById(status.id)
                        val p = updated?.mediaUrl ?: status.mediaUrl
                        if (!p.isNullOrBlank()) {
                            val candidate = java.io.File(p)
                            if (candidate.exists() && candidate.length() > 0) {
                                downloadedFile = candidate
                                break
                            }
                        }
                    }

                    if (downloadedFile != null && downloadedFile.exists()) {
                        val ext = downloadedFile.extension.ifBlank {
                            if (status.mediaType == StatusMediaType.VIDEO) "mp4" else "jpg"
                        }
                        val destFile = java.io.File(statusFolder, "${cleanSenderName}_$dateStr.$ext")
                        downloadedFile.copyTo(destFile, overwrite = true)

                        android.media.MediaScannerConnection.scanFile(
                            context,
                            arrayOf(destFile.absolutePath),
                            null,
                            null
                        )
                        statusRepository.markStatusSaved(status.id, destFile.absolutePath, true)
                        forceRefreshTrigger.value = System.currentTimeMillis()

                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(true, "تم حفظ وسائط الحالة بنجاح ونقلها إلى تبويب المحفوظات والمعرض 📥")
                        }
                    } else {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(false, "جارٍ استدعاء وسائط الحالة من واتساب في الخلفية، يرجى المحاولة بعد لحظات ⏳")
                        }
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "تعذر حفظ الحالة: ${e.localizedMessage ?: "خطأ غير متوقع"}")
                }
            }
        }
    }

    /**
     * فحص المجلد المخفي لواتساب الرسمي (.Statuses) على ذاكرة الجهاز لاستخراج ملف الوسائط مباشرة.
     */
    private fun findInLocalWhatsAppStatuses(mediaType: StatusMediaType, timestamp: Long): java.io.File? {
        val candidateDirs = listOf(
            java.io.File(android.os.Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"),
            java.io.File(android.os.Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses")
        )
        for (dir in candidateDirs) {
            try {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles() ?: continue
                    val matching = files.filter { f ->
                        val ext = f.extension.lowercase()
                        if (mediaType == StatusMediaType.VIDEO) ext in listOf("mp4", "mkv", "3gp")
                        else ext in listOf("jpg", "jpeg", "png", "webp")
                    }.sortedByDescending { it.lastModified() }

                    if (matching.isNotEmpty()) {
                        val closeMatch = matching.firstOrNull { Math.abs(it.lastModified() - timestamp) < 24 * 3600 * 1000L }
                            ?: matching.firstOrNull()
                        if (closeMatch != null && closeMatch.length() > 0) {
                            return closeMatch
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    /**
     * طلب استدعاء وسائط الحالة من واتساب عند فتح المعاينة في حال عدم توفرها.
     */
    fun requestStatusMediaDownload(statusId: String) {
        viewModelScope.launch {
            try {
                val status = statusRepository.getStatusById(statusId)
                if (status != null && status.mediaType != StatusMediaType.TEXT && status.mediaUrl.isNullOrBlank()) {
                    val local = findInLocalWhatsAppStatuses(status.mediaType, status.timestamp.toEpochMilli())
                    if (local != null) {
                        statusRepository.updateMediaUrl(statusId, local.absolutePath)
                        forceRefreshTrigger.value = System.currentTimeMillis()
                        return@launch
                    }
                }
                whatsAppEngine.downloadStatusMedia(statusId)
            } catch (_: Exception) {}
        }
    }

    fun unSaveStatus(statusId: String) {
        viewModelScope.launch {
            statusRepository.markStatusSaved(statusId, null, false)
        }
    }
}

