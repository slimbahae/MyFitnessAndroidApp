package com.example.myfitness.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myfitness.utils.WorkoutReminderWorker
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout Reminder Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set your daily workout reminder time",
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            selectedHour = hour
                            selectedMinute = minute
                            WorkoutReminderWorker.scheduleWorkoutReminder(context, hour, minute)
                        },
                        selectedHour,
                        selectedMinute,
                        true
                    ).show()
                }
            ) {
                Text("Set Reminder Time")
            }

            Text(
                text = "Current reminder time: ${String.format("%02d:%02d", selectedHour, selectedMinute)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 