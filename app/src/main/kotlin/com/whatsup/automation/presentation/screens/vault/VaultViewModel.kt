package com.whatsup.automation.presentation.screens.vault

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.domain.model.VaultItem
import com.whatsup.automation.domain.model.VaultMediaType
import com.whatsup.automation.domain.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * طرق العرض في شاشة الخزينة (واجهة البطاقات الرئيسية HUB أو الصفحة الفرعية المستقلة SUB_PAGE).
 */
enum class VaultViewMode {
    HUB,
    SUB_PAGE
}

/**
 * التبويبات الفرعية للخزينة السرية.
 */
enum class VaultSubTab {
    TEXTS,       // 1. نصوص محذوفة (أحمر)
    EDITED,      // 2. رسائل تم تعديلها (كهرماني/أصفر)
    AUDIO,       // 3. ملاحظات وتسجيلات صوتية (سيان)
    MEDIA        // 4. صور ووسائط العرض لمرة واحدة (أخضر زمردي)
}

/**
 * حالة واجهة المستخدم للخزينة السرية والمحذوفات.
 */
data class VaultUiState(
    val viewMode: VaultViewMode = VaultViewMode.HUB,
    val activeTab: VaultSubTab = VaultSubTab.TEXTS,
    val searchQuery: String = "",
    val items: List<VaultItem> = emptyList(),
    val deletedTexts: List<VaultItem> = emptyList(),
    val editedMessages: List<VaultItem> = emptyList(),
    val mediaVaultItems: List<VaultItem> = emptyList(),
    val audioVaultItems: List<VaultItem> = emptyList(),
    val otherVaultItems: List<VaultItem> = emptyList(),
    val deletedCount: Int = 0,
    val isLoading: Boolean = false,
    val selectedItem: VaultItem? = null,
    val inspectedUserHistory: List<VaultItem> = emptyList(),
    val inspectedUserPhone: String? = null,
    val inspectedUserName: String? = null,
    val trackingMode: String = "ALL", // "ALL" or "SELECTED"
    val trackedPhones: Set<String> = emptySet(),
    val isTrackingDialogVisible: Boolean = false
) {
    val deletedTextCount: Int get() = deletedTexts.size
    val editedCount: Int get() = editedMessages.size
    val mediaCount: Int get() = mediaVaultItems.size
    val audioCount: Int get() = audioVaultItems.size
    val otherCount: Int get() = otherVaultItems.size
}

/**
 * مدير حالة الخزينة والمحذوفات (Vault ViewModel).
 */
