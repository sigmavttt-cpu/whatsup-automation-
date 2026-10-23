package com.whatsup.automation.presentation.screens.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.data.security.DeviceSecurityManager
import com.whatsup.automation.data.security.LicenseManager
import com.whatsup.automation.data.security.LicenseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * مدير حالة واجهة التفعيل والاشتراكات.
 */
@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val licenseManager: LicenseManager,
    private val deviceSecurityManager: DeviceSecurityManager
) : ViewModel() {

    val licenseState: StateFlow<LicenseState> = licenseManager.licenseState

    val deviceFingerprint: String = deviceSecurityManager.getOrCreateDeviceFingerprint()

    private val _inputKey = MutableStateFlow("")
    val inputKey: StateFlow<String> = _inputKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun onKeyChange(key: String) {
        _inputKey.value = key.trim()
        _errorMessage.value = null
    }

    fun submitActivation() {
        val key = _inputKey.value.trim()
        if (key.length < 8) {
            _errorMessage.value = "يرجى لصق رمز الهاش المشفر الخاص بالترخيص"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            _errorMessage.value = null
            _successMessage.value = null

            val result = licenseManager.activateLicense(key)
            result.fold(
                onSuccess = { msg ->
                    _successMessage.value = msg
                    _isSubmitting.value = false
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "فشل التفعيل، تحقق من صحة الهاش أو الاتصال بالإنترنت"
                    _isSubmitting.value = false
                }
            )
        }
    }

    fun refreshStatus() {
        licenseManager.checkLocalStateAndRefresh()
    }
}
