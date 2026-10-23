package com.whatsup.automation.presentation.screens.update.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.UpdateType
import com.whatsup.automation.presentation.theme.*

@Composable
fun UpdateTypeBadge(type: UpdateType) {
    val (label, bg, fg) = when (type) {
        UpdateType.MANDATORY -> Triple("تحديث إجباري حاسم", RoseError.copy(alpha = 0.2f), RoseError)
        UpdateType.CRITICAL_SECURITY -> Triple("تحديث أمني حرج", RoseError.copy(alpha = 0.2f), RoseError)
        UpdateType.SILENT_HOTFIX -> Triple("إصلاح سريع", NeonPurple.copy(alpha = 0.2f), NeonPurple)
        UpdateType.OPTIONAL -> Triple("تحديث متاح", WhatsAppGreen.copy(alpha = 0.2f), WhatsAppGreenLight)
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
fun VersionCompareBadge(
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
            Text(
                text = "v$currentVersion",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
        }

        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("الإصدار الجديد", fontSize = 10.5.sp, color = TextMuted)
            Text(
                text = "v$newVersion",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppGreen,
                fontFamily = FontFamily.Monospace
            )
        }

        Box(modifier = Modifier.size(1.dp, 24.dp).background(GlassBorder))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("حجم الحزمة", fontSize = 10.5.sp, color = TextMuted)
            Text(
                text = fileSize,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyberCyan,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ChangelogBox(
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
            text = changelog.ifBlank { "تحسينات شاملة على الأداء واستقرار المحرك وحصانة الحساب." },
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}
