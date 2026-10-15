package com.whatsup.automation.presentation.screens.update

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateType
import com.whatsup.automation.domain.model.UpdateUiState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

/**
 * درع التحديث الإجباري والاختياري الشامل (FullScreenUpdateShield).
 * يوفر حماية أمنية وتجربة مستخدم فاخرة بنظام Dark Glassmorphism مع عرض سرعة التحميل والـ Changelog.
 */
@Composable
fun FullScreenUpdateShield(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is UpdateUiState.Idle -> { /* لا شيء */ }
        is UpdateUiState.Checking -> {
            // يمكن إظهار مؤشر خفيف اختياري
        }
        is UpdateUiState.UpToDate -> {
            // شاشة "التطبيق محدث" محجوبة تماماً بناءً على طلب المستخدم الصريح
            LaunchedEffect(Unit) {
                viewModel.dismissOptionalUpdate()
            }
        }
        is UpdateUiState.UpdateAvailable -> {
            if (state.isMandatory) {
                MandatoryUpdateShieldContent(
                    updateInfo = state.updateInfo,
                    currentVersionName = state.currentVersionName,
                    onStartDownload = { viewModel.startDownload(useMirror = false) }
                )
            } else {
                OptionalUpdateDialog(
                    updateInfo = state.updateInfo,
                    currentVersionName = state.currentVersionName,
                    onStartDownload = { viewModel.startDownload(useMirror = false) },
                    onDismiss = { viewModel.dismissOptionalUpdate() }
                )
            }
        }
        is UpdateUiState.Downloading -> {
            DownloadProgressContent(
                state = state,
                onUseMirror = { viewModel.startDownload(useMirror = true) }
            )
        }
        is UpdateUiState.VerifyingChecksum -> {
            VerifyingChecksumContent(state = state)
        }
        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallContent(
                state = state,
                canInstall = viewModel.canInstallPackages(),
                onRequestPermission = { viewModel.openInstallPermissionSettings() },
                onInstall = { viewModel.proceedWithTeardownAndInstall(state.apkFile) }
            )
        }
        is UpdateUiState.PreparingTeardown -> {
            TeardownProgressContent(state = state)
        }
        is UpdateUiState.Installing -> {
            InstallingContent(state = state)
        }
        is UpdateUiState.Failed -> {
            UpdateErrorDialog(
                state = state,
                onRetry = { viewModel.startDownload(useMirror = false) },
                onUseMirror = { viewModel.startDownload(useMirror = true) },
                onDismiss = { viewModel.dismissOptionalUpdate() }
            )
        }
    }
}

/**
 * واجهة الدرع الإجباري الكامل — تمنع الإغلاق أو الرجوع للخلف تماماً.
 */
