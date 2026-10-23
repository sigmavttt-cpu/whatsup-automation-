package com.whatsup.automation.presentation.screens.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.presentation.theme.*

/**
 * نافذة حوار إدارة نطاق حفظ وتتبع المحذوفات في الخزينة:
 * - اختيار حفظ الجميع (ALL)
 * - أو اختيار جهات اتصال محددة فقط (SELECTED) مع إمكانية إضافة وإزالة الأرقام
 */
@Composable
fun VaultTrackingConfigDialog(
    currentMode: String, // "ALL" or "SELECTED"
    trackedPhones: Set<String>,
    onDismiss: () -> Unit,
    onSaveMode: (String) -> Unit,
    onAddPhone: (String) -> Unit,
    onRemovePhone: (String) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    var newPhoneInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.78f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarkBgPrimary)
                .border(1.2.dp, WhatsAppGreen.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.16f))
                                .border(1.dp, WhatsAppGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                text = "تحديد نطاق حفظ المحذوفات",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "اختر من يتم تسجيل محذوفاته وتعديلاته",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }

                // Options: All vs Selected
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Option 1: ALL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedMode == "ALL") WhatsAppGreen.copy(alpha = 0.12f) else DarkSurface)
                            .border(
                                1.2.dp,
                                if (selectedMode == "ALL") WhatsAppGreen else Color(0x22FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedMode = "ALL"
                                onSaveMode("ALL")
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedMode == "ALL",
                                onClick = {
                                    selectedMode = "ALL"
                                    onSaveMode("ALL")
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WhatsAppGreen,
                                    unselectedColor = TextMuted
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حفظ محذوفات الكل (الجميع)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "يتم التقاط وحفظ الرسائل المحذوفة لجميع المحادثات الواردة تلقائياً",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    // Option 2: SELECTED
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selectedMode == "SELECTED") CyberCyan.copy(alpha = 0.12f) else DarkSurface)
                            .border(
                                1.2.dp,
                                if (selectedMode == "SELECTED") CyberCyan else Color(0x22FFFFFF),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedMode = "SELECTED"
                                onSaveMode("SELECTED")
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = selectedMode == "SELECTED",
                                onClick = {
                                    selectedMode = "SELECTED"
                                    onSaveMode("SELECTED")
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = CyberCyan,
                                    unselectedColor = TextMuted
                                )
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "أشخاص محددون فقط",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "تسجيل وحفظ المحذوفات فقط للأرقام المضافة في القائمة أدناه وتجاهل البقية",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // If SELECTED: show add phone input and list
                if (selectedMode == "SELECTED") {
                    Text(
                        text = "قائمة الأرقام المراقبة (${trackedPhones.size}):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )

                    // Input row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newPhoneInput,
                            onValueChange = {
                                newPhoneInput = it
                                inputError = null
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("مثال: 967771234567", fontSize = 11.5.sp, color = TextMuted) },
                            singleLine = true,
                            isError = inputError != null,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Color(0x3300B0FF),
                                focusedContainerColor = DarkSurface,
                                unfocusedContainerColor = DarkSurface,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                val clean = newPhoneInput.replace("[^0-9]".toRegex(), "")
                                if (clean.length in 7..15) {
                                    onAddPhone(clean)
                                    newPhoneInput = ""
                                    inputError = null
                                } else {
                                    inputError = "أدخل رقماً صحيحاً (7-15 رقماً)"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "إضافة", tint = Color.Black, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("إضافة", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = Color(0xFFFF5252),
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Tracked phones list
                    if (trackedPhones.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم تقم بإضافة أي أرقام بعد.\nأضف أرقام الأشخاص المراد تتبعهم.",
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(trackedPhones.toList()) { phone ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSurface)
                                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Person, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                            androidx.compose.runtime.CompositionLocalProvider(
                                                androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                                            ) {
                                                Text(
                                                    text = "+$phone",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { onRemovePhone(phone) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("تم وحفظ الإعدادات", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
