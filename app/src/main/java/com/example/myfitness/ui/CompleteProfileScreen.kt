package com.example.myfitness.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myfitness.ai.GeminiWorkoutGenerator
import com.example.myfitness.ui.model.UserProfile
import com.example.myfitness.ui.theme.Purple
import com.example.myfitness.ui.theme.DarkGray
import com.example.myfitness.ui.theme.White
import com.example.myfitness.ui.theme.LightGray
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Flag
import com.example.myfitness.model.WorkoutStats
import kotlinx.coroutines.tasks.await

@Composable
fun CompleteProfileScreen(navController: NavController, auth: FirebaseAuth) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val uid = auth.currentUser?.uid ?: return
    val email = auth.currentUser?.email ?: ""
    val db = FirebaseFirestore.getInstance()

    var step by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var debugInfo by remember { mutableStateOf("") }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Profile Setup (${step + 1}/6)", style = MaterialTheme.typography.headlineSmall.copy(color = White))
            Spacer(modifier = Modifier.height(24.dp))

            // Debug info display
            if (debugInfo.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkGray)
                ) {
                    Text(
                        text = debugInfo,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall.copy(color = Purple)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    when (step) {
                        0 -> StepInput("First Name", firstName, { firstName = it }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        1 -> StepInput("Last Name", lastName, { lastName = it }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        2 -> StepInput("Age", age, { age = it }, keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Outlined.Cake, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        3 -> StepInput("Height (cm)", height, { height = it }, keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Outlined.Height, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        4 -> StepInput("Weight (kg)", weight, { weight = it }, keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        5 -> GoalPicker(goal, onSelect = { goal = it }, leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 0) {
                    TextButton(onClick = { step-- }) {
                        Text("Back", color = Purple)
                    }
                }

                Button(
                    onClick = {
                        if (step < 5) {
                            step++
                        } else {
                            // Validate inputs before proceeding
                            if (firstName.isBlank() || lastName.isBlank() ||
                                age.isBlank() || height.isBlank() || weight.isBlank() || goal.isBlank()) {
                                debugInfo = "Please fill in all fields"
                                return@Button
                            }

                            val ageInt = age.toIntOrNull()
                            val heightInt = height.toIntOrNull()
                            val weightInt = weight.toIntOrNull()

                            if (ageInt == null || heightInt == null || weightInt == null) {
                                debugInfo = "Please enter valid numbers for age, height, and weight"
                                return@Button
                            }

                            isLoading = true
                            debugInfo = "Saving profile..."

                            val profile = UserProfile(
                                uid = uid,
                                email = email,
                                firstName = firstName,
                                lastName = lastName,
                                age = ageInt,
                                heightCm = heightInt,
                                weightKg = weightInt,
                                goal = goal
                            )

                            // Save profile first
                            db.collection("users").document(uid).set(profile)
                                .addOnSuccessListener {
                                    debugInfo = "Profile saved! Generating workout..."
                                    Log.d("ProfileSave", "Profile saved successfully")

                                    coroutineScope.launch {
                                        try {
                                            // Check API key first
                                            val apiKey = com.example.myfitness.BuildConfig.GEMINI_API_KEY
                                            Log.d("APIKey", "API Key exists: ${apiKey.isNotBlank()}")
                                            Log.d("APIKey", "API Key length: ${apiKey.length}")

                                            if (apiKey.isBlank()) {
                                                debugInfo = "API Key not configured properly"
                                                isLoading = false
                                                return@launch
                                            }

                                            // Load existing workout progress and stats
                                            val existingWorkoutDoc = db.collection("users")
                                                .document(uid)
                                                .collection("workouts")
                                                .document("current")
                                                .get()
                                                .await()

                                            val existingStatsDoc = db.collection("users")
                                                .document(uid)
                                                .collection("stats")
                                                .get()
                                                .await()

                                            // Get the latest stats
                                            val latestStats = existingStatsDoc.documents
                                                .mapNotNull { it.toObject(WorkoutStats::class.java) }
                                                .maxByOrNull { it.lastUpdated }

                                            val userMap = mapOf(
                                                "age" to profile.age,
                                                "heightCm" to profile.heightCm,
                                                "weightKg" to profile.weightKg,
                                                "goal" to profile.goal
                                            )

                                            Log.d("WorkoutGen", "Generating workout for: $userMap")
                                            debugInfo = "Calling Gemini API..."

                                            val plan: JsonElement? = GeminiWorkoutGenerator.generateWorkoutPlan(userMap)

                                            if (plan != null) {
                                                Log.d("WorkoutPlan", "Generated plan: $plan")
                                                debugInfo = "Workout generated! Saving..."

                                                // Convert JsonElement to string for Firestore
                                                val planString = plan.toString()

                                                // Save new workout plan
                                                db.collection("users")
                                                    .document(uid)
                                                    .collection("workouts")
                                                    .document("current")
                                                    .set(mapOf(
                                                        "plan" to planString,
                                                        "lastUpdated" to System.currentTimeMillis()
                                                    ))
                                                    .addOnSuccessListener {
                                                        Log.d("WorkoutSave", "Workout saved successfully")
                                                        
                                                        // Preserve existing stats if they exist
                                                        if (latestStats != null) {
                                                            val statsCollection = db.collection("users")
                                                                .document(uid)
                                                                .collection("stats")
                                                            
                                                            // Create a new stats document with previous progress
                                                            val newStatsDoc = mapOf(
                                                                "completedExercises" to latestStats.completedExercises,
                                                                "totalCaloriesBurned" to latestStats.totalCaloriesBurned,
                                                                "daysCompleted" to latestStats.daysCompleted,
                                                                "lastUpdated" to System.currentTimeMillis()
                                                            )
                                                            
                                                            statsCollection.add(newStatsDoc)
                                                                .addOnSuccessListener {
                                                                    Log.d("StatsSave", "Stats preserved and new document created")
                                                                    Toast.makeText(context, "Profile and workout updated with previous progress!", Toast.LENGTH_SHORT).show()
                                                                    navController.navigate("home") {
                                                                        popUpTo("completeProfile") { inclusive = true }
                                                                    }
                                                                }
                                                                .addOnFailureListener { exception ->
                                                                    Log.e("StatsSave", "Failed to preserve stats", exception)
                                                                    debugInfo = "Failed to preserve stats: ${exception.message}"
                                                                    isLoading = false
                                                                }
                                                        } else {
                                                            // If no previous stats exist, create new empty stats
                                                            val statsCollection = db.collection("users")
                                                                .document(uid)
                                                                .collection("stats")
                                                            
                                                            val newStatsDoc = mapOf(
                                                                "completedExercises" to mapOf<String, Int>(),
                                                                "totalCaloriesBurned" to 0,
                                                                "daysCompleted" to 0,
                                                                "lastUpdated" to System.currentTimeMillis()
                                                            )
                                                            
                                                            statsCollection.add(newStatsDoc)
                                                                .addOnSuccessListener {
                                                                    Log.d("StatsSave", "New stats document created")
                                                                    Toast.makeText(context, "Profile and workout updated!", Toast.LENGTH_SHORT).show()
                                                                    navController.navigate("home") {
                                                                        popUpTo("completeProfile") { inclusive = true }
                                                                    }
                                                                }
                                                                .addOnFailureListener { exception ->
                                                                    Log.e("StatsSave", "Failed to create new stats", exception)
                                                                    debugInfo = "Failed to create new stats: ${exception.message}"
                                                                    isLoading = false
                                                                }
                                                        }
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        Log.e("WorkoutSave", "Failed to save workout", exception)
                                                        debugInfo = "Failed to save workout: ${exception.message}"
                                                        isLoading = false
                                                    }
                                            } else {
                                                Log.e("WorkoutGen", "Workout generation returned null")
                                                debugInfo = "Workout generation failed - check logs"
                                                isLoading = false
                                            }
                                        } catch (e: Exception) {
                                            Log.e("WorkoutGen", "Exception during workout generation", e)
                                            debugInfo = "Error: ${e.message}"
                                            isLoading = false
                                        }
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("ProfileSave", "Failed to save profile", exception)
                                    debugInfo = "Failed to save profile: ${exception.message}"
                                    isLoading = false
                                }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Purple
                            )
                            Text("Processing...", color = White)
                        }
                    } else {
                        Text(if (step < 5) "Next" else "Finish", color = White)
                    }
                }
            }
        }
    }
}

@Composable
fun StepInput(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = LightGray) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Purple,
            unfocusedBorderColor = LightGray,
            focusedLabelColor = Purple,
            unfocusedLabelColor = LightGray,
            cursorColor = Purple,
            focusedTextColor = White,
            unfocusedTextColor = White
        ),
        leadingIcon = leadingIcon
    )
}

@Composable
fun GoalPicker(current: String, onSelect: (String) -> Unit, leadingIcon: @Composable (() -> Unit)? = null) {
    val goals = listOf("Gain weight", "Lose weight", "Maintain weight", "Build muscle", "Improve endurance")
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text("Fitness Goal", color = LightGray) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Purple)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = LightGray,
                focusedLabelColor = Purple,
                unfocusedLabelColor = LightGray,
                cursorColor = Purple,
                focusedTextColor = White,
                unfocusedTextColor = White
            ),
            leadingIcon = leadingIcon
        )

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            goals.forEach { goal ->
                DropdownMenuItem(
                    text = { Text(goal, color = White) },
                    onClick = {
                        onSelect(goal)
                        expanded = false
                    }
                )
            }
        }
    }
}