package com.example.myfitness.ui.model

data class UserProfile(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val age: Int = 0,
    val gender: String = "",
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val goal: String = ""
)
