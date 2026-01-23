package com.example.childapp

data class UiState(
    val permissionsGranted: Boolean = false,
    val serviceRunning: Boolean = false,
    val message: String = "",
    val wifiEnabled: Boolean = true
)
