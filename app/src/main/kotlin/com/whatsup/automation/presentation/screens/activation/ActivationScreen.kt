package com.whatsup.automation.presentation.screens.activation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.whatsup.automation.data.security.LicenseState
import com.whatsup.automation.presentation.theme.*

/**
 * شاشة قفل وتفعيل الاشتراكات الفاخرة (Activation & Subscription Screen).
 * مصممة بنمط White Emerald Luxury الملكي مع دعم كامل لاتجاه RTL.
 */
@Composable
fun ActivationScreen(
    viewModel: ActivationViewModel,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEDF4EE),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // بطاقة الشعار والرمز التفاعلي
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isExpired || isBlocked) {
                                listOf(Color(0xFFFFEBEE), Color(0xFFFFCDD2))
                            } else {
                                listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                            }
                        )
                    )
                    .border(
                        2.dp,
                        if (isExpired || isBlocked) Color(0xFFEF5350) else WhatsAppGreen.copy(alpha = 0.5f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isExpired -> Icons.Default.HourglassDisabled
                        isBlocked -> Icons.Default.Block
                        else -> Icons.Default.Key
                    },
                    contentDescription = null,
                    tint = if (isExpired || isBlocked) Color(0xFFD32F2F) else WhatsAppGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // شارة الحالة (Status Badge)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = when {
                    isExpired -> Color(0xFFFFEBEE)
                    isBlocked -> Color(0xFFFFEBEE)
                    else -> Color(0xFFE8F5E9)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isExpired || isBlocked) Color(0xFFFFCDD2) else Color(0xFFA5D6A7)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpired || isBlocked) Icons.Default.Warning else Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = if (isExpired || isBlocked) Color(0xFFC62828) else WhatsAppGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            isExpired -> "انتهت فترة الاشتراك"
                            isBlocked -> "تم إيقاف الاشتراك"
                            else -> "تفعيل الاشتراك مطلوب"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpired || isBlocked) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // العنوان الرئيسي
            Text(
                text = "WhatsUp Automation",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // الوصف التوضيحي
            Text(
                text = when {
                    isExpired -> "لقد انتهت فترة اشتراكك الحالية. يسعدنا استمرارك معنا وتجديد اشتراكك عبر التواصل مع الإدارة."
                    isBlocked -> "تم تعليق هذا الترخيص من قبل الإدارة. يُرجى مراجعة الدعم الفني لحل الأمر."
                    else -> "مرحباً بك! يرجى إدخال كود التفعيل الذي استلمته من الإدارة للبدء باستخدام محرك الأتمتة والردود التلقائية."
                },
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // بطاقة الإدخال الفاخرة (Bento Card)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "كود التفعيل (License Key)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { viewModel.onKeyChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "WUP-XXXX-XXXX-XXXX",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = WhatsAppGreen
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
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                viewModel.submitActivation()
                            }
                        )
                    )

                    // عرض رسالة الخطأ إن وجدت
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFEBEE))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 12.sp,
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // عرض رسالة النجاح إن وجدت
                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        successMessage?.let { success ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = success,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // زر تفعيل الاشتراك
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitActivation()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isSubmitting && inputKey.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جارٍ التحقق والربط بالسحابة...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفعيل الاشتراك الآن 🚀",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // زر التواصل المباشر عبر تيليجرام
            OutlinedButton(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0288D1)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF0288D1)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF0288D1)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تواصل معنا عبر تيليجرام للدفع أو التجديد ✈️",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0288D1)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // شارة الأمان في الأسفل
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نظام التراخيص مشفر عتادياً ومربوط بالسحابة بأمان",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
