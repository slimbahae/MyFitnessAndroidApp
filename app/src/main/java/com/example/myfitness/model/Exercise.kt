package com.example.myfitness.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val gifUrl: String,
    val target: String,
    val secondaryMuscles: List<String>,
    val instructions: List<String>
)

@Serializable
data class ExerciseApiResponse(
    val id: String,
    val gifUrl: String
    // Add other fields if needed
) 