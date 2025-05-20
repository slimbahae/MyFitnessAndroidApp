package com.example.myfitness.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myfitness.model.Exercise
import com.example.myfitness.model.WorkoutDay
import com.example.myfitness.model.WorkoutStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import com.example.myfitness.utils.NotificationHelper
import com.example.myfitness.utils.WorkoutTips
import com.example.myfitness.ui.components.RemindersDialog
import java.time.LocalTime
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.ImageLoader
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Dialog

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, exerciseDatabase: List<Exercise>) {
    val uid    = FirebaseAuth.getInstance().currentUser?.uid
    val db     = FirebaseFirestore.getInstance()
    val auth   = FirebaseAuth.getInstance()
    val scroll = rememberScrollState()

    // Scope pour lancer des coroutines depuis des callbacks
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // États UI
    var workout            by rememberSaveable { mutableStateOf<List<WorkoutDay>>(emptyList()) }
    var isLoading          by rememberSaveable { mutableStateOf(true) }
    var completedExercises by rememberSaveable { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCalories      by rememberSaveable { mutableStateOf(0) }
    var daysCompleted      by rememberSaveable { mutableStateOf(0) }
    var completedDays      by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var userName           by rememberSaveable { mutableStateOf("User") }
    var congratsDialog     by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    var lastUnlockedDayIndex by rememberSaveable { mutableStateOf(0) }
    var showRemindersDialog by rememberSaveable { mutableStateOf(false) }
    var showReminderDialog by rememberSaveable { mutableStateOf<Pair<String, String>?>(null) }
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    var savedReminders     by rememberSaveable { mutableStateOf<List<LocalTime>>(emptyList()) }
    var exerciseDatabase by rememberSaveable { mutableStateOf<List<Exercise>>(emptyList()) }
    var geminiResponse     by rememberSaveable { mutableStateOf("") }
    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }

    // Nom du jour courant, ex. "Monday"
    val todayName = LocalDate.now()
        .dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        .replaceFirstChar { it.uppercase() }

    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Load user data
    LaunchedEffect(Unit) {
        try {
            val userDoc = db.collection("users").document(uid!!).get().await()
            userName = userDoc.getString("firstName") ?: "User"
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error loading user data: ${e.message}")
        }
    }

    // Load exercise database
    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            exerciseDatabase = json.decodeFromString(jsonString)
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error loading exercises: ${e.message}")
        }
    }

    // Chargement initial du plan et des stats
    LaunchedEffect(Unit) {
        try {
            // Plan d'entraînement
            val planDoc = db.collection("users")
                .document(uid!!).collection("workouts")
                .document("current").get().await()
            planDoc.get("plan")?.let {
                workout = json.decodeFromString(it.toString())

                // Find the index of today's workout and set lastUnlockedDayIndex
                val todayIndex = workout.indexOfFirst { it.day.equals(todayName, ignoreCase = true) }
                lastUnlockedDayIndex = if (todayIndex != -1) todayIndex else 0 // Default to first day if today not found

            } ?: Log.e("HomeScreen", "No plan found")

            // Statistiques
            val statsDoc = db.collection("users")
                .document(uid).collection("stats")
                .document("current").get().await()
            if (statsDoc.exists()) {
                statsDoc.toObject(WorkoutStats::class.java)?.let { s ->
                    completedExercises = s.completedExercises
                    totalCalories      = s.totalCaloriesBurned
                    daysCompleted      = s.daysCompleted
                    completedDays      = workout
                        .filter { wday ->
                            wday.exerciseIds.any { exId -> completedExercises.containsKey(exId) }
                        }
                        .map { it.day }
                        .toSet()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "Error loading data: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    // Helper pour sauvegarder les stats
    fun saveStats() {
        if (uid == null) return
        val stats = WorkoutStats(
            completedExercises = completedExercises,
            totalCaloriesBurned = totalCalories,
            daysCompleted       = daysCompleted
        )
        db.collection("users")
            .document(uid)
            .collection("stats")
            .document("current")
            .set(stats)
    }

    // Function to reset stats
    fun resetStats() {
        completedExercises = emptyMap()
        totalCalories = 0
        daysCompleted = 0
        completedDays = emptySet()
        saveStats()
        coroutineScope.launch {
            snackbarHostState.showSnackbar("Stats have been reset!")
        }
    }

    // Calculate total time and exercises for completed days
    val completedWorkoutDays = workout.filter { completedDays.contains(it.day) }
    val totalTime = completedWorkoutDays.sumOf { it.duration }
    val totalExercises = completedWorkoutDays.sumOf { it.exerciseIds.size }

    // Add this function to check and send workout reminders
    fun checkAndSendWorkoutReminders() {
        val currentTime = LocalTime.now()
        savedReminders.forEach { reminderTime ->
            // If it's time for a reminder (within 1 minute of the set time)
            if (currentTime.hour == reminderTime.hour && 
                currentTime.minute == reminderTime.minute) {
                
                // Find today's workout
                val todayWorkout = workout.find { it.day.equals(todayName, ignoreCase = true) }
                
                if (todayWorkout != null) {
                    // Calculate remaining exercises and sets
                    val remainingExercises = todayWorkout.exerciseIds.count { exerciseId ->
                        (completedExercises[exerciseId] ?: 0) < 1
                    }
                    
                    val totalSets = todayWorkout.exerciseIds.size
                    val completedSets = todayWorkout.exerciseIds.sumOf { 
                        completedExercises[it] ?: 0 
                    }
                    val remainingSets = totalSets - completedSets

                    // Create meaningful notification message
                    val message = when {
                        remainingExercises == 0 -> "Great job! You've completed all exercises for today! 🎉"
                        remainingSets == 1 -> "Just one set left to complete today's workout! 💪"
                        else -> "Time for your workout! You have $remainingExercises exercises and $remainingSets sets remaining today. ${WorkoutTips.getRandomMotivationTip()}"
                    }

                    // Show in-app notification
                    showReminderDialog = Pair("Workout Time! 💪", message)

                    // Send system notification
                    notificationHelper.showWorkoutReminder(
                        "Workout Time! 💪",
                        message
                    )
                } else {
                    val message = "It's your workout time! Check your weekly plan for today's exercises. ${WorkoutTips.getRandomMotivationTip()}"
                    
                    // Show in-app notification
                    showReminderDialog = Pair("Workout Reminder", message)

                    // Send system notification
                    notificationHelper.showWorkoutReminder(
                        "Workout Reminder",
                        message
                    )
                }
            }
        }
    }

    // Add this function to send progress updates
    fun sendProgressUpdate() {
        val totalWorkouts = workout.size
        val completedWorkouts = completedDays.size
        val remainingWorkouts = totalWorkouts - completedWorkouts

        val message = when {
            completedWorkouts == 0 -> "Start your fitness journey today! Your first workout is waiting for you. ${WorkoutTips.getRandomMotivationTip()}"
            remainingWorkouts == 0 -> "Congratulations! You've completed all workouts for this week! 🎉"
            remainingWorkouts == 1 -> "You're almost there! Just one workout left to complete this week. Keep going! 💪"
            else -> "You've completed $completedWorkouts out of $totalWorkouts workouts this week. $remainingWorkouts workouts remaining! ${WorkoutTips.getRandomMotivationTip()}"
        }

        notificationHelper.showWorkoutReminder(
            "Weekly Progress Update 📊",
            message
        )
    }

    // Update the saveReminders function
    fun saveReminders(reminders: List<LocalTime>) {
        savedReminders = reminders
        // Send initial progress update
        sendProgressUpdate()
    }

    // Add periodic checks for reminders
    LaunchedEffect(Unit) {
        while (true) {
            delay(60 * 1000L) // Check every minute
            checkAndSendWorkoutReminders()
        }
    }

    // Add periodic progress updates
    LaunchedEffect(Unit) {
        while (true) {
            delay(4 * 60 * 60 * 1000L) // Every 4 hours
            sendProgressUpdate()
        }
    }

    // Add Gemini API test section at the bottom of HomeScreen
    val geminiScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRemindersDialog = true },
                containerColor = Purple,
                contentColor = White
            ) {
                Icon(Icons.Filled.Notifications, contentDescription = "Set Reminders")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkGray,
                contentColor = White
            ) {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = { 
                        if (currentRoute != "home") {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple,
                        selectedTextColor = Purple,
                        indicatorColor = Color(0xFF232323)
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == "statistics", // Workouts/Stats screen
                    onClick = { 
                        if (currentRoute != "statistics") {
                            navController.navigate("statistics") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                     },
                    icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Workouts") },
                    label = { Text("Workouts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple,
                        selectedTextColor = Purple,
                        indicatorColor = Color(0xFF232323)
                    )
                )
                NavigationBarItem(
                    selected = currentRoute == "profile",
                    onClick = { 
                        if (currentRoute != "profile") {
                            navController.navigate("profile") {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                     },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Purple,
                        selectedTextColor = Purple,
                        indicatorColor = Color(0xFF232323)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(bottom = 80.dp)
            ) {
                // Greeting Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape),
                        tint = Purple
                    )
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text("Welcome back,", fontSize = 16.sp, color = LightGray)
                        Text(userName+ " 👋", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        auth.signOut()
                        navController.navigate("login") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Purple)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Stats Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = "Stats",
                            tint = Purple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "My Stats",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    IconButton(
                        onClick = { resetStats() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset Stats",
                            tint = Purple
                        )
                    }
                }

                // Redesigned Stats Cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { navController.navigate("statistics") },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Calories Card
                    Card(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(120.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Purple),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Whatshot,
                                contentDescription = "Calories Burnt",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp).padding(bottom = 2.dp)
                            )
                            Text(
                                text = "${totalCalories}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "kcal burnt",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    // Time & Exercises Cards
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F7E6)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = "Total Time",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${totalTime / 60}h",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF181818)
                                    )
                                    Text(
                                        text = "total time",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B6B6B)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = "Exercises",
                                    tint = Color(0xFF8F5AFF),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$totalExercises",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF181818)
                                    )
                                    Text(
                                        text = "exercises",
                                        fontSize = 13.sp,
                                        color = Color(0xFF6B6B6B)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Workout Cards
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Purple
                    )
                } else {
                    // Weekly Plan Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Weekly Plan",
                            tint = Purple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "My Weekly Plan",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                    
                    // Find the index of the first incomplete day
                    val firstIncompleteDayIndex = workout.indexOfFirst { !completedDays.contains(it.day) }.let { if (it == -1) workout.lastIndex else it }

                    workout.forEachIndexed { index, day ->
                        val locked = index > firstIncompleteDayIndex
                        val completed = completedDays.contains(day.day)
                        val isRestDay = day.type.equals("Rest", ignoreCase = true)
                        val exercisesForDay = day.exerciseIds.mapNotNull { id -> exerciseDatabase.find { it.id == id } }
                        WorkoutDayCard(
                            day = day,
                            exercises = exercisesForDay,
                            isLocked = locked,
                            isCompleted = completed,
                            completedExercises = completedExercises,
                            isRestDay = isRestDay,
                            onExerciseClick = { ex -> if (!isRestDay) selectedExerciseId = ex.id },
                            onExerciseComplete = { id ->
                                if (isRestDay) {
                                    if (!completedDays.contains(day.day)) {
                                        completedDays = completedDays + day.day
                                        daysCompleted = completedDays.size
                                        saveStats()
                                        congratsDialog = Pair("🛌", "You completed your rest day!")
                                    }
                                    return@WorkoutDayCard
                                }
                                val prev = completedExercises[id] ?: 0
                                val sets = 3 // Default sets per exercise
                                if (prev < sets) {
                                    completedExercises = completedExercises + (id to (prev + 1))
                                    totalCalories += day.calories / (day.exerciseIds.size.takeIf { it > 0 } ?: 1)
                                    if ((completedExercises[id] ?: 0) + 1 == sets) {
                                        congratsDialog = Pair("🎉", "Exercise completed!")
                                    }
                                    if (
                                        day.exerciseIds.all { exId -> (completedExercises[exId] ?: 0) >= sets } &&
                                        !completedDays.contains(day.day)
                                    ) {
                                        completedDays = completedDays + day.day
                                        daysCompleted = completedDays.size
                                        saveStats()
                                        congratsDialog = Pair("🏆", "You completed today's workout!")
                                    }
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

            }

            // Congratulation Modal
            congratsDialog?.let { (emoji, message) ->
                AlertDialog(
                    onDismissRequest = { congratsDialog = null },
                    containerColor = Color.White,
                    title = {
                        Text(
                            text = emoji,
                            fontSize = 64.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Text(
                            text = message,
                            fontSize = 22.sp,
                            color = Color(0xFF181818),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { congratsDialog = null }) {
                            Text("OK", color = Purple, fontSize = 18.sp)
                        }
                    }
                )
            }
            // Reminder Dialog
            showReminderDialog?.let { (title, message) ->
                AlertDialog(
                    onDismissRequest = { showReminderDialog = null },
                    containerColor = DarkGray,
                    title = {
                        Text(
                            text = title,
                            color = White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    text = {
                        Text(
                            text = message,
                            color = LightGray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showReminderDialog = null }
                        ) {
                            Text("OK", color = Purple)
                        }
                    }
                )
            }
        }

        // Add this at the end of the HomeScreen composable
        if (showRemindersDialog) {
            RemindersDialog(
                onDismiss = { showRemindersDialog = false },
                onSaveReminders = { reminders ->
                    saveReminders(reminders)
                    showRemindersDialog = false
                },
                initialReminders = savedReminders
            )
        }

        // Show ExerciseDetailScreen as a dialog when selectedExerciseId is not null
        selectedExerciseId?.let { exId ->
            Dialog(onDismissRequest = { selectedExerciseId = null }) {
                ExerciseDetailScreen(
                    exerciseId = exId,
                    exerciseDatabase = exerciseDatabase,
                    onBack = { selectedExerciseId = null }
                )
            }
        }
    }
}

@Composable
fun WorkoutDayCard(
    day: WorkoutDay,
    exercises: List<Exercise>,
    isLocked: Boolean,
    isCompleted: Boolean,
    completedExercises: Map<String, Int>,
    isRestDay: Boolean = false,
    onExerciseClick: (Exercise) -> Unit,
    onExerciseComplete: (String) -> Unit
) {
    if (isRestDay) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF232323)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = "Rest Day",
                        tint = Purple,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.width(18.dp))
                    Column {
                        Text(
                            "${day.day} - Rest Day",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            day.notes.ifBlank { "Take a break and recover!" },
                            color = Color(0xFFB0AFC6),
                            fontSize = 16.sp
                        )
                    }
                }
                if (!isLocked && !isCompleted) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Mark rest day as completed
                            onExerciseComplete("rest_${day.day}")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mark Rest Day Done")
                    }
                } else if (isCompleted) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Completed",
                            tint = Purple,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Rest Day Completed!", color = Purple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
        return
    }

    val cardBackgroundColor = when {
        isCompleted -> Color(0xFFE8F7E6)
        isLocked -> Color(0xFF181818)
        else -> Color(0xFFF8F6FF)
    }
    val textColor = if (isLocked) Color(0xFFB0AFC6) else Color(0xFF181818)
    val purpleTextColor = if (isLocked) Color(0xFFB0AFC6) else Purple
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (isLocked) 0.5f else 1f)
            .clickable(enabled = !isLocked) { },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header Row
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.FitnessCenter,
                        contentDescription = "Workout Icon",
                        tint = purpleTextColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${day.day} - ${day.muscleGroup}",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(day.type, color = purpleTextColor, fontSize = 15.sp)
                    }
                }
                // Display completion icon or duration
                if (isCompleted) {
                     Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed",
                        tint = Purple,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text("${day.duration} min", color = Color(0xFF6B6B6B), fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Exercises List
            Column {
                exercises.forEach { ex ->
                    val sets = 3 // Default sets per exercise
                    val doneSets = completedExercises[ex.id] ?: 0
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLocked) { onExerciseClick(ex) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                ex.name,
                                color = textColor,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Sets: ", color = textColor, fontSize = 14.sp)
                                (1..sets).forEach { setNum ->
                                    val checked = setNum <= doneSets
                                    IconButton(
                                        onClick = {
                                            if (!isLocked && doneSets < setNum) {
                                                onExerciseComplete(ex.id)
                                            }
                                        },
                                        enabled = !isLocked && !isCompleted && !checked
                                    ) {
                                        Icon(
                                            imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.FitnessCenter,
                                            contentDescription = if (checked) "Set done" else "Mark set done",
                                            tint = if (checked) Purple else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (!isLocked && !isCompleted && doneSets >= sets) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Done",
                                    tint = Purple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Done", color = Purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🔥 ${day.calories} kcal", color = purpleTextColor, fontWeight = FontWeight.Bold)
                 // Update text based on state
                val statusText = when {
                    isCompleted -> "Completed!"
                    isLocked -> "Available on ${day.day}"
                    else -> "Tap exercise for details"
                }
                Text(statusText, color = Color(0xFF6B6B6B), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun AsyncImage1(
    model: Any,
    contentDescription: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onLoading: @Composable () -> Unit = {},
    onError: @Composable () -> Unit = {}
) {
    val painter = coil.compose.rememberAsyncImagePainter(
        model = model,
        imageLoader = imageLoader
    )
    val state = painter.state

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.fillMaxSize()
        )

        when (state) {
            is AsyncImagePainter.State.Loading -> onLoading()
            is AsyncImagePainter.State.Error -> onError()
            else -> {}
        }
    }
}
