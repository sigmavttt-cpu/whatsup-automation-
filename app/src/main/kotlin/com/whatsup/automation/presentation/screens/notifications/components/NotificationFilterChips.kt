package com.whatsup.automation.presentation.screens.notifications.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun NotificationFilterChips(
    selectedFilter: NotificationFilter,
    stats: NotificationStats,
    onFilterSelect: (NotificationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NotificationFilter.entries.forEach { filter ->
            val count = when (filter) {
                NotificationFilter.ALL -> stats.total
                NotificationFilter.ERROR -> stats.errors
                NotificationFilter.WARNING -> stats.warnings
                NotificationFilter.SUCCESS -> stats.successes
                NotificationFilter.INFO -> stats.infos
            }

            FilterChipItem(
                filter = filter,
                count = count,
                isSelected = selectedFilter == filter,
                onClick = { onFilterSelect(filter) }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    filter: NotificationFilter,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val accentColor = when (filter) {
        NotificationFilter.ALL -> WhatsAppGreenLight
        NotificationFilter.ERROR -> RoseError
        NotificationFilter.WARNING -> AmberWarning
        NotificationFilter.SUCCESS -> WhatsAppGreen
        NotificationFilter.INFO -> CyberCyan
    }

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else DarkSurface,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color(0x22FFFFFF),
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .debouncedClickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = filter.icon,
            contentDescription = filter.title,
            tint = if (isSelected) accentColor else TextSecondary,
            modifier = Modifier.size(15.dp)
        )

        Text(
            text = filter.title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimary else TextSecondary
        )

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) accentColor.copy(alpha = 0.35f) else Color(0x22FFFFFF))
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accentColor else TextMuted
            )
        }
    }
}
