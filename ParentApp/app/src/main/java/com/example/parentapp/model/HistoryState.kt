package com.example.parentapp.model

data class HistoryState(
    val items: List<AttendanceRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null
)
