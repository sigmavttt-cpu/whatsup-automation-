package com.whatsup.automation.presentation.screens.activation.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassDisabled
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.data.security.LicenseState
import com.whatsup.automation.presentation.theme.*

@Composable
fun LicenseStatusBadge(
    licenseState: LicenseState,
    modifier: Modifier = Modifier
) {
    val isExpired = licenseState is LicenseState.Expired
    val isBlocked = licenseState is LicenseState.Blocked

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (isExpired || isBlocked) RoseError.copy(alpha = 0.15f)
                else WhatsAppGreen.copy(alpha = 0.15f)
            )
            .border(
                1.dp,
                if (isExpired || isBlocked) RoseError.copy(alpha = 0.4f)
                else WhatsAppGreen.copy(alpha = 0.4f),
                CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isExpired || isBlocked) RoseError else WhatsAppGreen)
            )
            Text(
                text = when {
                    isExpired -> "انتهت صلاحية الاشتراك"
                    isBlocked -> "تم إيقاف الاشتراك"
                    else -> "تفعيل الاشتراك مطلوب"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isExpired || isBlocked) RoseError else WhatsAppGreenLight
            )
        }
    }
}

@Composable
fun DeviceFingerprintCard(
    fingerprint: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkBgSecondary)
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "بصمة جهازك (Device ID):",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (fingerprint.length > 20) "${fingerprint.take(10)}...${fingerprint.takeLast(10)}" else fingerprint,
                    fontSize = 12.sp,
                    color = WhatsAppGreenLight,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Device ID", fingerprint)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "تم نسخ معرّف الجهاز بنجاح", Toast.LENGTH_SHORT).show()
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = WhatsAppGreen
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "نسخ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WhatsAppGreen
                )
            }
        }
    }
}
