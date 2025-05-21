package com.example.myfitness.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
import androidx.compose.ui.layout.ContentScale
import com.example.myfitness.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.icons.filled.Logout

@OptIn(ExperimentalAnimationApi::class)
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
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Logout button at the top right
        IconButton(
            onClick = {
                auth.signOut()
                navController.navigate(com.example.myfitness.Routes.LOGIN) {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Logout,
                contentDescription = "Logout",
                tint = Purple
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Profile Setup (${step + 1}/7)",
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = White,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Step-specific engaging text
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
                },
                label = "stepText"
            ) { currentStep ->
                when (currentStep) {
                    0 -> EngagingText(
                        "Let's start with your first name!",
                        "This helps us personalize your fitness journey.",
                        scale
                    )
                    1 -> EngagingText(
                        "Now, your last name!",
                        "We're almost done with the basics.",
                        scale
                    )
                    2 -> EngagingText(
                        "How young are you?",
                        "Your age helps us create age-appropriate workouts.",
                        scale
                    )
                    3 -> EngagingText(
                        "What's your gender?",
                        "This helps us tailor your fitness plan perfectly.",
                        scale
                    )
                    4 -> EngagingText(
                        "How tall are you?",
                        "Your height is crucial for calculating your ideal workout intensity.",
                        scale
                    )
                    5 -> EngagingText(
                        "What's your current weight?",
                        "This helps us track your progress effectively.",
                        scale
                    )
                    6 -> EngagingText(
                        "What's your fitness goal?",
                        "Let's make your dreams a reality!",
                        scale
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            // Animated illustrations
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500)) with 
                    fadeOut(animationSpec = tween(500)) + slideOutVertically(animationSpec = tween(500))
                },
                label = "illustrations"
            ) { currentStep ->
                when (currentStep) {
                    0, 1 -> Image(
                        painter = painterResource(id = R.drawable.name),
                        contentDescription = "Name Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                    2 -> Image(
                        painter = painterResource(id = R.drawable.cake),
                        contentDescription = "Cake Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                    3 -> Image(
                        painter = painterResource(id = R.drawable.gender),
                        contentDescription = "Gender Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                    4 -> Image(
                        painter = painterResource(id = R.drawable.height),
                        contentDescription = "Height Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                    5 -> Image(
                        painter = painterResource(id = R.drawable.weight),
                        contentDescription = "Weight Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                    6 -> Image(
                        painter = painterResource(id = R.drawable.goal),
                        contentDescription = "Goal Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(200.dp)
                            .padding(bottom = 24.dp)
                    )
                }
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
                        3 -> GenderPicker(gender, onSelect = { gender = it }, leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        4 -> StepInput("Height (cm)", height, { height = it }, keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Outlined.Height, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        5 -> StepInput("Weight (kg)", weight, { weight = it }, keyboardType = KeyboardType.Number, leadingIcon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
                        6 -> GoalPicker(goal, onSelect = { goal = it }, leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp)) })
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
                        if (step < 6) {
                            step++
                        } else {
                            // Validate inputs before proceeding
                            if (firstName.isBlank() || lastName.isBlank() ||
                                age.isBlank() || gender.isBlank() || height.isBlank() || weight.isBlank() || goal.isBlank()) {
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
                                gender = gender,
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
                                                "gender" to profile.gender,
                                                "heightCm" to profile.heightCm,
                                                "weightKg" to profile.weightKg,
                                                "goal" to profile.goal
                                            )

                                            Log.d("WorkoutGen", "Generating workout for: $userMap")
                                            debugInfo = "Calling Gemini API..."

                                            val plan: JsonElement? = GeminiWorkoutGenerator.generateWorkoutPlan(userMap, context)

                                            if (plan != null) {
                                                Log.d("WorkoutPlan", "Generated plan: $plan")
                                                debugInfo = "Workout generated! Saving..."

                                                // Convert JsonElement to string for Firestore
                                                val planString = plan.toString()

                                                // Save new workout plan (already in the correct format)
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
                        Text(if (step < 6) "Next" else "Finish", color = White)
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
fun GenderPicker(current: String, onSelect: (String) -> Unit, leadingIcon: @Composable (() -> Unit)? = null) {
    val genders = listOf("Male", "Female", "Other")
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = current,
            onValueChange = {},
            readOnly = true,
            label = { Text("Gender", color = LightGray) },
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DarkGray)
                .fillMaxWidth(0.9f)
        ) {
            genders.forEach { gender ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            gender,
                            color = White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelect(gender)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = White,
                        leadingIconColor = Purple,
                        trailingIconColor = Purple,
                        disabledTextColor = LightGray,
                        disabledLeadingIconColor = LightGray,
                        disabledTrailingIconColor = LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(DarkGray)
                .fillMaxWidth(0.9f)
        ) {
            goals.forEach { goal ->
                DropdownMenuItem(
                    text = { 
                        Text(
                            goal,
                            color = White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    onClick = {
                        onSelect(goal)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = White,
                        leadingIconColor = Purple,
                        trailingIconColor = Purple,
                        disabledTextColor = LightGray,
                        disabledLeadingIconColor = LightGray,
                        disabledTrailingIconColor = LightGray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EngagingText(title: String, subtitle: String, scale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Purple,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .scale(scale)
                .padding(bottom = 8.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LightGray,
                fontSize = 16.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}