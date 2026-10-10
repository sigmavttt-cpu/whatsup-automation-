package com.whatsup.automation.presentation.screens.system

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.UpdateUiState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.screens.update.UpdateViewModel
import com.whatsup.automation.presentation.theme.*

/**
 * شاشة إدارة وتدقيق التحديثات المؤسسية (Updates Hub Screen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesHubScreen(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    val currentVersion = viewModel.currentVersionName
    val currentCode = viewModel.currentVersionCode
    val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    val isChecking = uiState is UpdateUiState.Checking
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isChecking,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.checkForUpdates(isManual = true)
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
        // بطاقة ملخص الإصدار الحالي
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = ObsidianGlowBorder.Emerald
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.2f))
                                .border(1.5.dp, WhatsAppGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "WhatsUp Automation Enterprise",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "الإصدار الحالي: v$currentVersion (Build $currentCode)",
                                fontSize = 12.sp,
                                color = WhatsAppGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0x15FFFFFF))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("المعمارية (ABI):", fontSize = 11.sp, color = TextMuted)
                            Text(primaryAbi, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        }
                        Column {
                            Text("قناة التوزيع:", fontSize = 11.sp, color = TextMuted)
                            Text("مستقرة (Production/Stable)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CyberCyan)
                        }
                        Column {
                            Text("حالة الخادم:", fontSize = 11.sp, color = TextMuted)
                            Text("Supabase Online 🟢", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                        }
                    }

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.checkForUpdates(isManual = true)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        enabled = uiState !is UpdateUiState.Checking && uiState !is UpdateUiState.Downloading
                    ) {
                        if (uiState is UpdateUiState.Checking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("جارٍ فحص التحديثات عبر Supabase...", fontSize = 13.sp, color = Color.White)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.White)
                                Text("فحص التحديثات الآن ⚡", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // بطاقة ميزات محرك التحديثات المؤسسي
        item {
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = ObsidianGlowBorder.Cyan
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "مواصفات محرك التحديث الذكي",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    FeatureRow(
                        icon = Icons.Filled.Bolt,
                        title = "تنزيل قطاعي قابل للاستئناف (Resumable Chunked)",
                        description = "استئناف فوري للتحميل عبر Range Headers عند انقطاع الإنترنت أو إغلاق التطبيق."
                    )

                    FeatureRow(
                        icon = Icons.Filled.Lock,
                        title = "التحقق المشفر من سلامة الحزمة (SHA-256 Checksum)",
                        description = "فحص بصمة التشفير الثنائية قبل التثبيت لمنع أي تلف أو تلاعب في الحزم."
                    )

                    FeatureRow(
                        icon = Icons.Filled.SettingsBackupRestore,
                        title = "بروتوكول التهدئة الآمنة (Graceful Teardown)",
                        description = "حفظ ومزامنة قواعد البيانات (WAL Checkpoint) وإيقاف الخدمات بأمان تام قبل التثبيت."
                    )

                    FeatureRow(
                        icon = Icons.Filled.Security,
                        title = "درع التحديثات الإجبارية (FullScreenUpdateShield)",
                        description = "حظر الإصدارات القديمة المعرضة للحظر مع التحديث الذري السلس."
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyberCyan.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
        }
    }
}
