package com.whatsup.automation.presentation.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.data.local.dao.LogDao
import com.whatsup.automation.data.local.dao.RuleUsageStat
import com.whatsup.automation.data.local.dao.StatusDao
import com.whatsup.automation.data.local.entity.LogEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

data class TopContactStat(
    val name: String,
    val phone: String,
    val count: Int,
    val type: String
)

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val totalProcessed: Int = 0,
    val totalReplies: Int = 0,
    val totalContactsSaved: Int = 0,
    val statusesViewed: Int = 0,
    val statusesReacted: Int = 0,
    val successRate: Float = 100f,
    val timeSavedFormatted: String = "0 دقيقة",
    val peakHourLabel: String = "--:--",
    val peakHourCount: Int = 0,
    val hourlyCounts: List<Int> = List(24) { 0 },
    val topRules: List<RuleUsageStat> = emptyList(),
    val topContacts: List<TopContactStat> = emptyList(),
    val recentLogs: List<LogEntity> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val logDao: LogDao,
    private val statusDao: StatusDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            calculateStats()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            calculateStats()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun calculateStats() = withContext(Dispatchers.IO) {
        try {
            val totalCount = logDao.getTotalCount()
            val repliesCount = logDao.getRepliesSentCount()
            val contactsCount = logDao.getContactsSavedCount()
            val noMatchCount = logDao.getNoMatchCount()

            // 1. حساب نسبة النجاح
            val effectiveTotal = repliesCount + noMatchCount
            val successRate = if (effectiveTotal > 0) {
                ((repliesCount.toFloat() / effectiveTotal.toFloat()) * 100f).coerceIn(0f, 100f)
            } else {
                100f
            }

            // 2. حساب إحصائيات الحالات الفعلية
            val allStatuses = try { statusDao.getAllStatuses().first() } catch (_: Exception) { emptyList() }
            val viewedStatusesCount = allStatuses.count { it.isViewed }
            val reactedStatusesCount = allStatuses.count { it.isReacted }

            // 3. حساب الوقت الموفّر (45 ثانية لكل رد + 30 ثانية لكل جهة اتصال + 10 ثوانٍ لكل مشاهدة/تفاعل حالة)
            val totalSecondsSaved = (repliesCount * 45L) + (contactsCount * 30L) + (viewedStatusesCount * 10L)
            val timeSavedText = when {
                totalSecondsSaved >= 3600 -> {
                    val hours = totalSecondsSaved / 3600.0
                    String.format(java.util.Locale.US, "%.1f ساعة", hours)
                }
                totalSecondsSaved >= 60 -> {
                    "${totalSecondsSaved / 60} دقيقة"
                }
                else -> "${totalSecondsSaved} ثانية"
            }

            // 4. تحليل ساعات الذروة خلال آخر 7 أيام
            val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 3600 * 1000)
            val timestamps = logDao.getLogTimestampsSince(sevenDaysAgo)
            val hourlyBuckets = IntArray(24) { 0 }
            val cal = Calendar.getInstance()

            for (ts in timestamps) {
                cal.timeInMillis = ts
                val hour = cal.get(Calendar.HOUR_OF_DAY) // 0..23
                if (hour in 0..23) {
                    hourlyBuckets[hour]++
                }
            }

            var maxHour = -1
            var maxCount = 0
            for (h in 0..23) {
                if (hourlyBuckets[h] > maxCount) {
                    maxCount = hourlyBuckets[h]
                    maxHour = h
                }
            }

            val peakLabel = if (maxHour != -1 && maxCount > 0) {
                String.format(java.util.Locale.US, "%02d:00 - %02d:00", maxHour, (maxHour + 1) % 24)
            } else {
                "لا توجد ذروة بعد"
            }

            // 5. القواعد الأكثر استخداماً
            val topRulesList = logDao.getTopUsedRules(limit = 5)

            // 6. أكثر جهات الاتصال نشاطاً في الحالات
            val topStatusSenders = allStatuses
                .groupBy { it.senderPhone }
                .map { (phone, statuses) ->
                    val name = statuses.firstOrNull()?.senderName?.ifBlank { phone } ?: phone
                    TopContactStat(
                        name = name,
                        phone = phone,
                        count = statuses.size,
                        type = "حالات"
                    )
                }
                .sortedByDescending { it.count }
                .take(5)

            // 7. سجل الأحداث والعمليات الأخيرة (Live Timeline Logs)
            val recentLogsList = try {
                logDao.getRecentLogs(10).first()
            } catch (_: Exception) {
                emptyList()
            }

            _uiState.update { current ->
                current.copy(
                    totalProcessed = totalCount,
                    totalReplies = repliesCount,
                    totalContactsSaved = contactsCount,
                    statusesViewed = viewedStatusesCount,
                    statusesReacted = reactedStatusesCount,
                    successRate = successRate,
                    timeSavedFormatted = timeSavedText,
                    peakHourLabel = peakLabel,
                    peakHourCount = maxCount,
                    hourlyCounts = hourlyBuckets.toList(),
                    topRules = topRulesList,
                    topContacts = topStatusSenders,
                    recentLogs = recentLogsList
                )
            }
        } catch (_: Exception) {}
    }
}
