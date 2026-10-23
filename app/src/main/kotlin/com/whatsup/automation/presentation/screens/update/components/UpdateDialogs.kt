package com.whatsup.automation.presentation.screens.update.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateUiState
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.theme.*

/**
 * نافذة حوار التحديث الاختياري المباشرة.
 */
@Composable
fun OptionalUpdateDialog(
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تحديث جديد متاح",
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
                            color = WhatsAppGreenLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = GlassBorderSubtle, thickness = 1.dp)

                ChangelogBox(
                    changelog = updateInfo.changelogAr,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            Text("تحديث الآن", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color(0x30FFFFFF), Color(0x30FFFFFF)))
                        )
                    ) {
                        Text("لاحقاً", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار الخطأ عند فشل الفحص أو التنزيل.
 */
@Composable
fun UpdateErrorDialog(
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (state.canUseMirror) {
                        OutlinedButton(
                            onClick = onUseMirror,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("التنزيل من الخادم البديل", color = CyberCyan)
                        }
                    }

                    if (!state.isMandatory) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
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
fun UpToDateDialog(
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
                    text = "التطبيق محدث لأحدث إصدار",
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
