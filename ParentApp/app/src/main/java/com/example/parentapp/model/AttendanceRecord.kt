package com.example.parentapp.model

data class AttendanceRecord(
    val id: String,
    val type: String, // "entry" | "exit"
    val time: Long    // epoch millis
) {
    val isEntry: Boolean get() = type.equals("entry", ignoreCase = true)
    val isExit: Boolean get() = type.equals("exit", ignoreCase = true)
}
