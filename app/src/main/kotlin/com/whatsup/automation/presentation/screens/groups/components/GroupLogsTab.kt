package com.whatsup.automation.presentation.screens.groups.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.GroupLogStatus
import com.whatsup.automation.domain.model.GroupMessageLog
import com.whatsup.automation.presentation.components.AppEmptyState
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogsListTab(
    logs: List<GroupMessageLog>,
    emptyMessage: String,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyState(
                icon = Icons.Outlined.History,
                title = "لا توجد سجلات",
                subtitle = emptyMessage
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            items(logs, key = { it.id }) { log ->
                GroupLogCardItem(log = log)
            }
        }
    }
}

@Composable
fun GroupLogCardItem(
    log: GroupMessageLog,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.getDefault()) }
    val formattedTime = log.sentAt?.let { dateFormat.format(Date(it.toEpochMilli())) } ?: "الآن"

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.recipientName} (${log.recipientPhone})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Text(
                    text = when (log.status) {
                        GroupLogStatus.IN_PROGRESS -> "جاري الإرسال"
                        GroupLogStatus.COMPLETED -> "تم الإرسال بنجاح"
                        GroupLogStatus.FAILED -> "فشل الإرسال"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (log.status) {
                        GroupLogStatus.IN_PROGRESS -> AmberWarning
                        GroupLogStatus.COMPLETED -> WhatsAppGreen
                        GroupLogStatus.FAILED -> CoralError
                    }
                )
            }

            Text(
                text = "\"${log.messageText}\"",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                if (!log.errorMessage.isNullOrBlank()) {
                    Text(text = log.errorMessage, fontSize = 11.sp, color = CoralError)
                }
            }
        }
    }
}
