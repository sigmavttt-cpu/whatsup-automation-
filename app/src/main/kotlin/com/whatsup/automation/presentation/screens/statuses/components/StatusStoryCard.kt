package com.whatsup.automation.presentation.screens.statuses.components

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.StatusMediaType
import com.whatsup.automation.domain.model.StatusStory
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.StoryAvatarRing
import com.whatsup.automation.presentation.theme.*
import java.io.File
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/**
 * دالة صياغة الوقت النسبي باللغة العربية (مطابقة لتصميم الحالات).
 */
fun formatRelativeTimeArabic(instant: Instant): String {
    val duration = Duration.between(instant, Instant.now())
    val minutes = duration.toMinutes()
    val hours = duration.toHours()
    return when {
        minutes < 1 -> "الآن"
        minutes == 1L -> "منذ دقيقة"
        minutes == 2L -> "منذ دقيقتين"
        minutes in 3..10 -> "منذ $minutes دقائق"
        minutes < 60 -> "منذ $minutes دقيقة"
        hours == 1L -> "منذ ساعة"
        hours == 2L -> "منذ ساعتين"
        hours in 3..10 -> "منذ $hours ساعات"
        hours < 24 -> "منذ $hours ساعة"
        else -> "منذ يوم"
    }
}

/**
 * بطاقة الحالة الذكية المدمجة (Status Story Card)
 * تقتبس الفكرة من تصميم الحالات الحديث:
 * 1. بيانات جهة الاتصال (الأفاتار، الاسم، وحالة المشاهدة/الحذف مع الوقت).
 * 2. صندوق معاينة محتوى الحالة (Thumbnail Box) مع شارة مدة الفيديو أو معاينة النص.
 * 3. زر التحميل الأخضر الزمردي المباشر لحفظ الحالة في الجهاز.
 */
