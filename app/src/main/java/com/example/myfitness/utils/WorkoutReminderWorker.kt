package com.example.myfitness.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WorkoutReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.showWorkoutReminder(
            "Workout Reminder",
            "Time for your daily workout! Stay consistent to achieve your fitness goals."
        )
        return Result.success()
    }

    companion object {
        fun scheduleWorkoutReminder(context: Context, hourOfDay: Int, minute: Int) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hourOfDay)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

            val workRequest = androidx.work.PeriodicWorkRequestBuilder<WorkoutReminderWorker>(
                java.time.Duration.ofDays(1)
            )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

            androidx.work.WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "workout_reminder",
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    workRequest
                )
        }
    }
} 