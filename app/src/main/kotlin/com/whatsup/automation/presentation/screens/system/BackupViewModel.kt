package com.whatsup.automation.presentation.screens.system

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsup.automation.data.security.BackupManager
import com.whatsup.automation.data.security.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastResult: BackupResult? = null
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(350)
            _isRefreshing.value = false
        }
    }

    fun exportBackup(password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, lastResult = null) }
            val res = backupManager.exportEncryptedBackup(password)
            _uiState.update { it.copy(isExporting = false, lastResult = res) }
        }
    }

    fun importBackup(stream: InputStream, password: String, merge: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, lastResult = null) }
            val res = backupManager.importEncryptedBackup(stream, password, merge)
            _uiState.update { it.copy(isImporting = false, lastResult = res) }
        }
    }

    fun dismissResult() {
        _uiState.update { it.copy(lastResult = null) }
    }
}
