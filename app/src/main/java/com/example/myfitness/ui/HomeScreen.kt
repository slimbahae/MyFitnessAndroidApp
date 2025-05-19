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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myfitness.R
import com.example.myfitness.model.Exercise
import com.example.myfitness.model.WorkoutDay
import com.example.myfitness.model.WorkoutStats
import com.example.myfitness.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val uid    = FirebaseAuth.getInstance().currentUser?.uid
    val db     = FirebaseFirestore.getInstance()
    val auth   = FirebaseAuth.getInstance()
    val scroll = rememberScrollState()

    // Scope pour lancer des coroutines depuis des callbacks
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // États UI
    var workout            by remember { mutableStateOf<List<WorkoutDay>>(emptyList()) }
    var isLoading          by remember { mutableStateOf(true) }
    var completedExercises by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var totalCalories      by remember { mutableStateOf(0) }
    var daysCompleted      by remember { mutableStateOf(0) }
    var completedDays      by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedExercise   by remember { mutableStateOf<Exercise?>(null) }
    var userName          by remember { mutableStateOf("User") }
    var congratsDialog    by remember { mutableStateOf<Pair<String, String>?>(null) }

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

    // Chargement initial du plan et des stats
    LaunchedEffect(Unit) {
        try {
            // Plan d'entraînement
            val planDoc = db.collection("users")
                .document(uid!!).collection("workouts")
                .document("current").get().await()
            planDoc.get("plan")?.let {
                workout = json.decodeFromString(it.toString())
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
                            wday.exercises.any { ex -> completedExercises.containsKey(ex.name) }
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
    val totalExercises = completedWorkoutDays.sumOf { it.exercises.size }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("statistics") },
                containerColor = Purple,
                contentColor = White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "View Stats")
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
                        Text(userName, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = White)
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
                    
                    workout.forEach { day ->
                        val locked = !day.day.equals(todayName, ignoreCase = true)
                        val completed = completedDays.contains(day.day)
                        WorkoutDayCard(
                            day = day,
                            isLocked = locked,
                            isCompleted = completed,
                            completedExercises = completedExercises,
                            onExerciseClick = { ex -> selectedExercise = ex },
                            onExerciseComplete = { name ->
                                val prev = completedExercises[name] ?: 0
                                completedExercises = completedExercises + (name to (prev + 1))
                                
                                // Calculate calories per set instead of per exercise
                                val caloriesPerSet = day.calories / day.exercises.sumOf { it.sets }
                                totalCalories += caloriesPerSet

                                // If this was the last set for this exercise
                                val exercise = day.exercises.find { it.name == name }
                                if (exercise != null && prev + 1 == exercise.sets) {
                                    congratsDialog = Pair("🎉", "Exercise completed!")
                                }

                                // Si tous les exos sont faits pour ce jour
                                if (
                                    day.exercises.all { ex -> (completedExercises[ex.name] ?: 0) >= ex.sets } &&
                                    !completedDays.contains(day.day)
                                ) {
                                    completedDays = completedDays + day.day
                                    daysCompleted = completedDays.size
                                    saveStats()
                                    congratsDialog = Pair("🏆", "You completed today's workout!")
                                }
                            }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Exercice Detail Dialog
            selectedExercise?.let { ex ->
                AlertDialog(
                    onDismissRequest = { selectedExercise = null },
                    containerColor = DarkGray,
                    title = { Text(ex.name, color = White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            // Static Video Placeholder
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Black),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Play Video",
                                        tint = Purple,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Sets
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Repeat, contentDescription = "Sets Icon", tint = Purple)
                                Spacer(Modifier.width(8.dp))
                                Text("Sets: ${ex.sets}", color = LightGray, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            // Reps
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Repeat, contentDescription = "Reps Icon", tint = Purple)
                                Spacer(Modifier.width(8.dp))
                                Text("Reps: ${ex.reps}", color = LightGray, fontSize = 16.sp)
                            }
                             Spacer(modifier = Modifier.height(8.dp))

                            // Instructions
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Description, contentDescription = "Instructions Icon", tint = Purple)
                                Spacer(Modifier.width(8.dp))
                                Text("Instructions: ${ex.instructions}", color = LightGray, fontSize = 16.sp)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedExercise = null }) {
                            Text("OK", color = Purple)
                        }
                    }
                )
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
        }
    }
}

@Composable
fun WorkoutDayCard(
    day: WorkoutDay,
    isLocked: Boolean,
    isCompleted: Boolean,
    completedExercises: Map<String, Int>,
    onExerciseClick: (Exercise) -> Unit,
    onExerciseComplete: (String) -> Unit
) {
    val cardBackgroundColor = when {
        isCompleted -> Color(0xFFE8F7E6) // Light green for completed
        isLocked -> Color(0xFF181818) // Dark gray for locked
        else -> Color(0xFFF8F6FF) // Light purple for active/incomplete
    }

    val textColor = if (isLocked) Color(0xFFB0AFC6) else Color(0xFF181818)
    val purpleTextColor = if (isLocked) Color(0xFFB0AFC6) else Purple

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .alpha(if (isLocked) 0.5f else 1f)
            .clickable(enabled = !isLocked) { /* jours verrouillés inactifs */ },
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
                day.exercises.forEach { ex ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLocked) { onExerciseClick(ex) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ex.name,
                            color = textColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                        val done = completedExercises[ex.name] ?: 0
                        if (!isLocked && !isCompleted) { // Hide buttons if locked or completed
                            if (done < ex.sets) {
                                Button(
                                    onClick = { onExerciseComplete(ex.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Text("Set ${done + 1}/${ex.sets}", fontSize = 14.sp)
                                }
                            } else {
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
