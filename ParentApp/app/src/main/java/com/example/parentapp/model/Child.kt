package com.example.parentapp.model

data class Child(
    val id: String,
    val name: String,
    val surname: String
) {
    val fullName: String get() = "$name $surname"
}
