package com.whatsup.automation.presentation.screens.update.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

/**
 * واجهة الدرع الإجباري الكامل — تمنع الإغلاق أو الرجوع للخلف تماماً.
 */
@Composable
fun MandatoryUpdateShieldContent(
    updateInfo: AppUpdateInfo,
    currentVersionName: String,
    onStartDownload: () -> Unit
) {
    // منع زر الرجوع نهائياً في التحديث الإجباري
    BackHandler(enabled = true) { /* Block back press */ }

    val infiniteTransition = rememberInfiniteTransition(label = "ShieldPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            borderBrush = ObsidianGlowBorder.Rose
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(RoseError.copy(alpha = 0.15f))
                            .border(2.dp, RoseError.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SecurityUpdateGood,
                            contentDescription = "تحديث أمني حاسم",
                            tint = RoseError,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    UpdateTypeBadge(type = updateInfo.updateType)

                    Text(
                        text = updateInfo.titleAr.ifBlank { "تحديث إجباري جديد متاح" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    VersionCompareBadge(
                        currentVersion = currentVersionName,
                        newVersion = updateInfo.versionName,
                        fileSize = updateInfo.getFormattedSize()
                    )

                    ChangelogBox(
                        changelog = updateInfo.changelogAr,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                            Text(
                                text = "تنزيل وتثبيت التحديث الآن",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "هذا التحديث إلزامي لضمان استقرار الاتصال وحصانة الحساب ضد الحظر.",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
