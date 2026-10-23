package com.whatsup.automation.presentation.screens.pairing.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.domain.model.CountryCode
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.PhoneInputWithCountrySelector
import com.whatsup.automation.presentation.components.SafeActionButton
import com.whatsup.automation.presentation.components.SafeButtonStyle
import com.whatsup.automation.presentation.screens.pairing.PairingUiState
import com.whatsup.automation.presentation.theme.*

@Composable
fun PairingCodeCard(
    uiState: PairingUiState,
    onCountrySelected: (CountryCode) -> Unit,
    onNationalNumberChanged: (String) -> Unit,
    onRequestPairingCode: () -> Unit,
    onOpenWhatsApp: () -> Unit,
    onCopyCode: (String) -> Unit,
    onConfirmSuccess: () -> Unit,
    onDismissError: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onCleanStaleData: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "أدخل رقم هاتفك لتوليد كود الاقتران المكون من 8 خانات:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            // حقل إدخال رقم الهاتف مع محدد رمز الدولة باليسار وافتراض اليمن
            PhoneInputWithCountrySelector(
                selectedCountry = uiState.selectedCountry,
                nationalNumber = uiState.nationalPhoneNumber,
                onCountrySelected = onCountrySelected,
                onNationalNumberChanged = onNationalNumberChanged,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            )

            if (uiState.nationalPhoneNumber.isNotBlank()) {
                val cleanDigits = uiState.nationalPhoneNumber.filter { it.isDigit() }.trimStart('0')
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "الرقم الدولي الذي سيتم ربطه: ${uiState.selectedCountry.dialCode} $cleanDigits",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = WhatsAppGreenLight,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // دليل الخطوات السريع للربط
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "خطوات ربط الحساب على هذا الهاتف:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppGreenLight
                    )
                    Text("1. اضغط \"طلب كود الاقتران\".", fontSize = 11.sp, color = TextSecondary)
                    Text("2. انسخ الكود أو اضغط \"فتح واتساب لإدخال الكود\".", fontSize = 11.sp, color = TextSecondary)
                    Text(
                        "3. في واتساب، اختر: (الأجهزة المرتبطة) ➔ (ربط جهاز) ➔ (الربط باستخدام رقم الهاتف) والصق الكود.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            // زر طلب كود الاقتران مع حماية النقر المزدوج
            SafeActionButton(
                text = when {
                    uiState.isLoading -> "جارٍ طلب الكود من واتساب..."
                    uiState.isCleaningData -> "جارٍ تنظيف البيانات العالقة..."
                    uiState.retryCooldownSeconds > 0 -> "انتظر (${uiState.retryCooldownSeconds} ثانية)..."
                    else -> "طلب كود الاقتران (8 أرقام)"
                },
                icon = Icons.Filled.Key,
                onClick = onRequestPairingCode,
                enabled = !uiState.isLoading && !uiState.isCleaningData && uiState.retryCooldownSeconds <= 0,
                style = SafeButtonStyle.Primary,
                modifier = Modifier.fillMaxWidth()
            )

            // شريط عداد الانتظار التنازلي لحماية الحساب من قيود واتساب
            if (uiState.retryCooldownSeconds > 0) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarning.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { (uiState.retryCooldownSeconds.toFloat() / 60f).coerceIn(0.05f, 1f) },
                            modifier = Modifier.size(24.dp),
                            color = AmberWarning,
                            strokeWidth = 2.5.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "فترة انتظار لحماية الحساب (${uiState.retryCooldownSeconds} ثانية متبقية)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarning
                            )
                            Text(
                                text = "تمنع خوادم واتساب طلب الأكواد المتتالية السريعة. يرجى الانتظار حتى انتهاء العداد لإعادة المحاولة بأمان.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // زر تنظيف الجلسة والبيانات المعلقة
            if (onCleanStaleData != null) {
                SafeActionButton(
                    text = if (uiState.isCleaningData) "جارٍ تنظيف البيانات وإعادة الضبط..." else "تنظيف البيانات والجلسة المعلقة 🧹",
                    icon = Icons.Filled.DeleteSweep,
                    onClick = onCleanStaleData,
                    enabled = !uiState.isLoading && !uiState.isCleaningData,
                    style = SafeButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // عرض رسالة الخطأ التفاعلية مع زر إعادة المحاولة
            uiState.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = RoseError.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WarningAmber,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "تنبيه أثناء الاقتران",
                                    color = RoseError,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (onDismissError != null) {
                                IconButton(
                                    onClick = onDismissError,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = error,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onRetry ?: onRequestPairingCode,
                                enabled = uiState.retryCooldownSeconds <= 0 && !uiState.isLoading && !uiState.isCleaningData,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (uiState.retryCooldownSeconds > 0) TextMuted else WhatsAppGreenLight
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.retryCooldownSeconds > 0) "انتظر (${uiState.retryCooldownSeconds}ث)" else "إعادة طلب الكود",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // عرض الكود عند توليده مع المؤقت التنازلي والأزرار المباشرة
            uiState.pairingCode?.let { code ->
                val formattedCode = if (code.length == 8 && !code.contains("-")) {
                    "${code.take(4)}-${code.drop(4)}"
                } else {
                    code
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF070E1A))
                        .border(1.dp, WhatsAppGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "كود الاقتران الصادر:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            if (uiState.codeCountdownSeconds > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.codeCountdownSeconds <= 15) RoseError.copy(alpha = 0.2f) else AmberWarning.copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (uiState.codeCountdownSeconds <= 15) RoseError.copy(alpha = 0.4f) else AmberWarning.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Timer,
                                            contentDescription = null,
                                            tint = if (uiState.codeCountdownSeconds <= 15) RoseError else AmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "صالح لمدة: ${uiState.codeCountdownSeconds} ث",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (uiState.codeCountdownSeconds <= 15) RoseError else AmberWarning,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = formattedCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            letterSpacing = 4.sp
                        )

                        // زر فتح واتساب وزر نسخ الكود
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onOpenWhatsApp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = DarkBgPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فتح واتساب لإدخال الكود",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkBgPrimary
                                )
                            }

                            OutlinedButton(
                                onClick = { onCopyCode(code) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخ كود الاقتران", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onConfirmSuccess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تأكيد اكتمال الاقتران بنجاح", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
