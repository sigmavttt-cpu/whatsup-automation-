package com.whatsup.automation.presentation.screens.statuses.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppTooltipBox
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.screens.statuses.StatusSubTab
import com.whatsup.automation.presentation.theme.*

/**
 * زر التبويب الفرعي مع شارة العدد بتصميم نظيف بدون إيموجي.
 */
@Composable
fun StatusTabButton(
    title: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    tooltipText: String = "",
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    AppTooltipBox(tooltipText = tooltipText) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) accentColor.copy(alpha = 0.18f) else DarkSurface.copy(alpha = 0.5f))
                .border(1.dp, if (isSelected) accentColor else Color(0x18FFFFFF), RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
                .padding(vertical = 10.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else TextSecondary
                )
                Text(
                    text = "$count",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) TextPrimary else TextMuted
                )
            }
        }
    }
}

/**
 * زر فلتر الترتيب الصغير.
 */
@Composable
fun StatusSortChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) WhatsAppGreen.copy(alpha = 0.2f) else Color(0x10FFFFFF))
            .border(1.dp, if (isSelected) WhatsAppGreen else Color(0x20FFFFFF), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = if (isSelected) WhatsAppGreen else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private data class EmptyStatusData(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val ctaText: String,
    val ctaAction: () -> Unit
)

/**
 * بطاقة الحالات الفارغة الإرشادية مع أزرار الإجراء المباشر.
 */
@Composable
fun EmptyStatusesCard(
    subTab: StatusSubTab,
    hasSearch: Boolean,
    onClearSearch: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchToNew: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        borderBrush = ObsidianGlowBorder.None
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val config = when {
                hasSearch -> EmptyStatusData(
                    icon = Icons.Filled.SearchOff,
                    title = "لا توجد نتائج مطابقة للبحث",
                    desc = "جرّب البحث باسم آخر أو رقم هاتف مختلف.",
                    ctaText = "مسح البحث",
                    ctaAction = onClearSearch
                )
                subTab == StatusSubTab.NEW -> EmptyStatusData(
                    icon = Icons.Filled.CheckCircleOutline,
                    title = "لا توجد حالات جديدة حالياً",
                    desc = "تمت مشاهدة والتفاعل مع جميع الحالات المرصودة حتى الآن.",
                    ctaText = "تحديث الحالات الآن",
                    ctaAction = onRefresh
                )
                subTab == StatusSubTab.VIEWED -> EmptyStatusData(
                    icon = Icons.Filled.Visibility,
                    title = "لا توجد حالات تمت مشاهدتها بعد",
                    desc = "ستظهر هنا الحالات فور مشاهدتها أو التفاعل معها.",
                    ctaText = "العودة للحالات الجديدة",
                    ctaAction = onSwitchToNew
                )
                subTab == StatusSubTab.SAVED -> EmptyStatusData(
                    icon = Icons.Filled.BookmarkBorder,
                    title = "لا توجد حالات محفوظة بعد",
                    desc = "اضغط على زر التحميل بجانب أي حالة لحفظها في جهازك وتصفحها هنا في أي وقت.",
                    ctaText = "استعراض الحالات الجديدة",
                    ctaAction = onSwitchToNew
                )
                subTab == StatusSubTab.DELETED_TEMP -> EmptyStatusData(
                    icon = Icons.Filled.HistoryToggleOff,
                    title = "لا توجد حالات محذوفة مؤقتاً",
                    desc = "الحالات التي يحذفها أصحابها خلال 24 ساعة تُحفظ هنا تلقائياً لخصوصيتك.",
                    ctaText = "تحديث الحالات",
                    ctaAction = onRefresh
                )
                subTab == StatusSubTab.FAVORITES -> EmptyStatusData(
                    icon = Icons.Filled.StarOutline,
                    title = "قائمة المفضلة فارغة",
                    desc = "اضغط على رمز النجمة بجانب أي جهة اتصال لتفضيل حالاتها دائماً.",
                    ctaText = "استعراض الحالات الجديدة",
                    ctaAction = onSwitchToNew
                )
                else -> EmptyStatusData(
                    icon = Icons.Filled.DoneAll,
                    title = "لا توجد حالات في هذا القسم",
                    desc = "تصفح الأقسام الأخرى أو قم بتحديث القائمة.",
                    ctaText = "العودة للجديدة",
                    ctaAction = onSwitchToNew
                )
            }

            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(44.dp)
            )

            Text(
                text = config.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = config.desc,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    config.ctaAction()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text(
                    text = config.ctaText,
                    fontSize = 12.sp,
                    color = DarkBgPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
