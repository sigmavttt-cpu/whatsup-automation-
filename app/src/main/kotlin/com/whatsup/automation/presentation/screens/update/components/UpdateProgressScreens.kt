package com.whatsup.automation.presentation.screens.update.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.UpdateUiState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

/**
 * نافذة منبثقة لتنزيل التحديث مع عرض حي للمقاييس بتنسيق Monospace LTR.
 */
@Composable
fun DownloadProgressContent(
    state: UpdateUiState.Downloading,
    onUseMirror: () -> Unit
) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = state.progressPercent,
        animationSpec = tween(300, easing = LinearEasing),
        label = "DownloadProgress"
    )

    val downloadedMb = state.bytesDownloaded / (1024.0 * 1024.0)
    val totalMb = state.totalBytes / (1024.0 * 1024.0)
    val speedKb = state.speedBytesPerSec / 1024.0
    val speedMb = speedKb / 1024.0
    val speedText = if (speedMb >= 1.0) String.format(java.util.Locale.US, "%.2f MB/s", speedMb) else String.format(java.util.Locale.US, "%.0f KB/s", speedKb)

    Dialog(
        onDismissRequest = { /* Block dismiss */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            cornerRadius = 20.dp,
            borderBrush = ObsidianGlowBorder.Cyan
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ترويسة التنزيل
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "جارٍ التحميل...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${(state.progressPercent * 100).toInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // شريط التقدم
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceElevated)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan)
                    )
                }

                // إحصائيات الحجم والسرعة بتنسيق LTR Monospace
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb),
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = speedText,
                            fontSize = 12.sp,
                            color = WhatsAppGreen,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                if (state.etaSeconds > 0) {
                    Text(
                        text = "الوقت المتبقي التقديري: حوالي ${state.etaSeconds} ثانية",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (!state.updateInfo.mirrorApkUrl.isNullOrBlank()) {
                    TextButton(
                        onClick = onUseMirror,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("التبديل إلى الخادم البديل", color = CyberCyan, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * شاشة مرحلة فحص بصمة التشفير SHA-256.
 */
@Composable
fun VerifyingChecksumContent(state: UpdateUiState.VerifyingChecksum) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary.copy(alpha = 0.95f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = ObsidianGlowBorder.Purple
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = NeonPurple,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 4.dp
                )

                Text(
                    text = "جارٍ فحص سلامة وتشفير الحزمة...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "التحقق من مطابقة بصمة SHA-256 لمنع التلاعب وضمان الأمان التام.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * نافذة منبثقة لجاهزية التثبيت بعد اكتمال التنزيل وفحص البصمة.
 */
@Composable
fun ReadyToInstallContent(
    state: UpdateUiState.ReadyToInstall,
    canInstall: Boolean,
    onRequestPermission: () -> Unit,
    onInstall: () -> Unit
) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    Dialog(
        onDismissRequest = { /* Block dismiss */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        BentoCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            cornerRadius = 20.dp,
            borderBrush = ObsidianGlowBorder.Emerald
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "التحديث جاهز للتثبيت",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "v${state.updateInfo.versionName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreen,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "اكتمل التنزيل بنجاح وتم فحص البصمة المشفرة SHA-256.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!canInstall) {
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("السماح بتثبيت التطبيقات", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onInstall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null, tint = Color.White)
                        Text("تثبيت التحديث الآن", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * شاشة مرحلة التهدئة الآمنة للخدمات والـ DB.
 */
@Composable
fun TeardownProgressContent(state: UpdateUiState.PreparingTeardown) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary.copy(alpha = 0.98f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = ObsidianGlowBorder.Cyan
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(44.dp))
                Text(
                    text = "بروتوكول الإغلاق الآمن (Graceful Teardown)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = state.stage,
                    fontSize = 12.5.sp,
                    color = CyberCyan,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * شاشة بدء التثبيت الفعلي عبر حزمة النظام.
 */
@Composable
fun InstallingContent(state: UpdateUiState.Installing) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary.copy(alpha = 0.98f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = ObsidianGlowBorder.Emerald
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(48.dp))
                Text(
                    text = "جارٍ إطلاق معالج تثبيت النظام...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "يرجى تأكيد التحديث في نافذة أندرويد للمتابعة.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
