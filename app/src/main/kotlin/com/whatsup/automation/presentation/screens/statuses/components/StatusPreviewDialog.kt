package com.whatsup.automation.presentation.screens.statuses.components

import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.*
import androidx.core.content.FileProvider
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.presentation.components.StoryAvatarRing
import com.whatsup.automation.presentation.theme.*
import java.io.File
import kotlin.math.abs

/**
 * نافذة معاينة الحالة المنبثقة الواقعية بمنتصف الشاشة (Status Preview Dialog)
 * تحاكي استعراض الحالة الطبيعية مع زر (X) للإغلاق وأزرار الحفظ/النسخ والتفاعل.
 */
@Composable
fun StatusPreviewDialog(
    story: StatusStory,
    onDismiss: () -> Unit,
    onDownloadOrCopy: () -> Unit,
    onReactClick: (String) -> Unit,
    onRequestDownload: ((String) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val localBitmap = remember(story.savedFilePath, story.mediaUrl) {
        val path = story.savedFilePath ?: story.mediaUrl
        if (!path.isNullOrBlank()) {
            try {
                val f = File(path)
                if (f.exists() && f.length() > 0) {
                    if (story.mediaType == StatusMediaType.VIDEO) {
                        try {
                            val retriever = MediaMetadataRetriever()
                            retriever.setDataSource(f.absolutePath)
                            val frame = retriever.frameAtTime
                            retriever.release()
                            frame
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        BitmapFactory.decodeFile(f.absolutePath)
                    }
                } else null
            } catch (_: Exception) {
                null
            }
        } else null
    }

    var loadTimeout by remember { mutableStateOf(false) }

    // طلب استدعاء تنزيل وسائط الحالة تلقائياً عند فتح المعاينة إذا لم تكن متوفرة
    LaunchedEffect(story.id, story.mediaUrl) {
        loadTimeout = false
        val path = story.savedFilePath ?: story.mediaUrl
        if ((path.isNullOrBlank() || !File(path).exists()) && story.mediaType != StatusMediaType.TEXT) {
            onRequestDownload?.invoke(story.id)
            kotlinx.coroutines.delay(4000L)
            loadTimeout = true
        }
    }

    // تدرجات لونية فاخرة للحالات النصية
    val statusGradients = remember {
        listOf(
            listOf(Color(0xFF004D40), Color(0xFF00796B), Color(0xFF009688)), // زمردي كلاسيكي
            listOf(Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFF9C27B0)), // بنفسجي غسقي
            listOf(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFE91E63)), // وردي ياقوتي
            listOf(Color(0xFFBF360C), Color(0xFFE64A19), Color(0xFFFF5722)), // برتقالي ناري
            listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF2196F3)), // أزرق محيطي
            listOf(Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF4CAF50))  // أخضر طبيعي
        )
    }

    val selectedGradient = remember(story.id) {
        val idx = abs(story.id.hashCode()) % statusGradients.size
        statusGradients[idx]
    }

    // مؤقت شريط تقدم الحالة (الستوري) يبدأ من 0 حتى 100% مع دعم الإيقاف المؤقت عند اللمس
    val totalDurationMs = remember(story.mediaType) {
        if (story.mediaType == StatusMediaType.VIDEO) 15_000L else 6_000L
    }
    val progress = remember { Animatable(0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(story.id, isPaused) {
        if (!isPaused) {
            val remaining = (1f - progress.value).coerceIn(0f, 1f)
            val duration = (remaining * totalDurationMs).toInt().coerceAtLeast(100)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            )
            onDismiss()
        } else {
            progress.stop()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable { onDismiss() }
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            // صندوق المعاينة بمنتصف الشاشة
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0F1A22))
                    .border(1.5.dp, ObsidianGlowBorder.Emerald, RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // منع نقر الخلفية من إغلاق الصندوق
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // شريط التقدم الزمني للقصة (Story Progress Bar) من البداية حتى النهاية
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x33FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress.value.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(WhatsAppGreen)
                    )
                }

                if (isPaused) {
                    Surface(
                        color = CyberCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "⏸️ المؤقت متوقف مؤقتاً — انقر للمتابعة",
                            fontSize = 11.sp,
                            color = CyberCyan,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // 1. شريط الرأس (الأفاتار + الاسم + الوقت + زر X الدائري للخروج)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StoryAvatarRing(
                            initials = story.senderName.ifBlank { story.senderPhone }.take(2),
                            isViewed = story.isViewed || story.isReacted,
                            size = 46.dp
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = story.senderName.ifBlank { story.senderPhone },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (story.isDeleted) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = CoralError,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "تم حذف الحالة",
                                        fontSize = 11.sp,
                                        color = CoralError,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = if (story.isViewed) TextMuted else WhatsAppGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (story.isViewed) "تمت المشاهدة" else "حالة جديدة",
                                        fontSize = 11.sp,
                                        color = if (story.isViewed) TextMuted else WhatsAppGreenLight
                                    )
                                }

                                Text("•", fontSize = 10.sp, color = TextMuted)

                                Text(
                                    text = formatRelativeTimeArabic(story.timestamp),
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // زر الخروج المضيء (X)
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x25FFFFFF))
                            .border(1.dp, Color(0x35FFFFFF), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق المعاينة",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(color = Color(0x1AFFFFFF), thickness = 1.dp)

                // 2. محتوى الحالة (كأنه حالة واتساب طبيعية بمنتصف الشاشة)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 260.dp, max = 420.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (localBitmap == null && story.mediaType == StatusMediaType.TEXT)
                                Brush.linearGradient(selectedGradient)
                            else
                                Brush.linearGradient(listOf(Color(0xFF14212C), Color(0xFF0A1015)))
                        )
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isPaused = !isPaused
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (localBitmap != null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = localBitmap.asImageBitmap(),
                                contentDescription = "معاينة كاملة",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                            if (story.mediaType == StatusMediaType.VIDEO) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .border(2.dp, WhatsAppGreen, CircleShape)
                                        .clickable {
                                            val path = story.savedFilePath ?: story.mediaUrl
                                            if (!path.isNullOrBlank()) {
                                                try {
                                                    val f = File(path)
                                                    val uri = FileProvider.getUriForFile(
                                                        context,
                                                        "${context.packageName}.fileprovider",
                                                        f
                                                    )
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(uri, "video/*")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {}
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "تشغيل الفيديو",
                                        tint = WhatsAppGreen,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        when (story.mediaType) {
                            StatusMediaType.TEXT -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = story.textContent ?: "لا يوجد نص في الحالة",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 28.sp
                                    )
                                }
                            }
                            StatusMediaType.VIDEO -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = WhatsAppGreen,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "جارٍ تحميل مقطع الفيديو...",
                                        fontSize = 13.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            StatusMediaType.IMAGE -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = CyberCyan,
                                        strokeWidth = 3.dp
                                    )
                                    Text(
                                        text = "جارٍ تحميل صورة الحالة...",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                    if (loadTimeout) {
                                        OutlinedButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onRequestDownload?.invoke(story.id)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("إعادة المحاولة 🔄", fontSize = 11.5.sp, color = CyberCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. شريط الإجراءات السفلي (نسخ النص أو حفظ الحالة + التفاعل بالقلب الأخضر)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // زر النسخ أو الحفظ المباشر الذكي
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDownloadOrCopy()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (story.isSaved) ElectricBlue else WhatsAppGreen,
                            contentColor = if (story.isSaved) Color.White else Color.Black
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isText = story.mediaType == StatusMediaType.TEXT
                            val icon = if (story.isSaved) {
                                Icons.Filled.Check
                            } else if (isText) {
                                Icons.Filled.ContentCopy
                            } else {
                                Icons.Filled.FileDownload
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (story.isSaved) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (story.isSaved) "محفوظة في الجهاز ✅" else if (isText) "نسخ النص 📋" else "حفظ الحالة 📥",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (story.isSaved) Color.White else Color.Black
                            )
                        }
                    }

                    // أزرار التفاعل السريع بالإيموجي
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("💚", "❤️", "🔥", "😂").forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x18FFFFFF))
                                    .border(1.dp, Color(0x25FFFFFF), CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onReactClick(emoji)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
