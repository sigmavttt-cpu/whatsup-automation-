package com.whatsup.automation.domain.usecase

import com.whatsup.automation.domain.model.AppUpdateInfo
import com.whatsup.automation.domain.repository.UpdateRepository
import javax.inject.Inject

/**
 * حالة استخدام فحص التحديثات المتاحة من Supabase.
 */
class CheckForUpdateUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    suspend fun execute(
        channel: String = "stable",
        currentVersionCode: Int,
        currentAbi: String = "arm64-v8a"
    ): Result<AppUpdateInfo?> {
        return updateRepository.checkForUpdate(channel, currentVersionCode, currentAbi)
    }

    operator fun invoke(
        channel: String = "stable",
        currentVersionCode: Int,
        currentAbi: String = "arm64-v8a"
    ) = kotlinx.coroutines.flow.flow {
        emit(execute(channel, currentVersionCode, currentAbi))
    }
}
