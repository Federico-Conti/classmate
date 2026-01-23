package com.example.childapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onPermissionsResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            permissionsGranted = granted,
            message = if (granted) "Permits ON" else "Permits OFF"
        )
    }

    fun setServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(serviceRunning = running)
    }

    fun setWifiEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(wifiEnabled = enabled)
    }

}
