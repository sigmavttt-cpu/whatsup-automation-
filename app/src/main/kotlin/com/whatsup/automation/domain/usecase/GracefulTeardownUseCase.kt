package com.whatsup.automation.domain.usecase

import com.whatsup.automation.domain.repository.UpdateRepository
import javax.inject.Inject

/**
 * حالة استخدام بروتوكول التهدئة الآمنة (Graceful Teardown) للمحرك والخدمات والـ DB قبل التثبيت.
 */
class GracefulTeardownUseCase @Inject constructor(
    private val updateRepository: UpdateRepository
) {
    suspend fun execute(): Result<Unit> {
        return updateRepository.performGracefulTeardown()
    }

    suspend operator fun invoke(): Result<Unit> = execute()
}
