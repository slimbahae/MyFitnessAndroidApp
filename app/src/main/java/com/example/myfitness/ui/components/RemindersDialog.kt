package com.example.myfitness.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myfitness.ui.theme.Purple
import com.example.myfitness.ui.theme.DarkGray
import com.example.myfitness.ui.theme.White
import com.example.myfitness.ui.theme.LightGray
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.compose.ui.platform.LocalContext
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersDialog(
    onDismiss: () -> Unit,
    onSaveReminders: (List<LocalTime>) -> Unit,
    initialReminders: List<LocalTime> = emptyList()
) {
    var reminders by remember { mutableStateOf(initialReminders) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    @RequiresApi(Build.VERSION_CODES.O)
    fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                val newTime = LocalTime.of(selectedHour, selectedMinute)
                reminders = reminders + newTime
            },
            hour,
            minute,
            true
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    "Workout Reminders",
                    style = MaterialTheme.typography.titleLarge,
                    color = White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Add new reminder button
                Button(
                    onClick = { showTimePickerDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Reminder",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Reminder")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // List of reminders
                reminders.forEach { time ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = "Time",
                                tint = Purple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                time.format(DateTimeFormatter.ofPattern("hh:mm a")),
                                color = White
                            )
                        }
                        IconButton(
                            onClick = { reminders = reminders - time }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Purple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = { onSaveReminders(reminders) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Save Reminders")
                }
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkGray)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Time",
                    style = MaterialTheme.typography.titleLarge,
                    color = White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Time picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Hours
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hours", color = LightGray)
                        NumberPicker(
                            value = selectedHour,
                            onValueChange = { selectedHour = it },
                            range = 0..23
                        )
                    }
                    
                    Text(":", color = White, style = MaterialTheme.typography.headlineMedium)
                    
                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minutes", color = LightGray)
                        NumberPicker(
                            value = selectedMinute,
                            onValueChange = { selectedMinute = it },
                            range = 0..59
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm button
                Button(
                    onClick = { onTimeSelected(selectedHour, selectedMinute) },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange
) {
    Column {
        Button(
            onClick = { 
                if (value < range.last) onValueChange(value + 1)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("▲")
        }
        
        Text(
            text = value.toString().padStart(2, '0'),
            color = White,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Button(
            onClick = { 
                if (value > range.first) onValueChange(value - 1)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Purple)
        ) {
            Text("▼")
        }
    }
} 