@Composable
private fun MandatoryUpdateShieldContent(
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
                    // أيقونة الدرع النابض الفاخرة
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

                    // وسام نوع التحديث
                    UpdateTypeBadge(type = updateInfo.updateType)

                    Text(
                        text = updateInfo.titleAr.ifBlank { "تحديث إجباري جديد متاح" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    // مقارنة الإصدارات
                    VersionCompareBadge(
                        currentVersion = currentVersionName,
                        newVersion = updateInfo.versionName,
                        fileSize = updateInfo.getFormattedSize()
                    )

                    // صندوق سجل التغييرات
                    ChangelogBox(
                        changelog = updateInfo.changelogAr,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // زر التحديث الفوري
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = Color.White)
                            Text(
                                text = "تنزيل وتثبيت التحديث الآن 🚀",
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

/**
 * نافذة حوار التحديث الاختياري — تصميم مستطيل مباشر: عنوان "تحديث جديد" وتحته زر "تحديث".
 */
@Composable
private fun OptionalUpdateDialog(
    updateInfo: AppUpdateInfo,
    currentVersionName: String,
    onStartDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ترويسة مباشرة: "تحديث جديد" وشارة الإصدار
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تحديث جديد ✨",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "الحجم: ${updateInfo.getFormattedSize()} • إصدارك: $currentVersionName",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.dp, WhatsAppGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v${updateInfo.versionName}",
                            color = WhatsAppGreenDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)

                // سجل التغييرات المختصر
                ChangelogBox(
                    changelog = updateInfo.changelogAr,
                    modifier = Modifier.fillMaxWidth()
                )

                // زر "تحديث" المباشر تحته تماماً
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartDownload,
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
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White)
                            Text("تحديث ⚡", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color(0x30FFFFFF), Color(0x30FFFFFF))))
                    ) {
                        Text("لاحقاً", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * نافذة منبثقة مستطيلة لتنزيل التحديث بأقصى سرعة وبروتوكول متدفق مباشر.
 */
@Composable
private fun DownloadProgressContent(
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
                // ترويسة التنزيل السريع
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
                            text = "جارٍ التحميل بأقصى سرعة ⚡",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "${(state.progressPercent * 100).toInt()}%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                }

                // شريط التقدم السريع فائق النعومة
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBgSecondary)
                        .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(LiquidEmeraldGradient)
                    )
                }

                // إحصائيات الحجم وسرعة التنزيل اللحظية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f / %.1f MB", downloadedMb, totalMb),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "السرعة: $speedText",
                        fontSize = 12.sp,
                        color = WhatsAppGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (state.etaSeconds > 0) {
                    Text(
                        text = "الوقت التقديري: حوالي ${state.etaSeconds} ثانية",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                if (!state.updateInfo.mirrorApkUrl.isNullOrBlank()) {
                    TextButton(onClick = onUseMirror, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("التبديل إلى الخادم البديل 🔄", color = CyberCyan, fontSize = 12.sp)
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
private fun VerifyingChecksumContent(state: UpdateUiState.VerifyingChecksum) {
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
                    text = "التحقق من مطابقة بصمة SHA-256 لمنع التلاعب وضمان الأمان التام 🔒",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * نافذة منبثقة مستطيلة لجاهزية التثبيت بعد اكتمال التنزيل وفحص البصمة.
 */
@Composable
private fun ReadyToInstallContent(
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
                            text = "التحديث جاهز للتثبيت ✅",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "v${state.updateInfo.versionName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreen
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("السماح بتثبيت التطبيقات ⚠️", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onInstall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SystemUpdateAlt, contentDescription = null, tint = Color.White)
                        Text("تثبيت التحديث الآن 🚀", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
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
private fun TeardownProgressContent(state: UpdateUiState.PreparingTeardown) {
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
private fun InstallingContent(state: UpdateUiState.Installing) {
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

/**
 * نافذة حوار الخطأ عند فشل الفحص أو التنزيل.
 */
@Composable
private fun UpdateErrorDialog(
    state: UpdateUiState.Failed,
    onRetry: () -> Unit,
    onUseMirror: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state.isMandatory) {
        BackHandler(enabled = true) { /* Block */ }
    }

    Dialog(onDismissRequest = { if (!state.isMandatory) onDismiss() }) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = ObsidianGlowBorder.Rose
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RoseError, modifier = Modifier.size(48.dp))

                Text(
                    text = "تعذر إكمال التحديث",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = state.errorMessage,
                    fontSize = 12.5.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.canRetry) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إعادة المحاولة 🔄", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.canUseMirror) {
                        OutlinedButton(
                            onClick = onUseMirror,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التنزيل من الخادم البديل 🌐", color = CyberCyan)
                        }
                    }

                    if (!state.isMandatory) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("إغلاق", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار تفيد بأن التطبيق في أحدث إصدار بالفعل.
 */
@Composable
private fun UpToDateDialog(
    currentVersion: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = ObsidianGlowBorder.Emerald
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Filled.Verified, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(48.dp))

                Text(
                    text = "التطبيق محدث لأحدث إصدار ✨",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "أنت تستخدم أحدث إصدار متاح حالياً (v$currentVersion). كافة ميزات الأتمتة وحصانة الحساب تعمل بكفاءة تامة.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------- المكونات المساعدة --------------------------------

@Composable
private fun UpdateTypeBadge(type: UpdateType) {
    val (label, bg, fg) = when (type) {
        UpdateType.MANDATORY -> Triple("تحديث إجباري حاسم 🚨", RoseError.copy(alpha = 0.2f), RoseError)
        UpdateType.CRITICAL_SECURITY -> Triple("تحديث أمني حرج 🛡️", RoseError.copy(alpha = 0.2f), RoseError)
        UpdateType.SILENT_HOTFIX -> Triple("إصلاح سريع ⚡", NeonPurple.copy(alpha = 0.2f), NeonPurple)
        UpdateType.OPTIONAL -> Triple("تحديث اختياري متاح ✨", WhatsAppGreen.copy(alpha = 0.2f), WhatsAppGreenLight)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

@Composable
private fun VersionCompareBadge(
    currentVersion: String,
    newVersion: String,
    fileSize: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الإصدار الحالي", fontSize = 10.5.sp, color = TextMuted)
            Text("v$currentVersion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الإصدار الجديد", fontSize = 10.5.sp, color = TextMuted)
            Text("v$newVersion", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
        }

        Box(modifier = Modifier.size(1.dp, 24.dp).background(GlassBorder))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("حجم الحزمة", fontSize = 10.5.sp, color = TextMuted)
            Text(fileSize, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = CyberCyan)
        }
    }
}

@Composable
private fun ChangelogBox(
    changelog: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkBgSecondary)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Article, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
            Text(
                text = "سجل التغييرات والتحسينات:",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        HorizontalDivider(color = Color(0x15FFFFFF))

        Text(
            text = changelog.ifBlank { "تحسينات شاملة على الأداء وحصانة الحساب." },
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}
