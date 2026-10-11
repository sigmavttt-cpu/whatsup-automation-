package com.whatsup.automation.presentation.screens.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.MainActivity
import com.whatsup.automation.presentation.theme.DarkBgPrimary
import com.whatsup.automation.presentation.theme.RoseError
import com.whatsup.automation.presentation.theme.TextPrimary
import com.whatsup.automation.presentation.theme.TextSecondary
import com.whatsup.automation.presentation.theme.WhatsAppGreen

class CrashReportActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CRASH_REPORT = "extra_crash_report"
        const val EXTRA_EXCEPTION_CLASS = "extra_exception_class"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val report = intent.getStringExtra(EXTRA_CRASH_REPORT) ?: "لا يتوفر تقرير تشخيصي"
        val exceptionClass = intent.getStringExtra(EXTRA_EXCEPTION_CLASS) ?: "UnknownException"
        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "حدث خطأ غير متوقع"

        setContent {
            CrashReportScreen(
                exceptionClass = exceptionClass,
                errorMessage = errorMessage,
                fullReport = report,
                onRestart = {
                    val restartIntent = Intent(this, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    startActivity(restartIntent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun CrashReportScreen(
    exceptionClass: String,
    errorMessage: String,
    fullReport: String,
    onRestart: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ترويسة شاشة الطوارئ
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RoseError.copy(alpha = 0.2f))
                        .border(1.5.dp, RoseError, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = RoseError,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column {
                    Text(
                        text = "شاشة تشخيص الطوارئ للمطور ⚠️",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "تم التقاط الخطأ محلياً ومنع إغلاق النظام الفجائي",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.5.sp
                    )
                }
            }

            // بطاقة نوع الخطأ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "نوع الاستثناء: $exceptionClass",
                        color = RoseError,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 12.5.sp
                    )
                }
            }

            // نافذة الـ StackTrace البرمجية
            Text(
                text = "تقرير التتبع التفصيلي (StackTrace):",
                color = Color(0xFFE2E8F0),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 400.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF020617))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
            ) {
                Text(
                    text = fullReport,
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }

            // أزرار الإجراءات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Crash Report", fullReport)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "تم نسخ تقرير الخطأ للحافظة 📋", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("نسخ التقرير", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Text("إعادة تشغيل التطبيق", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
