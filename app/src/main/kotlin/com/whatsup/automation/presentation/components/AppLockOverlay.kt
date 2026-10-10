package com.whatsup.automation.presentation.components

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.data.security.AppLockManager
import com.whatsup.automation.presentation.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة قفل التطبيق الزجاجية الفاخرة (Dark Glassmorphism App Lock Overlay).
 * تمنع الوصول للواجهة حتى يتم إدخال رمز الـ PIN أو التحقق بالبصمة.
 */
@Composable
fun AppLockOverlay(
    appLockManager: AppLockManager,
    modifier: Modifier = Modifier
) {
    val isUnlocked by appLockManager.isUnlocked.collectAsState()
    val isLockEnabled = remember(isUnlocked) { appLockManager.isLockEnabled() }

    if (isLockEnabled && !isUnlocked) {
        val context = LocalContext.current
        val activity = context as? Activity
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()

        var enteredPin by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        // تشغيل البصمة تلقائياً عند ظهور شاشة القفل إذا كانت مدعومة ومفعلة
        LaunchedEffect(Unit) {
            if (activity != null && appLockManager.isBiometricAvailable() && appLockManager.isBiometricEnabled()) {
                delay(300)
                appLockManager.promptBiometricAuth(
                    activity = activity,
                    onSuccess = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onError = { err ->
                        // خطأ هادئ لا يمنع إدخال الـ PIN
                    }
                )
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}, // اعتراض النقرات لمنع التفاعل مع الواجهة الخلفية
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // أيقونة الدرع المشفر
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    WhatsAppGreen.copy(alpha = 0.15f),
                                    WhatsAppGreenLight.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(1.5.dp, WhatsAppGreen.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "قفل الأمان",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "واتس آب أوتوميشن مشفر 🔒",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "أدخل رمز PIN أو استخدم البصمة لفتح التطبيق والخزينة",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // نقاط رمز PIN (4 نقاط)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        val dotColor by animateColorAsState(
                            targetValue = if (isError) Color(0xFFFF5252) else if (isFilled) Color(0xFF00E676) else Color(0x33FFFFFF),
                            animationSpec = tween(200), label = "dotColor"
                        )
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1.0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "dotScale"
                        )

                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(if (isFilled) dotColor else Color.Transparent)
                                .border(2.dp, dotColor, CircleShape)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // رسالة الخطأ
                AnimatedVisibility(
                    visible = isError,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Text(
                        text = errorMessage.ifBlank { "رمز PIN غير صحيح، حاول ثانية" },
                        color = Color(0xFFFF5252),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (!isError) {
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // لوحة المفاتيح الرقمية (Keypad)
                val keypadRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                Column(
                    modifier = Modifier.widthIn(max = 290.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (row in keypadRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (key in row) {
                                KeypadButton(
                                    key = key,
                                    isBiometricAvailable = appLockManager.isBiometricAvailable() && appLockManager.isBiometricEnabled(),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when (key) {
                                            "DEL" -> {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    isError = false
                                                }
                                            }
                                            "BIO" -> {
                                                if (activity != null) {
                                                    appLockManager.promptBiometricAuth(
                                                        activity = activity,
                                                        onSuccess = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        },
                                                        onError = { err ->
                                                            isError = true
                                                            errorMessage = err
                                                            scope.launch {
                                                                delay(2000)
                                                                isError = false
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                            else -> {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                    isError = false
                                                    if (enteredPin.length == 4) {
                                                        // التحقق من صحة الـ PIN
                                                        val success = appLockManager.verifyPin(enteredPin)
                                                        if (success) {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        } else {
                                                            isError = true
                                                            errorMessage = "رمز PIN غير صحيح ❌"
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            scope.launch {
                                                                delay(600)
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    isBiometricAvailable: Boolean,
    onClick: () -> Unit
) {
    val isSpecial = key == "BIO" || key == "DEL"
    val isEnabled = key != "BIO" || isBiometricAvailable

    Box(
        modifier = Modifier
            .sizeIn(minWidth = 52.dp, minHeight = 52.dp, maxWidth = 68.dp, maxHeight = 68.dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isSpecial) Color.Transparent
                else Color(0xFFF1F5F9)
            )
            .border(
                width = 1.dp,
                color = if (isSpecial) Color.Transparent else Color(0x3300A884),
                shape = CircleShape
            )
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (key) {
            "DEL" -> {
                Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "حذف",
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
            "BIO" -> {
                if (isBiometricAvailable) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "البصمة",
                        tint = WhatsAppGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = key,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}

/**
 * نافذة ضبط وإعداد قفل التطبيق بالـ PIN والبصمة.
 */
@Composable
fun AppLockSettingsDialog(
    appLockManager: AppLockManager,
    onDismiss: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) } // 1: enter new, 2: confirm
    var error by remember { mutableStateOf("") }
    val isLockCurrentlyEnabled = remember { appLockManager.isLockEnabled() }
    var biometricEnabled by remember { mutableStateOf(appLockManager.isBiometricEnabled()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFFFFFFF),
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = WhatsAppGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("حماية وقفل التطبيق والخزينة", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (step == 1) "أدخل رمز PIN جديد مكون من 4 أرقام:" else "أكّد رمز الـ PIN الجديد:",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = if (step == 1) newPin else confirmPin,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            if (step == 1) newPin = input else confirmPin = input
                            error = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color(0x3300A884),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    placeholder = { Text("••••", color = TextMuted) }
                )

                if (appLockManager.isBiometricAvailable()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تمكين فتح التطبيق بالبصمة", color = TextPrimary, fontSize = 14.sp)
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                appLockManager.setBiometricEnabled(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = WhatsAppGreen
                            )
                        )
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = Color(0xFFFF5252), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (step == 1) {
                        if (newPin.length == 4) {
                            step = 2
                        } else {
                            error = "الرمز يجب أن يتكون من 4 أرقام"
                        }
                    } else {
                        if (confirmPin == newPin) {
                            appLockManager.setPin(newPin)
                            appLockManager.setBiometricEnabled(biometricEnabled)
                            onDismiss()
                        } else {
                            error = "الرمزان غير متطابقين، حاول مجدداً"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
            ) {
                Text(if (step == 1) "التالي" else "تأكيد وتفعيل", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            if (isLockCurrentlyEnabled) {
                TextButton(
                    onClick = {
                        appLockManager.disableLock()
                        onDismiss()
                    }
                ) {
                    Text("تعطيل القفل 🔓", color = Color(0xFFFF5252))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = Color.Gray)
                }
            }
        }
    )
}
