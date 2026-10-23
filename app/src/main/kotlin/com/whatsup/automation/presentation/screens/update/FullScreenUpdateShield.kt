package com.whatsup.automation.presentation.screens.update

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.whatsup.automation.domain.model.UpdateUiState
import com.whatsup.automation.presentation.screens.update.components.*

/**
 * درع التحديث الإجباري والاختياري الشامل (FullScreenUpdateShield).
 * يوفر حماية أمنية وتجربة مستخدم فاخرة بنظام Dark Glassmorphism مع عرض سرعة التحميل وسجل التغييرات.
 */
@Composable
fun FullScreenUpdateShield(
    viewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is UpdateUiState.Idle -> { /* لا شيء */ }
        is UpdateUiState.Checking -> {
            // فحص هادئ بالخلفية
        }
        is UpdateUiState.UpToDate -> {
            LaunchedEffect(Unit) {
                viewModel.dismissOptionalUpdate()
            }
        }
        is UpdateUiState.UpdateAvailable -> {
            if (state.isMandatory) {
                MandatoryUpdateShieldContent(
                    updateInfo = state.updateInfo,
                    currentVersionName = state.currentVersionName,
                    onStartDownload = { viewModel.startDownload(useMirror = false) }
                )
            } else {
                OptionalUpdateDialog(
                    updateInfo = state.updateInfo,
                    currentVersionName = state.currentVersionName,
                    onStartDownload = { viewModel.startDownload(useMirror = false) },
                    onDismiss = { viewModel.dismissOptionalUpdate() }
                )
            }
        }
        is UpdateUiState.Downloading -> {
            DownloadProgressContent(
                state = state,
                onUseMirror = { viewModel.startDownload(useMirror = true) }
            )
        }
        is UpdateUiState.VerifyingChecksum -> {
            VerifyingChecksumContent(state = state)
        }
        is UpdateUiState.ReadyToInstall -> {
            ReadyToInstallContent(
                state = state,
                canInstall = viewModel.canInstallPackages(),
                onRequestPermission = { viewModel.openInstallPermissionSettings() },
                onInstall = { viewModel.proceedWithTeardownAndInstall(state.apkFile) }
            )
        }
        is UpdateUiState.PreparingTeardown -> {
            TeardownProgressContent(state = state)
        }
        is UpdateUiState.Installing -> {
            InstallingContent(state = state)
        }
        is UpdateUiState.Failed -> {
            UpdateErrorDialog(
                state = state,
                onRetry = { viewModel.startDownload(useMirror = false) },
                onUseMirror = { viewModel.startDownload(useMirror = true) },
                onDismiss = { viewModel.dismissOptionalUpdate() }
            )
        }
    }
}