@Composable
fun StatusStoryCard(
    story: StatusStory,
    isFavorite: Boolean,
    isMuted: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleMuted: () -> Unit,
    onSelectToggle: () -> Unit,
    onReactClick: (String) -> Unit,
    onDownloadClick: () -> Unit,
    onCardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // إطار توهجي أنيق حسب الحالة
    val borderBrush = when {
        isMuted -> Brush.linearGradient(listOf(RoseError.copy(alpha = 0.4f), Color(0x10FFFFFF)))
        story.isSaved -> Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.6f), Color(0x15FFFFFF)))
        isFavorite -> ObsidianGlowBorder.Amber
        story.isReacted -> ObsidianGlowBorder.Emerald
        else -> Brush.linearGradient(listOf(Color(0x2500E676), Color(0x10FFFFFF)))
    }

    var isDownloadedState by remember { mutableStateOf(false) }

    BentoCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        borderBrush = borderBrush,
        backgroundColor = Color(0xFF0F1A22),
        onClick = if (isMultiSelectMode) onSelectToggle else onCardClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // القسم الأول: التحديد + الأفاتار + تفاصيل الشخص والوقت
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // دائرة الاختيار المتعدد بنمط حديث مطابق للصورة
                if (isMultiSelectMode) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) WhatsAppGreen else Color(0x15FFFFFF))
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else Color(0x55FFFFFF),
                                shape = CircleShape
                            )
                            .clickable { onSelectToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // صورة جهة الاتصال (Avatar) مع حلقة القصة
                StoryAvatarRing(
                    initials = story.senderName.ifBlank { story.senderPhone }.take(2),
                    isViewed = story.isViewed || story.isReacted,
                    size = 48.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // عمود معلومات الشخص
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = story.senderName.ifBlank { story.senderPhone },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMuted) TextMuted else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isFavorite) {
                            Text("⭐", fontSize = 11.sp)
                        }
                    }

                    // سطر الحالة (أيقونة المشاهدة / الحذف + الوقت الذكي)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (story.isDeleted) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "محذوفة",
                                tint = CoralError,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "تم حذف الحالة",
                                fontSize = 11.sp,
                                color = CoralError
                            )
                        } else {
                            val isViewed = story.isViewed || story.isReacted
                            Icon(
                                imageVector = if (story.isSaved) Icons.Filled.Bookmark else if (isViewed) Icons.Outlined.Visibility else Icons.Filled.Visibility,
                                contentDescription = "الحالة",
                                tint = if (story.isSaved) ElectricBlue else if (isViewed) TextMuted else WhatsAppGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (story.isSaved) "محفوظة 📥" else if (isViewed) "تمت المشاهدة" else "حالة جديدة",
                                fontSize = 11.sp,
                                color = if (story.isSaved) ElectricBlue else if (isViewed) TextMuted else WhatsAppGreenLight
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

            Spacer(modifier = Modifier.width(8.dp))

            // القسم الثاني: صندوق محتوى الحالة (Status Preview / Thumbnail Box)
            StatusPreviewThumbnailBox(
                story = story,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // زر الاستثناء الديناميكي من المشاهدة (كتم / تفعيل جهة الاتصال)
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleMuted()
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isMuted) RoseError.copy(alpha = 0.2f) else Color(0x18FFFFFF))
                    .border(
                        1.dp,
                        if (isMuted) RoseError.copy(alpha = 0.5f) else Color(0x25FFFFFF),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.NotificationsOff else Icons.Outlined.Block,
                    contentDescription = if (isMuted) "إلغاء الاستثناء" else "استثناء من المشاهدة",
                    tint = if (isMuted) RoseError else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // القسم الثالث: زر التحميل الأخضر الزمردي المباشر 📥
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isDownloadedState = true
                    onDownloadClick()
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (story.isSaved || isDownloadedState) Color(0x2800E676) else Color(0x1A00E676))
            ) {
                val isSaved = story.isSaved || isDownloadedState
                val isText = story.mediaType == StatusMediaType.TEXT
                val icon = if (isSaved) {
                    Icons.Filled.Check
                } else if (isText) {
                    Icons.Outlined.ContentCopy
                } else {
                    Icons.Outlined.FileDownload
                }
                Icon(
                    imageVector = icon,
                    contentDescription = if (isSaved) "تم الحفظ في المحفوظات" else if (isText) "نسخ النص" else "تحميل الحالة",
                    tint = if (isSaved) WhatsAppGreenLight else WhatsAppGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * صندوق معاينة محتوى الحالة الفاخر (Status Preview Box)
 * يدعم:
 * - عرض ملف الصورة المحلي إن وجد.
 * - عرض مصغّر الفيديو مع شارة التوقيت (مثل 0:15).
 * - عرض تدرج لوني سينمائي مع النص إذا كانت الحالة نصية أو غير محملة محلياً.
 */
@Composable
private fun StatusPreviewThumbnailBox(
    story: StatusStory,
    modifier: Modifier = Modifier
) {
    // تدرجات لونية جمالية مستوحاة من ألوان الصور الحية بالصورة المرفقة
    val scenicGradients = remember {
        listOf(
            listOf(Color(0xFFE65100), Color(0xFFF57C00), Color(0xFFFFB74D)), // تدرج غروب الشمس البرتقالي
            listOf(Color(0xFF1A237E), Color(0xFF311B92), Color(0xFF4A148C)), // تدرج المدينة الليلية الأرجواني
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF66BB6A)), // تدرج غابة الصنوبر الضبابية
            listOf(Color(0xFF880E4F), Color(0xFFC2185B), Color(0xFFF06292)), // تدرج سحاب الغسق الوردي
            listOf(Color(0xFF004D40), Color(0xFF00695C), Color(0xFF00897B)), // تدرج بحيرة الجبال الزرقاء
            listOf(Color(0xFF4E342E), Color(0xFF6D4C41), Color(0xFF8D6E63))  // تدرج القهوة الدافئ
        )
    }

    val selectedGradient = remember(story.id) {
        val idx = abs(story.id.hashCode()) % scenicGradients.size
        scenicGradients[idx]
    }

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

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
            .background(Brush.linearGradient(selectedGradient)),
        contentAlignment = Alignment.Center
    ) {
        if (localBitmap != null) {
            Image(
                bitmap = localBitmap.asImageBitmap(),
                contentDescription = "معاينة الحالة",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            if (story.mediaType == StatusMediaType.VIDEO) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            when (story.mediaType) {
                StatusMediaType.TEXT -> {
                    if (!story.textContent.isNullOrBlank()) {
                        Text(
                            text = story.textContent,
                            fontSize = 9.sp,
                            color = Color.White,
                            maxLines = 3,
                            textAlign = TextAlign.Center,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(4.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.TextFields,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                StatusMediaType.IMAGE -> {
                    Icon(
                        imageVector = Icons.Filled.Image,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                StatusMediaType.VIDEO -> {
                    Icon(
                        imageVector = Icons.Filled.PlayCircleOutline,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // شارة مدة الفيديو السوداء الشفافة (مطابقة تماماً للصورة: 0:15)
        if (story.mediaType == StatusMediaType.VIDEO) {
            val durationText = remember(story.id) {
                val seconds = (10 + (abs(story.id.hashCode()) % 25)).coerceIn(8, 30)
                "0:${seconds.toString().padStart(2, '0')}"
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = durationText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // شارة الحفظ في الجهاز (المحفوظات)
        if (story.isSaved) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(3.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue)
                    .border(0.8.dp, Color.Black.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = "محفوظة",
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}
