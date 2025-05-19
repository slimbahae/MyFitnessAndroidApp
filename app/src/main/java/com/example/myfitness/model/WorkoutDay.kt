package com.example.myfitness.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutDay(
    val day: String,
    val type: String,
    val muscleGroup: String,
    val exercises: List<Exercise>,
    val duration: Int,
    val calories: Int,
    val notes: String
)

@Serializable
data class Exercise(
    val name: String,
    val sets: Int,
    val reps: String,
    val instructions: String
) 