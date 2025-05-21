package com.example.myfitness.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myfitness.MainActivity
import com.example.myfitness.R

class NotificationHelper(private val context: Context) {
    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_ID = "workout_reminders"
        const val CHANNEL_NAME = "Workout Reminders"
        const val CHANNEL_DESCRIPTION = "Notifications for workout reminders and tips"
        private val NOTIFICATION_ICON = R.mipmap.ic_launcher
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
                // Check if channel already exists
                if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                    val importance = NotificationManager.IMPORTANCE_HIGH
                    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                        description = CHANNEL_DESCRIPTION
                        enableVibration(true)
                        enableLights(true)
                        setShowBadge(true)
                        vibrationPattern = longArrayOf(0, 500, 200, 500)
                    }
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Notification channel created successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel: ${e.message}")
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showWorkoutReminder(title: String, content: String) {
        if (!hasNotificationPermission()) {
            Log.e(TAG, "Notification permission not granted")
            return
        }

        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(NOTIFICATION_ICON)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)

            with(NotificationManagerCompat.from(context)) {
                val notificationId = System.currentTimeMillis().toInt()
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                ) {
                    val notificationId = System.currentTimeMillis().toInt()
                    with(NotificationManagerCompat.from(context)) {
                        notify(notificationId, builder.build())
                    }
                } else {
                    Log.w(TAG, "Notification permission not granted")
                }

                Log.d(TAG, "Notification sent successfully with ID: $notificationId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    fun showWorkoutTip(tip: String) {
        showWorkoutReminder("Workout Tip", tip)
    }

    fun showExerciseReminder(exerciseName: String) {
        showWorkoutReminder(
            "Time to Exercise!",
            "Don't forget to complete your $exerciseName workout today!"
        )
    }
} 