@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultRepository: VaultRepository,
    private val sessionKeystore: com.whatsup.automation.data.security.SessionKeystore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _viewMode = MutableStateFlow(VaultViewMode.HUB)
    private val _activeTab = MutableStateFlow(VaultSubTab.TEXTS)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedItem = MutableStateFlow<VaultItem?>(null)
    private val _isRefreshing = MutableStateFlow(false)
    private val _inspectedUser = MutableStateFlow<Pair<String, String>?>(null) // phone to name
    private val _trackingMode = MutableStateFlow(sessionKeystore.getVaultTrackingMode())
    private val _trackedPhones = MutableStateFlow(sessionKeystore.getVaultTrackedPhones())
    private val _isTrackingDialogVisible = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    val selectedItem: StateFlow<VaultItem?> = _selectedItem.asStateFlow()

    val uiState: StateFlow<VaultUiState> = combine(
        _viewMode,
        _activeTab,
        _searchQuery,
        _inspectedUser,
        vaultRepository.getAllVaultItems(),
        vaultRepository.getDeletedCount(),
        _trackingMode,
        _trackedPhones,
        _isTrackingDialogVisible
    ) { args: Array<Any?> ->
        val mode = args[0] as VaultViewMode
        val tab = args[1] as VaultSubTab
        val query = args[2] as String
        val inspected = args[3] as? Pair<String, String>
        @Suppress("UNCHECKED_CAST")
        val allItems = args[4] as List<VaultItem>
        val delCount = args[5] as Int
        val trackingMode = args[6] as String
        @Suppress("UNCHECKED_CAST")
        val trackedPhones = args[7] as Set<String>
        val isDialogVisible = args[8] as Boolean

        val q = query.trim().lowercase()
        fun matchesQuery(item: VaultItem): Boolean {
            if (q.isBlank()) return true
            return item.senderName.lowercase().contains(q) ||
                    item.senderPhone.contains(q) ||
                    (item.originalText?.lowercase()?.contains(q) == true)
        }

        // 1. نصوص محذوفة: الرسائل النصية التي تم حذفها لدى الجميع
        val textsList = allItems.filter {
            it.isDeleted &&
            (it.mediaType == VaultMediaType.TEXT || (it.localFilePath.isNullOrBlank() && !it.originalText.isNullOrBlank())) &&
            matchesQuery(it)
        }

        // 2. رسائل تم تعديلها: الرسائل التي تحتوي على علامة تعديل أو نص أصلي مع نص معدل
        val editedList = allItems.filter {
            (it.originalText?.contains("✏️") == true || it.originalText?.contains("⬅️") == true || it.originalText?.contains("أصل:") == true) &&
            matchesQuery(it)
        }

        // 3. وسائط: الصور ومقاطع الفيديو (العرض لمرة واحدة أو المحذوفة)
        val mediaList = allItems.filter {
            (it.mediaType in listOf(VaultMediaType.IMAGE, VaultMediaType.VIDEO)) &&
            (it.isViewOnce || it.isDeleted) &&
            matchesQuery(it)
        }

        // 4. صوتيات: الملاحظات والتسجيلات الصوتية (العرض لمرة واحدة أو المحذوفة)
        val audioList = allItems.filter {
            it.mediaType == VaultMediaType.AUDIO &&
            (it.isViewOnce || it.isDeleted) &&
            matchesQuery(it)
        }

        // 5. لمرة واحدة: وسائط العرض لمرة واحدة المحفوظة
        val viewOnceList = allItems.filter {
            it.isViewOnce &&
            matchesQuery(it)
        }

        // 6. ملفات ومستندات أخرى
        val otherList = allItems.filter {
            (it.mediaType == VaultMediaType.DOCUMENT || (it.mediaType !in listOf(VaultMediaType.TEXT, VaultMediaType.IMAGE, VaultMediaType.VIDEO, VaultMediaType.AUDIO))) &&
            matchesQuery(it)
        }

        val activeList = when (tab) {
            VaultSubTab.TEXTS -> textsList
            VaultSubTab.EDITED -> editedList
            VaultSubTab.AUDIO -> audioList
            VaultSubTab.MEDIA -> mediaList
        }

        val userHistory = if (inspected != null) {
            allItems.filter { it.senderPhone == inspected.first || it.senderName == inspected.second }
        } else {
            emptyList()
        }

        VaultUiState(
            viewMode = mode,
            activeTab = tab,
            searchQuery = query,
            items = activeList,
            deletedTexts = textsList,
            editedMessages = editedList,
            mediaVaultItems = mediaList,
            audioVaultItems = audioList,
            otherVaultItems = otherList,
            deletedCount = delCount,
            isLoading = false,
            selectedItem = _selectedItem.value,
            inspectedUserHistory = userHistory,
            inspectedUserPhone = inspected?.first,
            inspectedUserName = inspected?.second,
            trackingMode = trackingMode,
            trackedPhones = trackedPhones,
            isTrackingDialogVisible = isDialogVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VaultUiState(isLoading = true)
    )

    fun navigateToSubPage(tab: VaultSubTab) {
        _activeTab.value = tab
        _viewMode.value = VaultViewMode.SUB_PAGE
    }

    fun navigateBackToHub() {
        _viewMode.value = VaultViewMode.HUB
    }

    fun openUserHistory(phone: String, name: String) {
        _inspectedUser.value = phone to name
    }

    fun closeUserHistory() {
        _inspectedUser.value = null
    }

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

    /**
     * حفظ وسائط العرض لمرة واحدة أو المحذوفة مباشرة في ألبوم صور/فيديوهات الهاتف
     */
    fun saveMediaToGallery(
        item: VaultItem,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val mediaPath = item.localFilePath
                if (!mediaPath.isNullOrBlank() && File(mediaPath).exists()) {
                    val srcFile = File(mediaPath)
                    val isVideo = item.mediaType == VaultMediaType.VIDEO || srcFile.extension.lowercase() in listOf("mp4", "mkv", "3gp")
                    val isAudio = item.mediaType == VaultMediaType.AUDIO || srcFile.extension.lowercase() in listOf("ogg", "mp3", "m4a", "opus")
                    val ext = srcFile.extension.ifBlank {
                        if (isVideo) "mp4" else if (isAudio) "ogg" else "jpg"
                    }

                    val parentDir = if (isVideo) {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    } else if (isAudio) {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    } else {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    }

                    val targetFolder = File(parentDir, "WhatsUp_Vault")
                    if (!targetFolder.exists()) targetFolder.mkdirs()

                    val destFile = File(targetFolder, "vault_${item.id}_${System.currentTimeMillis()}.$ext")
                    srcFile.copyTo(destFile, overwrite = true)

                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(destFile.absolutePath),
                        null,
                        null
                    )

                    withContext(Dispatchers.Main) {
                        onResult(true, "تم حفظ الوسائط بنجاح في المعرض 📥")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, "ملف الوسائط غير متوفر محلياً على الهاتف")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "تعذر الحفظ في المعرض: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * نسخ النص إلى حافظة الهاتف
     */
    fun copyTextToClipboard(
        text: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("WhatsUp Deleted Message", text)
            clipboard.setPrimaryClip(clip)
            onResult(true, "تم نسخ النص إلى الحافظة بنجاح 📋")
        } catch (e: Exception) {
            onResult(false, "تعذر نسخ النص: ${e.localizedMessage}")
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(450)
            _isRefreshing.value = false
        }
    }

    fun showTrackingConfigDialog() {
        _isTrackingDialogVisible.value = true
    }

    fun hideTrackingConfigDialog() {
        _isTrackingDialogVisible.value = false
    }

    fun setTrackingMode(mode: String) {
        sessionKeystore.setVaultTrackingMode(mode)
        _trackingMode.value = mode
    }

    fun addTrackedPhone(phone: String) {
        sessionKeystore.addVaultTrackedPhone(phone)
        _trackedPhones.value = sessionKeystore.getVaultTrackedPhones()
    }

    fun removeTrackedPhone(phone: String) {
        sessionKeystore.removeVaultTrackedPhone(phone)
        _trackedPhones.value = sessionKeystore.getVaultTrackedPhones()
    }
}
