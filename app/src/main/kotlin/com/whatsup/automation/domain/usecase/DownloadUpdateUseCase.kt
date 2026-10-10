package com.whatsup.automation.domain.usecase

import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.model.UpdateDownloadProgress
import com.whatsup.automation.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * حالة استخدام تنزيل حزمة التحديث عبر المحرك القطاعي القابل للاستئناف.
 */
class DownloadUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    fun execute(
        updateInfo: AppUpdateInfo,
        useMirror: Boolean = false
    ): Flow<UpdateDownloadProgress> {
        return updateRepository.downloadUpdate(updateInfo, useMirror)
    }

    operator fun invoke(
        updateInfo: AppUpdateInfo,
        useMirror: Boolean = false
    ): Flow<UpdateDownloadProgress> = execute(updateInfo, useMirror)
}
