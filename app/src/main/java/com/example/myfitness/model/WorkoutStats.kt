package com.example.myfitness.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutStats(
    val completedExercises: Map<String, Int> = emptyMap(),
    val totalCaloriesBurned: Int = 0,
    val daysCompleted: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) 