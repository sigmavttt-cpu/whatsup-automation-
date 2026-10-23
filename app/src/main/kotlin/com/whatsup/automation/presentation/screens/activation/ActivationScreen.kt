package com.whatsup.automation.presentation.screens.activation

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.whatsup.automation.presentation.screens.activation.components.DeviceFingerprintCard
import com.whatsup.automation.presentation.screens.activation.components.LicenseStatusBadge
import com.whatsup.automation.presentation.theme.*

/**
 * شاشة قفل وتفعيل الاشتراكات الفاخرة (Activation & Subscription Screen).
 * مصممة بنمط Cyber Jade Dark Glassmorphism مع دعم كامل لاتجاه RTL.
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
            .background(DarkBgPrimary)
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
            // أيقونة القفل والشعار
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        if (isExpired || isBlocked) RoseError.copy(alpha = 0.15f)
                        else WhatsAppGreen.copy(alpha = 0.15f)
                    )
                    .border(
                        2.dp,
                        if (isExpired || isBlocked) RoseError.copy(alpha = 0.5f)
                        else WhatsAppGreen.copy(alpha = 0.5f),
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
                    tint = if (isExpired || isBlocked) RoseError else WhatsAppGreen,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            LicenseStatusBadge(licenseState = licenseState)

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "WhatsUp Automation",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isExpired -> "لقد انتهت فترة اشتراكك الحالية. يسعدنا استمرارك معنا وتجديد اشتراكك عبر التواصل مع الإدارة."
                    isBlocked -> "تم تعليق هذا الترخيص من قبل الإدارة. يُرجى مراجعة الدعم الفني لحل الأمر."
                    else -> "مرحباً بك! يرجى إدخال كود التفعيل الذي استلمته من الإدارة للبدء باستخدام محرك الأتمتة."
                },
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // بطاقة الإدخال
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DeviceFingerprintCard(fingerprint = viewModel.deviceFingerprint)

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "رمز الترخيص المشفر (الهاش):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!text.isNullOrBlank()) {
                                    viewModel.onKeyChange(text)
                                    Toast.makeText(context, "تم لصق الهاش من الحافظة", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "لصق من الحافظة",
                                fontSize = 12.sp,
                                color = WhatsAppGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

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
                                color = TextMuted,
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
                                        tint = TextMuted
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = WhatsAppGreen,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedContainerColor = DarkBgSecondary,
                            unfocusedContainerColor = DarkBgSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
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

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RoseError.copy(alpha = 0.12f))
                                    .border(1.dp, RoseError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = RoseError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = RoseError,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = successMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        successMessage?.let { msg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WhatsAppGreen.copy(alpha = 0.12f))
                                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = WhatsAppGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg,
                                    fontSize = 12.sp,
                                    color = WhatsAppGreenLight,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            viewModel.submitActivation()
                        },
                        enabled = inputKey.isNotBlank() && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WhatsAppGreen,
                            disabledContainerColor = DarkBgSecondary
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = DarkBgPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "جارٍ التحقق...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBgPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = DarkBgPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفعيل الاشتراك الآن",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBgPrimary
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
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = CyberCyan
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تواصل معنا عبر تيليجرام للدعم أو التجديد",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "نظام التراخيص مشفر عتادياً ومحمي محلياً",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
