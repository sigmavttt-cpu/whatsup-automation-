package com.whatsup.automation.presentation.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.screens.vault.VaultSubTab
import com.whatsup.automation.presentation.theme.*

@Composable
fun VaultHeaderBanner(
    deletedTextCount: Int,
    editedCount: Int,
    trackingMode: String = "ALL",
    trackedPhonesCount: Int = 0,
    onOpenTrackingConfig: () -> Unit = {}
) {
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(WhatsAppGreen.copy(alpha = 0.15f))
                        .border(1.5.dp, WhatsAppGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الرسائل المحذوفة والمعدلة",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (trackingMode == "ALL") "نطاق الحفظ: الجميع (كافة جهات الاتصال)" else "نطاق الحفظ: مخصص ($trackedPhonesCount أشخاص)",
                        fontSize = 11.sp,
                        color = if (trackingMode == "ALL") WhatsAppGreenLight else CyberCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenTrackingConfig,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tune,
                        contentDescription = "ضبط نطاق الحفظ",
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatBadgeItem(
                    modifier = Modifier.weight(1f),
                    title = "نصوص محذوفة",
                    count = deletedTextCount,
                    color = Color(0xFFFF4D4D),
                    icon = Icons.Default.DeleteSweep
                )
                StatBadgeItem(
                    modifier = Modifier.weight(1f),
                    title = "رسائل معدلة",
                    count = editedCount,
                    color = AmberWarning,
                    icon = Icons.Default.Edit
                )
            }
        }
    }
}

@Composable
fun StatBadgeItem(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    color: Color,
    icon: ImageVector
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A131A))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
            }
            Column {
                Text(text = title, fontSize = 10.5.sp, color = TextMuted)
                Text(
                    text = count.toString(),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun VaultHubCategoryCard(
    title: String,
    subtitle: String,
    count: Int,
    countLabel: String,
    color: Color,
    icon: ImageVector,
    latestInfo: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0D1720))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = 0.16f))
                            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = color.copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, color.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "$count $countLabel",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF080F14))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = latestInfo,
                    fontSize = 10.5.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VaultSubPageHeader(
    activeTab: VaultSubTab,
    onBack: () -> Unit
) {
    val (title, subtitle, color) = when (activeTab) {
        VaultSubTab.TEXTS -> Triple("النصوص المحذوفة لدى الجميع", "الرسائل النصية التي قام الطرف الآخر بحذفها", Color(0xFFFF4D4D))
        VaultSubTab.EDITED -> Triple("رسائل تم تعديلها وحفظ النص السابق", "كشف النص الأصلي المخفي ومقارنته بالمعدل", AmberWarning)
        VaultSubTab.AUDIO -> Triple("الملاحظات الصوتية المحذوفة", "البصمات والتسجيلات الصوتية المسحوبة من الطرفين", CyberCyan)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurface)
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "رجوع",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VaultCategoryTabButton(
    modifier: Modifier = Modifier,
    title: String,
    count: Int,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val bgModifier = if (isSelected) {
        Modifier
            .background(activeColor.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .border(1.2.dp, activeColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    } else {
        Modifier
            .background(DarkSurface, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(bgModifier)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeColor else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) activeColor else TextSecondary
                )
                if (count > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) activeColor.copy(alpha = 0.25f) else Color(0x33FFFFFF))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) activeColor else TextMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
