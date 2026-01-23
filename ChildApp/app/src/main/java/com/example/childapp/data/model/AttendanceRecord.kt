package com.example.childapp.data.model

import com.google.firebase.Timestamp

data class AttendanceRecord(
    val type: String,      // "entry" | "exit"
    val time: Timestamp
)
