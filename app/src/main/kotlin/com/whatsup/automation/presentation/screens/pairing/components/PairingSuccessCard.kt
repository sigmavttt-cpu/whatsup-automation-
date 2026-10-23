package com.whatsup.automation.presentation.screens.pairing.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.GlassmorphicCard
import com.whatsup.automation.presentation.components.LuxuryOutlinedButton
import com.whatsup.automation.presentation.theme.*

@Composable
fun PairingSuccessCard(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    phoneNumber: String? = null
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "تأكيد قطع الاتصال",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من رغبتك في تسجيل الخروج وإلغاء ربط هذا الحساب؟ ستتوقف أتمتة الردود وقراءة الحالات فوراً.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("نعم، إلغاء الربط", color = RoseError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        borderBrush = Brush.linearGradient(listOf(WhatsAppGreen, CyberCyan))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(WhatsAppGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = WhatsAppGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "الحساب متصل ويعمل بنجاح",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            if (!phoneNumber.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WhatsAppGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = phoneNumber,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WhatsAppGreenLight
                        )
                    }
                }
            }

            Text(
                text = "محرك الأتمتة والتقاط الرسائل نشط الآن 24/7. يتم فحص كل رسالة واردة والرد عليها وحفظ الأسماء فوراً.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            LuxuryOutlinedButton(
                text = "إلغاء الربط / تسجيل الخروج",
                onClick = { showLogoutDialog = true },
                icon = Icons.Filled.Logout,
                borderBrush = Brush.linearGradient(listOf(RoseError.copy(alpha = 0.5f), RoseError)),
                textColor = RoseError,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        }
    }
}
