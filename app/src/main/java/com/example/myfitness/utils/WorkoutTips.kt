package com.example.myfitness.utils

object WorkoutTips {
    private val generalTips = listOf(
        "Stay hydrated during your workout!",
        "Remember to warm up before exercising.",
        "Focus on proper form over quantity.",
        "Take rest days to allow your body to recover.",
        "Consistency is key to achieving your fitness goals.",
        "Don't forget to stretch after your workout.",
        "Track your progress to stay motivated.",
        "Listen to your body and don't push too hard.",
        "Mix up your routine to prevent plateaus.",
        "Get enough sleep for better recovery."
    )

    private val motivationTips = listOf(
        "You're stronger than you think!",
        "Every workout brings you closer to your goal.",
        "Small progress is still progress.",
        "Your future self will thank you.",
        "You've got this! 💪",
        "Make today count!",
        "Stay focused on your goals.",
        "You're building a better version of yourself.",
        "Consistency beats intensity.",
        "Keep pushing forward!"
    )

    fun getRandomTip(): String {
        return (generalTips + motivationTips).random()
    }

    fun getRandomGeneralTip(): String {
        return generalTips.random()
    }

    fun getRandomMotivationTip(): String {
        return motivationTips.random()
    }
} 