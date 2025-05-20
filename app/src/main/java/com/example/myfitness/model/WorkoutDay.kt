package com.example.myfitness.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDay(
    val day: String,
    val type: String,
    val muscleGroup: String,
    val exerciseIds: List<String>,
    val duration: Int,
    val calories: Int,
    val notes: String
) 