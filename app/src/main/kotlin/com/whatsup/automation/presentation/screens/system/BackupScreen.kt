package com.whatsup.automation.presentation.screens.system

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.whatsup.automation.data.security.BackupResult
import com.whatsup.automation.presentation.components.BentoCard
import com.whatsup.automation.presentation.components.LiquidGradientButton
import com.whatsup.automation.presentation.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }

    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }

    // منتقي الملفات للاستيراد
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImportUri = uri
            showImportPasswordDialog = true
        }
    }

    // نافذة إدخال كلمة سر التصدير
    if (showExportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("تعيين كلمة مرور التشفير", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "سيتم تشفير النسخة الاحتياطية محلياً بتشفير عسكري AES-256-GCM. احفظ كلمة المرور لاستعادة بياناتك لاحقاً.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("كلمة المرور (4 خانات فأكثر)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPassword.length >= 4) {
                            showExportPasswordDialog = false
                            viewModel.exportBackup(exportPassword)
                            exportPassword = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    enabled = exportPassword.length >= 4
                ) {
                    Text("بدء التصدير المشفر", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExportPasswordDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkBgSecondary
        )
    }

    // نافذة إدخال كلمة سر الاستيراد
    if (showImportPasswordDialog && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = { showImportPasswordDialog = false },
            title = { Text("فك تشفير النسخة الاحتياطية", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "أدخل كلمة المرور التي تم استخدامها أثناء تصدير هذا الملف لاستعادة القواعد بنجاح.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("كلمة المرور") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri
                        if (uri != null && importPassword.isNotEmpty()) {
                            try {
                                context.contentResolver.openInputStream(uri)?.let { stream ->
                                    viewModel.importBackup(stream, importPassword, merge = false)
                                }
                            } catch (_: Exception) {}
                            showImportPasswordDialog = false
                            importPassword = ""
                            selectedImportUri = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    enabled = importPassword.isNotEmpty()
                ) {
                    Text("فك التشفير والاستعادة", color = Color.Black)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportPasswordDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            },
            containerColor = DarkBgSecondary
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.refresh()
        },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
        // بطاقة التعريف والدرع
        item {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "النسخ الاحتياطي المشفر محلياً",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "تصدير واستعادة قواعدك بأمان تام بدون أي سحابة",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // نتيجة العملية إن وجدت
        item {
            uiState.lastResult?.let { res ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (res) {
                                is BackupResult.ExportSuccess, is BackupResult.ImportSuccess -> WhatsAppGreen.copy(alpha = 0.15f)
                                is BackupResult.Error -> RoseError.copy(alpha = 0.15f)
                            }
                        )
                        .border(
                            1.dp,
                            when (res) {
                                is BackupResult.ExportSuccess, is BackupResult.ImportSuccess -> WhatsAppGreen.copy(alpha = 0.4f)
                                is BackupResult.Error -> RoseError.copy(alpha = 0.4f)
                            },
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (res) {
                                    is BackupResult.ExportSuccess, is BackupResult.ImportSuccess -> Icons.Default.CheckCircle
                                    is BackupResult.Error -> Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = when (res) {
                                    is BackupResult.ExportSuccess, is BackupResult.ImportSuccess -> WhatsAppGreen
                                    is BackupResult.Error -> RoseError
                                }
                            )
                            Text(
                                text = when (res) {
                                    is BackupResult.ExportSuccess -> "تم تصدير ${res.rulesCount} قاعدة بنجاح!"
                                    is BackupResult.ImportSuccess -> "تم استعادة ${res.restoredRulesCount} قاعدة بنجاح!"
                                    is BackupResult.Error -> "فشل في العملية"
                                },
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        when (res) {
                            is BackupResult.ExportSuccess -> {
                                Text(
                                    text = "تم حفظ الملف المشفر في:\n${res.filePath}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = {
                                        try {
                                            val file = File(res.filePath)
                                            val fileUri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/octet-stream"
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة النسخة الاحتياطية المشفرة"))
                                        } catch (_: Exception) {}
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("مشاركة ملف النسخة الاحتياطية", fontSize = 12.sp)
                                }
                            }
                            is BackupResult.Error -> {
                                Text(
                                    text = res.message,
                                    fontSize = 12.sp,
                                    color = RoseError
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // أزرار العمليات
        item {
            BentoCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "العمليات السريعة",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    LiquidGradientButton(
                        text = if (uiState.isExporting) "جاري تشفير وتصدير القواعد..." else "📤 تصدير نسخة احتياطية مشفرة",
                        onClick = { showExportPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isExporting && !uiState.isImporting
                    )

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f)),
                        enabled = !uiState.isExporting && !uiState.isImporting
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isImporting) "جاري فك التشفير والاستيراد..." else "📥 استعادة نسخة احتياطية مشفرة",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
    }
}
