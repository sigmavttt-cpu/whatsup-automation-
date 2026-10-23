package com.whatsup.automation.presentation.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.PulseReactor
import com.whatsup.automation.presentation.theme.*

/**
 * بطاقة المحرك والحساب الكبرى (Hero Engine & Profile Card)
 */
@Composable
fun SettingsEngineCard(
    onNavigateToPairing: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.Emerald
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.5.dp, WhatsAppGreen.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PulseReactor(isActive = true, activeColor = WhatsAppGreen)
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "محرك Sigma AutoFlow",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                            )
                            Text(
                                text = "متصل ومحمي محلياً 100%",
                                fontSize = 12.sp,
                                color = WhatsAppGreenLight,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToPairing()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = DarkBgPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "إدارة الربط",
                        fontSize = 12.sp,
                        color = DarkBgPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // شريط مؤشرات الأمان المشفرة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BentoMiniPill(
                    label = "Android Keystore",
                    subLabel = "تشفير AES-256",
                    tint = CyberCyan,
                    modifier = Modifier.weight(1f)
                )
                BentoMiniPill(
                    label = "Zero-Cloud",
                    subLabel = "محلي بدون سيرفرات",
                    tint = WhatsAppGreen,
                    modifier = Modifier.weight(1f)
                )
                BentoMiniPill(
                    label = "v8.3.0",
                    subLabel = "Sigma Stable",
                    tint = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
