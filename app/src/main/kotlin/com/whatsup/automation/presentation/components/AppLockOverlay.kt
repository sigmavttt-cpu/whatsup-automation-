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
import androidx.compose.material.icons.filled.Check
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
                .background(Color(0xFF060D12))
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
            .sizeIn(minWidth = 54.dp, minHeight = 54.dp, maxWidth = 68.dp, maxHeight = 68.dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(
                if (isSpecial) Color.Transparent
                else Color(0xCC0F1A22)
            )
            .border(
                width = 1.dp,
                color = if (isSpecial) Color.Transparent else Color(0x3300E676),
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
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            else -> {
                Text(
                    text = key,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * نافذة ضبط وإعداد قفل التطبيق بالـ PIN والبصمة بنظام احترافي فاخر.
 * تدفق من خطوتين لتعيين وتأكيد الرمز بلوحة أرقام مدمجة ومؤشرات نقاط نيونية متوهجة.
 */
@Composable
fun AppLockSettingsDialog(
    appLockManager: AppLockManager,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isLockCurrentlyEnabled = remember { appLockManager.isLockEnabled() }
    var biometricEnabled by remember { mutableStateOf(appLockManager.isBiometricEnabled()) }

    var step by remember { mutableIntStateOf(1) } // 1: أدخل الرمز الجديد, 2: أكد الرمز
    var tempPin by remember { mutableStateOf("") }
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isSuccess by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD060D12))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 380.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F1A22),
                                Color(0xFF060D12)
                            )
                        )
                    )
                    .border(1.5.dp, Color(0x6600E676), RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // منع الإغلاق عند النقر داخل المحتوى
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // أيقونة القفل النيونية
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0x3300E676), Color(0xCC0F1A22))
                                )
                            )
                            .border(1.2.dp, Color(0x6600E676), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSuccess) Icons.Filled.Check else Icons.Filled.Lock,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isSuccess) "تم تفعيل القفل بنجاح ✨"
                        else if (step == 1) "تعيين رمز قفل جديد (PIN)"
                        else "تأكيد رمز القفل الجديد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isSuccess) "تطبيقك الآن محمي برمز PIN مشفر محلياً"
                        else if (step == 1) "أدخل 4 أرقام لإنشاء رمز القفل"
                        else "أعِد إدخال الرمز لتأكيد الحفظ",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // نقاط رمز الـ PIN التفاعلية (4 نقاط)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val isFilled = index < enteredPin.length
                            val dotColor by animateColorAsState(
                                targetValue = if (isError) Color(0xFFFF5252) else if (isSuccess) WhatsAppGreenLight else if (isFilled) Color(0xFF00E676) else Color(0x33FFFFFF),
                                animationSpec = tween(180),
                                label = "dialogDotColor"
                            )
                            val dotScale by animateFloatAsState(
                                targetValue = if (isFilled) 1.25f else 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dialogDotScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(dotScale)
                                    .clip(CircleShape)
                                    .background(if (isFilled || isSuccess) dotColor else Color.Transparent)
                                    .border(2.dp, dotColor, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // رسالة الخطأ
                    AnimatedVisibility(
                        visible = isError,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Text(
                            text = errorMessage.ifBlank { "الرمزان غير متطابقين، أعد المحاولة" },
                            color = Color(0xFFFF5252),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!isError) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // لوحة المفاتيح الرقمية (Keypad)
                    val keypadRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("", "0", "DEL")
                    )

                    Column(
                        modifier = Modifier.widthIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (row in keypadRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (key in row) {
                                    val isSpecial = key == "" || key == "DEL"
                                    val isEnabled = key.isNotEmpty()

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                            .background(if (isSpecial) Color.Transparent else Color(0xCC0F1A22))
                                            .border(1.dp, if (isSpecial) Color.Transparent else Color(0x3300E676), CircleShape)
                                            .clickable(enabled = isEnabled) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (key == "DEL") {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        isError = false
                                                    }
                                                } else if (key.isNotEmpty()) {
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + key
                                                        enteredPin = next
                                                        if (next.length == 4) {
                                                            if (step == 1) {
                                                                scope.launch {
                                                                    delay(200)
                                                                    tempPin = next
                                                                    enteredPin = ""
                                                                    step = 2
                                                                }
                                                            } else {
                                                                scope.launch {
                                                                    delay(150)
                                                                    if (next == tempPin) {
                                                                        isSuccess = true
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        appLockManager.setPin(next)
                                                                        appLockManager.setBiometricEnabled(biometricEnabled)
                                                                        delay(800)
                                                                        onDismiss()
                                                                    } else {
                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        isError = true
                                                                        errorMessage = "الرمزان غير متطابقين، أعد المحاولة"
                                                                        delay(1000)
                                                                        enteredPin = ""
                                                                        tempPin = ""
                                                                        step = 1
                                                                        isError = false
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "حذف",
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (key.isNotEmpty()) {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // مفتاح تفعيل البصمة الحيوية
                    if (appLockManager.isBiometricAvailable()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1F00E676))
                                .border(1.dp, Color(0x3300E676), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(20.dp))
                                Text("تمكين الفتح بالبصمة", color = Color.White, fontSize = 12.5.sp)
                            }
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
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // أزرار التحكم: تعطيل القفل أو الإلغاء
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLockCurrentlyEnabled) {
                            TextButton(
                                onClick = {
                                    appLockManager.disableLock()
                                    onDismiss()
                                }
                            ) {
                                Text("تعطيل القفل 🔓", color = Color(0xFFFF5252), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        TextButton(onClick = onDismiss) {
                            Text("إلغاء", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
