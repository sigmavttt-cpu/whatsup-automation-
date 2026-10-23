package com.whatsup.automation.presentation.screens.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Reply
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whatsup.automation.domain.model.DashboardStats
import com.whatsup.automation.presentation.components.AdaptiveBentoContainer
import com.whatsup.automation.presentation.components.StatCard
import com.whatsup.automation.presentation.theme.*

@Composable
fun DashboardStatsGrid(
    stats: DashboardStats,
    modifier: Modifier = Modifier
) {
    AdaptiveBentoContainer(modifier = modifier) { isTablet ->
        if (isTablet) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "الرسائل المعالجة",
                    value = stats.totalProcessed.toString(),
                    icon = Icons.Filled.Message,
                    accentColor = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "جهات الاتصال المحفوظة",
                    value = stats.totalSavedContacts.toString(),
                    icon = Icons.Filled.PersonAdd,
                    accentColor = WhatsAppGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "الردود التلقائية",
                    value = stats.totalRepliesSent.toString(),
                    icon = Icons.Filled.Reply,
                    accentColor = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "العمليات الناجحة",
                    value = stats.successfulOperations.toString(),
                    icon = Icons.Filled.CheckCircle,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "الرسائل المعالجة",
                        value = stats.totalProcessed.toString(),
                        icon = Icons.Filled.Message,
                        accentColor = CyberCyan,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "جهات الاتصال المحفوظة",
                        value = stats.totalSavedContacts.toString(),
                        icon = Icons.Filled.PersonAdd,
                        accentColor = WhatsAppGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "الردود التلقائية",
                        value = stats.totalRepliesSent.toString(),
                        icon = Icons.Filled.Reply,
                        accentColor = ElectricBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "العمليات الناجحة",
                        value = stats.successfulOperations.toString(),
                        icon = Icons.Filled.CheckCircle,
                        accentColor = NeonPurple,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
