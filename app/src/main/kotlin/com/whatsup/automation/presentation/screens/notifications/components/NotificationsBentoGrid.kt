package com.whatsup.automation.presentation.screens.notifications.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.debouncedClickable
import com.whatsup.automation.presentation.screens.notifications.NotificationFilter
import com.whatsup.automation.presentation.screens.notifications.NotificationStats
import com.whatsup.automation.presentation.theme.*

@Composable
fun NotificationsBentoGrid(
    stats: NotificationStats,
    selectedFilter: NotificationFilter,
    onFilterSelect: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BentoStatCard(
            title = "الأخطاء",
            count = stats.errors,
            icon = Icons.Filled.ErrorOutline,
            accentColor = RoseError,
            isSelected = selectedFilter == NotificationFilter.ERROR,
            onClick = {
                onFilterSelect(
                    if (selectedFilter == NotificationFilter.ERROR) NotificationFilter.ALL else NotificationFilter.ERROR
                )
            },
            modifier = Modifier.weight(1f)
        )

        BentoStatCard(
            title = "التحذيرات",
            count = stats.warnings,
            icon = Icons.Filled.WarningAmber,
            accentColor = AmberWarning,
            isSelected = selectedFilter == NotificationFilter.WARNING,
            onClick = {
                onFilterSelect(
                    if (selectedFilter == NotificationFilter.WARNING) NotificationFilter.ALL else NotificationFilter.WARNING
                )
            },
            modifier = Modifier.weight(1f)
        )

        BentoStatCard(
            title = "الناجحة",
            count = stats.successes,
            icon = Icons.Filled.CheckCircleOutline,
            accentColor = WhatsAppGreen,
            isSelected = selectedFilter == NotificationFilter.SUCCESS,
            onClick = {
                onFilterSelect(
                    if (selectedFilter == NotificationFilter.SUCCESS) NotificationFilter.ALL else NotificationFilter.SUCCESS
                )
            },
            modifier = Modifier.weight(1f)
        )

        BentoStatCard(
            title = "معلومات",
            count = stats.infos,
            icon = Icons.Filled.Info,
            accentColor = CyberCyan,
            isSelected = selectedFilter == NotificationFilter.INFO,
            onClick = {
                onFilterSelect(
                    if (selectedFilter == NotificationFilter.INFO) NotificationFilter.ALL else NotificationFilter.INFO
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BentoStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else accentColor.copy(alpha = 0.25f),
        animationSpec = tween(250),
        label = "bentoBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.16f) else DarkSurface,
        animationSpec = tween(250),
        label = "bentoBg"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.2.dp, borderColor, RoundedCornerShape(16.dp))
            .debouncedClickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = count.toString(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (count > 0) TextPrimary else TextMuted
            )

            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else TextSecondary,
                maxLines = 1
            )
        }
    }
}
