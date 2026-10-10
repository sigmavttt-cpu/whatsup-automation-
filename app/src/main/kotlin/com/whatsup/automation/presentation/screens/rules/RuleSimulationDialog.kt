package com.whatsup.automation.presentation.screens.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.domain.usecase.SimulationResult
import com.whatsup.automation.presentation.components.LiquidGradientButton
import com.whatsup.automation.presentation.theme.*

@Composable
fun RuleSimulationDialog(
    simulationResult: SimulationResult?,
    isSimulating: Boolean,
    onDismiss: () -> Unit,
    onRunSimulation: (message: String, phone: String) -> Unit
) {
    var testMessage by remember { mutableStateOf("السلام عليكم، حياك الله") }
    var testPhone by remember { mutableStateOf("+967770000000") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBgSecondary)
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyberCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "محاكي اختبار القواعد",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "اختبار فوري آمن بدون إرسال رسائل حقيقية",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary)
                    }
                }

                HorizontalDivider(color = GlassBorder)

                // Input Message
                OutlinedTextField(
                    value = testMessage,
                    onValueChange = { testMessage = it },
                    label = { Text("نص الرسالة التجريبية") },
                    placeholder = { Text("اكتب نصاً لاختبار تطابقه مع القواعد...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Input Phone
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    label = { Text("رقم هاتف المرسل الافتراضي") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = CyberCyan,
                        unfocusedLabelColor = TextSecondary
                    ),
                    shape = RoundedCornerShape(14.dp)
                )

                // Run Button
                LiquidGradientButton(
                    text = if (isSimulating) "جاري الاختبار..." else "🧪 تشغيل المحاكاة الآن",
                    onClick = {
                        if (testMessage.isNotBlank()) {
                            onRunSimulation(testMessage, testPhone)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSimulating && testMessage.isNotBlank()
                )

                // Simulation Output
                if (simulationResult != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    simulationResult.isMatched -> WhatsAppGreen.copy(alpha = 0.1f)
                                    simulationResult.isNegated || simulationResult.isQuotedSpeech -> AmberWarning.copy(alpha = 0.1f)
                                    else -> RoseError.copy(alpha = 0.1f)
                                }
                            )
                            .border(
                                1.dp,
                                when {
                                    simulationResult.isMatched -> WhatsAppGreen.copy(alpha = 0.4f)
                                    simulationResult.isNegated || simulationResult.isQuotedSpeech -> AmberWarning.copy(alpha = 0.4f)
                                    else -> RoseError.copy(alpha = 0.4f)
                                },
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        simulationResult.isMatched -> Icons.Default.CheckCircle
                                        simulationResult.isNegated || simulationResult.isQuotedSpeech -> Icons.Default.Warning
                                        else -> Icons.Default.Cancel
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        simulationResult.isMatched -> WhatsAppGreen
                                        simulationResult.isNegated || simulationResult.isQuotedSpeech -> AmberWarning
                                        else -> RoseError
                                    }
                                )
                                Text(
                                    text = when {
                                        simulationResult.isMatched -> "تم التطابق بنجاح!"
                                        simulationResult.isNegated -> "تم حظر الرد (درع النفي نشط)"
                                        simulationResult.isQuotedSpeech -> "تم التجاهل (كلام منقول أو سرد)"
                                        else -> "لم تتطابق أي قاعدة مفعلة"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 15.sp
                                )
                            }

                            if (simulationResult.isMatched) {
                                Text(
                                    text = "📌 القاعدة: ${simulationResult.matchedRuleName} (الأولوية: #${simulationResult.matchedRulePriority})",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (!simulationResult.simulatedReply.isNullOrBlank()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkBgPrimary)
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = "💬 نص الرد المتوقع (بعد Spintax):",
                                            fontSize = 11.sp,
                                            color = WhatsAppGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = simulationResult.simulatedReply,
                                            fontSize = 13.sp,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                if (!simulationResult.extractedName.isNullOrBlank()) {
                                    Text(
                                        text = "👤 الاسم المستخرج: ${simulationResult.extractedName}",
                                        fontSize = 12.sp,
                                        color = CyberCyan
                                    )
                                }
                            }

                            // Diagnostics
                            if (simulationResult.diagnostics.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "سجل التتبع والتشخيص:",
                                        fontSize = 10.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    simulationResult.diagnostics.forEach { diag ->
                                        Text(
                                            text = "• $diag",
                                            fontSize = 10.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
