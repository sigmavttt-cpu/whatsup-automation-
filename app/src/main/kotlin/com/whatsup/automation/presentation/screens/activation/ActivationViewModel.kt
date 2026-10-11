package com.whatsup.automation.presentation.screens.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val licenseManager: LicenseManager
) : ViewModel() {

    val licenseState: StateFlow<LicenseState> = licenseManager.licenseState

    private val _inputKey = MutableStateFlow("")
    val inputKey: StateFlow<String> = _inputKey.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun onKeyChange(key: String) {
        _inputKey.value = key.trim().uppercase()
        _errorMessage.value = null
    }

    fun submitActivation() {
        val key = _inputKey.value.trim().uppercase()
        if (key.length < 5) {
            _errorMessage.value = "يرجى كتابة كود تفعيل صحيح"
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
                    _errorMessage.value = error.message ?: "فشل التفعيل، تحقق من صحة الكود واتصال الإنترنت"
                    _isSubmitting.value = false
                }
            )
        }
    }

    fun refreshStatus() {
        licenseManager.checkLocalStateAndRefresh()
    }
}
