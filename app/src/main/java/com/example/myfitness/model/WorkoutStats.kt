package com.example.myfitness.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutStats(
    // completedExercises: Map<exerciseName, setsCompleted>
    val completedExercises: Map<String, Int> = emptyMap(),
    val totalCaloriesBurned: Int = 0,
    val daysCompleted: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) 