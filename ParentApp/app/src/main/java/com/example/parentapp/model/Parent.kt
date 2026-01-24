package com.example.parentapp.model

data class Parent(
    val id: String,
    val name: String,
    val surname: String,
    val childrens: List<String>
)
