package com.whatsup.automation.presentation.screens.auth.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.whatsup.automation.data.security.LicenseState
import com.whatsup.automation.presentation.screens.activation.ActivationViewModel
import com.whatsup.automation.presentation.theme.WhatsAppGreen
import com.whatsup.automation.presentation.theme.WhatsAppGreenLight

/**
 * نافذة الاشتراك والتحقق الزجاجية الداكنة الفاخرة (Cyber Jade Dark Glassmorphic Subscription Dialog).
 * تربط شاشة الدخول بنظام إدارة التراخيص المحلي والسحابي (LicenseManager & ActivationViewModel).
 */
@Composable
fun SubscriptionAuthDialog(
    viewModel: ActivationViewModel,
    onDismiss: () -> Unit,
    onActivationSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    telegramUrl: String = "https://t.me/SigmaAutoFlow"
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val licenseState by viewModel.licenseState.collectAsState()
    val inputKey by viewModel.inputKey.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    val scrollState = rememberScrollState()

    val isExpired = licenseState is LicenseState.Expired
    val isBlocked = licenseState is LicenseState.Blocked
    val isActive = licenseState is LicenseState.Active

    // عند نجاح التفعيل، نغلق النافذة ونوجه المستخدم
    LaunchedEffect(isActive) {
        if (isActive) {
            onActivationSuccess()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC040B08))
                .clickable { onDismiss() }
                .padding(horizontal = 20.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable(enabled = false) {} // منع إغلاق النافذة عند الضغط على الحاوية
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xF20F1A22))
                    .border(1.5.dp, Color(0x6600E676), RoundedCornerShape(28.dp))
                    .shadow(32.dp, RoundedCornerShape(28.dp), spotColor = Color(0x8800E676))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // شريط الإغلاق العلوي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x33FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // شارة الحالة النيونية
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isActive) Color(0x2600E676)
                                    else if (isExpired || isBlocked) Color(0x26EF4444)
                                    else Color(0x2606B6D4)
                                )
                                .border(
                                    1.dp,
                                    if (isActive) Color(0x6600E676)
                                    else if (isExpired || isBlocked) Color(0x66EF4444)
                                    else Color(0x6606B6D4),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = when {
                                    isActive -> "الاشتراك نشط 🛡️"
                                    isExpired -> "الاشتراك منتهٍ ⚠️"
                                    isBlocked -> "اشتراك محظور 🚫"
                                    else -> "تفعيل جديد 🔑"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    isActive -> Color(0xFF00E676)
                                    isExpired || isBlocked -> Color(0xFFEF4444)
                                    else -> Color(0xFF06B6D4)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // أيقونة الحالة الرئيسية المتوهجة
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        if (isActive) Color(0x4400E676) else Color(0x3300C853),
                                        Color.Transparent
                                    )
                                )
                            )
                            .border(1.5.dp, Color(0x6600E676), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Default.Verified else Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isActive) "اشتراكك مفعل بنجاح" else "تسجيل الدخول بكود الاشتراك",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = if (isActive)
                            "أنت متصل بالمنظومة السحابية ولديك وصول كامل لكافة خصائص الأتمتة الفورية"
                        else
                            "أدخل كود الاشتراك الخاص بك لربط هذا الجهاز وتفعيل مزايا Sigma AutoFlow",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                    )

                    if (isActive) {
                        val activeState = licenseState as? LicenseState.Active
                        val days = activeState?.remainingDays ?: 0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x3300E676))
                                .border(1.dp, Color(0x6600E676), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "الأيام المتبقية في الاشتراك",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$days يوماً",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onActivationSuccess,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(16.dp, RoundedCornerShape(25.dp), spotColor = Color(0x6600E676)),
                            shape = RoundedCornerShape(25.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                        ) {
                            Text(
                                text = "متابعة إلى لوحة التحكم 🚀",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        // بطاقة عرض بصمة ومعرّف الجهاز
                        val fingerprint = viewModel.deviceFingerprint
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x300F1A22))
                                .border(1.dp, Color(0x3300E676), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
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
                                        color = Color(0xFF94A3B8),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (fingerprint.length > 20) "${fingerprint.take(10)}...${fingerprint.takeLast(10)}" else fingerprint,
                                        fontSize = 12.sp,
                                        color = WhatsAppGreenLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Device ID", fingerprint)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "تم نسخ معرّف الجهاز بنجاح 📋", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x6600E676)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E676)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = WhatsAppGreenLight
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "نسخ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WhatsAppGreenLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // ترويسة حقل الهاش مع زر اللصق السريع
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "رمز الترخيص المشفر (الهاش):",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0)
                            )
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                    if (!text.isNullOrBlank()) {
                                        viewModel.onKeyChange(text)
                                        Toast.makeText(context, "تم لصق الهاش من الحافظة 📋", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = WhatsAppGreenLight,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "لصق من الحافظة",
                                    fontSize = 12.sp,
                                    color = WhatsAppGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // صندوق إدخال الهاش الكبير (Multi-line Glassmorphic Text Area)
                        OutlinedTextField(
                            value = inputKey,
                            onValueChange = { viewModel.onKeyChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            singleLine = false,
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            placeholder = {
                                Text(
                                    text = "الصق هنا كود الهاش المشفر الخاص بالترخيص (SIGMA-...)...",
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            trailingIcon = {
                                if (inputKey.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onKeyChange("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "مسح",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00E676),
                                unfocusedBorderColor = Color(0x4400E676),
                                focusedContainerColor = Color(0x40060D12),
                                unfocusedContainerColor = Color(0x40060D12)
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    viewModel.submitActivation()
                                }
                            )
                        )

                        // رسائل الخطأ والنجاح
                        AnimatedVisibility(visible = errorMessage != null) {
                            errorMessage?.let { error ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33EF4444))
                                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = error,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = successMessage != null) {
                            successMessage?.let { success ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x3300E676))
                                        .border(1.dp, Color(0x6600E676), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = success,
                                        color = Color(0xFF86EFAC),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // زر التحقق والتفعيل
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.submitActivation()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color(0x6600E676)),
                            enabled = !isSubmitting && inputKey.isNotBlank(),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WhatsAppGreen,
                                disabledContainerColor = Color(0x4400E676)
                            )
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جارٍ التحقق والربط بالسحابة...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تفعيل الاشتراك الآن ⚡",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // زر التواصل بتيليجرام للحصول على كود
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(context, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4D06B6D4)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF06B6D4))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "طلب كود تفعيل أو تجديد الاشتراك ✈️",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF06B6D4)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // تذييل مشفر عتادياً
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "نظام التراخيص محمي ومقيد ببصمة عتاد الجهاز",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}
