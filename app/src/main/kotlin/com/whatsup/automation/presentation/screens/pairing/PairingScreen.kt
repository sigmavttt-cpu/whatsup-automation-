package com.whatsup.automation.presentation.screens.pairing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsup.automation.presentation.components.AppScreenHeader
import com.whatsup.automation.presentation.components.ConnectionStatusBadge
import com.whatsup.automation.presentation.components.SegmentedPillSelector
import com.whatsup.automation.presentation.screens.pairing.components.PairingCodeCard
import com.whatsup.automation.presentation.screens.pairing.components.PairingQrCard
import com.whatsup.automation.presentation.screens.pairing.components.PairingSuccessCard
import com.whatsup.automation.presentation.screens.pairing.components.ServiceStatusCard
import com.whatsup.automation.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    viewModel: PairingViewModel,
    showHeader: Boolean = true,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshConnection(context) },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // شريط العنوان الموحد
            if (showHeader) {
                AppScreenHeader(
                    title = if (uiState.isPairedSuccess) "إدارة الجلسة والحساب" else "ربط الجلسة والحساب",
                    subtitle = if (uiState.isPairedSuccess) "الجلسة نشطة ومحرك الأتمتة يعمل" else "اختر طريقة الربط المناسبة لجهازك",
                    onOpenDrawer = onOpenDrawer,
                    actions = {
                        ConnectionStatusBadge(connectionState = uiState.connectionState)
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.isPairedSuccess) "إدارة الجلسة والحساب:" else "طريقة ربط الجهاز:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    ConnectionStatusBadge(connectionState = uiState.connectionState)
                }
            }

            if (uiState.isPairedSuccess) {
                // حالة الاتصال الفعال: لوحة إدارة الجلسة والخدمات الحيوية
                PairingSuccessCard(
                    phoneNumber = uiState.phoneNumberInput.ifBlank { null },
                    onLogout = { viewModel.logout() }
                )

                Text(
                    text = "صحة المحرك والخدمات الحيوية:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                ServiceStatusCard(
                    title = "خدمة قراءة إشعارات واتساب (أساسي)",
                    description = "تمكن التطبيق من التقاط الرسائل الواردة والرد الفوري عليها عبر RemoteInput",
                    isActive = uiState.isNotificationListenerActive,
                    icon = Icons.Filled.NotificationsActive,
                    activeColor = WhatsAppGreen,
                    onActionClick = { viewModel.openNotificationSettings(context) },
                    actionButtonText = if (uiState.isNotificationListenerActive) "الخدمة مفعلة" else "تفعيل الآن"
                )

                ServiceStatusCard(
                    title = "حصانة الخلفية 24/7 (توفير البطارية)",
                    description = "استثناء التطبيق من وضع Doze لضمان استمرار عمل الأتمتة دون إيقاف المعالج",
                    isActive = uiState.isBatteryOptimizationIgnored,
                    icon = Icons.Filled.BatteryChargingFull,
                    activeColor = CyberCyan,
                    onActionClick = { viewModel.openBatterySettings(context) },
                    actionButtonText = if (uiState.isBatteryOptimizationIgnored) "الحصانة نشطة" else "منح الحصانة"
                )

                ServiceStatusCard(
                    title = "أذونات دفتر جهات الاتصال",
                    description = "تتيح حفظ الأسماء المستخرجة تلقائياً في دفتر الهاتف",
                    isActive = uiState.hasContactsPermission,
                    icon = Icons.Filled.Contacts,
                    activeColor = ElectricBlue,
                    onActionClick = { viewModel.openAppSettings(context) },
                    actionButtonText = if (uiState.hasContactsPermission) "الأذونات ممنوحة" else "فتح الأذونات"
                )

                Button(
                    onClick = { viewModel.checkAllPermissions(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHover),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص وتحديث حالة الخدمات", fontSize = 13.sp)
                }
            } else {
                // محدد طرق الربط الكبسولي
                SegmentedPillSelector(
                    options = listOf(
                        PairingMethod.PAIRING_CODE to "كود الرقم",
                        PairingMethod.QR_CODE to "رمز QR",
                        PairingMethod.DIRECT_SYSTEM to "مساعد النظام"
                    ),
                    selectedOption = uiState.selectedMethod,
                    onOptionSelected = { viewModel.selectMethod(it) },
                    activeColor = WhatsAppGreen
                )

                // تنبيه الخطأ عند التواجد في تبويبات أخرى غير كود الرقم
                val errorMsg = uiState.errorMessage
                if (errorMsg != null && uiState.selectedMethod != PairingMethod.PAIRING_CODE) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = RoseError.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = RoseError,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = errorMsg,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissError() },
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
                }

                // محتوى التبويب المختار
                when (uiState.selectedMethod) {
                    PairingMethod.PAIRING_CODE -> {
                        PairingCodeCard(
                            uiState = uiState,
                            onCountrySelected = { viewModel.onCountrySelected(it) },
                            onNationalNumberChanged = { viewModel.onNationalPhoneNumberChange(it) },
                            onRequestPairingCode = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.requestPairingCode()
                            },
                            onOpenWhatsApp = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.openWhatsApp(context)
                            },
                            onCopyCode = { code ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Pairing Code", code.replace("-", "").trim())
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الكود: $code", Toast.LENGTH_SHORT).show()
                            },
                            onConfirmSuccess = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.confirmSimulatedSuccess()
                            },
                            onDismissError = { viewModel.dismissError() },
                            onRetry = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.retryPairing()
                            },
                            onCleanStaleData = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.cleanStaleData()
                                Toast.makeText(context, "تم تنظيف كافة البيانات العالقة والمقابس القديمة بنجاح 🧹", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    PairingMethod.QR_CODE -> {
                        PairingQrCard(
                            qrCodePayload = uiState.qrCodePayload,
                            onRefreshQr = { viewModel.refreshQr() },
                            onShareQr = { viewModel.shareQrCode(context) },
                            onConfirmSuccess = { viewModel.confirmSimulatedSuccess() }
                        )
                    }

                    PairingMethod.DIRECT_SYSTEM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ServiceStatusCard(
                                title = "خدمة قراءة إشعارات واتساب (أساسي)",
                                description = "تمكن التطبيق من التقاط الرسائل الواردة والرد الفوري عليها عبر RemoteInput",
                                isActive = uiState.isNotificationListenerActive,
                                icon = Icons.Filled.NotificationsActive,
                                activeColor = WhatsAppGreen,
                                onActionClick = { viewModel.openNotificationSettings(context) },
                                actionButtonText = if (uiState.isNotificationListenerActive) "الخدمة مفعلة" else "تفعيل الآن"
                            )

                            ServiceStatusCard(
                                title = "حصانة الخلفية 24/7 (توفير البطارية)",
                                description = "استثناء التطبيق من وضع Doze لضمان استمرار عمل الأتمتة دون إيقاف المعالج",
                                isActive = uiState.isBatteryOptimizationIgnored,
                                icon = Icons.Filled.BatteryChargingFull,
                                activeColor = CyberCyan,
                                onActionClick = { viewModel.openBatterySettings(context) },
                                actionButtonText = if (uiState.isBatteryOptimizationIgnored) "الحصانة نشطة" else "منح الحصانة"
                            )

                            ServiceStatusCard(
                                title = "أذونات دفتر جهات الاتصال",
                                description = "تتيح حفظ الأسماء المستخرجة تلقائياً في دفتر الهاتف",
                                isActive = uiState.hasContactsPermission,
                                icon = Icons.Filled.Contacts,
                                activeColor = ElectricBlue,
                                onActionClick = { viewModel.openAppSettings(context) },
                                actionButtonText = if (uiState.hasContactsPermission) "الأذونات ممنوحة" else "فتح الأذونات"
                            )

                            Button(
                                onClick = { viewModel.checkAllPermissions(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceHover),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص وتحديث حالة الخدمات", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
