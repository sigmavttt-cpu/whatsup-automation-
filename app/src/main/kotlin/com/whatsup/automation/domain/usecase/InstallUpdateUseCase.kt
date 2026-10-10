package com.whatsup.automation.domain.usecase

import com.whatsup.automation.domain.repository.UpdateRepository
import java.io.File
import javax.inject.Inject

/**
 * حالة استخدام التحقق من الإذن وإطلاق تثبيت حزمة APK.
 */
class InstallUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    fun canInstallPackages(): Boolean = updateRepository.canInstallPackages()

    fun launchInstallPermissionSettings() = updateRepository.launchInstallPermissionSettings()

    suspend fun execute(apkFile: File): Result<Unit> {
        return updateRepository.launchApkInstallation(apkFile)
    }

    suspend operator fun invoke(apkFile: File): Result<Unit> = execute(apkFile)
}
