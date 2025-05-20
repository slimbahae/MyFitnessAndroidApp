package com.example.myfitness.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myfitness.model.WorkoutStats
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ShowChart

// You will need to import the necessary components from your charting library
// Example: import com.your_charting_library.LineChart

val Black = Color(0xFF000000)
val Purple = Color(0xFF8F5AFF)
val DarkGray = Color(0xFF181818)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF5F5F5)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(navController: NavController, exerciseDatabase: List<com.example.myfitness.model.Exercise>? = null) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()

    var weeklyStats by remember { mutableStateOf<List<WorkoutStats>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Load historical stats
    LaunchedEffect(Unit) {
        try {
            val statsCollection = db.collection("users").document(uid!!).collection("stats")
            val stats = statsCollection.get().await()
            weeklyStats = stats.documents.mapNotNull { it.toObject(WorkoutStats::class.java) }
                .sortedBy { it.lastUpdated }
        } catch (e: Exception) {
            // Handle error
        } finally {
            isLoading = false
        }
    }

    Scaffold(
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
                    selected = currentRoute == "statistics",
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
                .background(Black)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp)
                    .verticalScroll(scrollState)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Black)
                        .padding(top = 36.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📊 Detailed Statistics",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = White
                    )
                    TextButton(onClick = { navController.navigateUp() }) {
                        Text("← Back", color = Purple, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Purple
                    )
                } else {
                    val latestStats = weeklyStats.lastOrNull()

                    // Weekly Progress Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("This Week's Progress", fontWeight = FontWeight.SemiBold, color = White, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            val daysCompleted = latestStats?.daysCompleted ?: 0
                            LinearProgressIndicator(
                                progress = daysCompleted / 7f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = Purple,
                                trackColor = Color(0xFF232323)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Days Completed", color = LightGray, fontSize = 14.sp)
                                    Text("$daysCompleted/7", color = Purple, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column {
                                    Text("Total Calories", color = LightGray, fontSize = 14.sp)
                                    Text("${latestStats?.totalCaloriesBurned ?: 0} kcal", color = Purple, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                        }
                    }

                    // Exercise Completion Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Exercise Completion", fontWeight = FontWeight.SemiBold, color = White, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            latestStats?.completedExercises?.forEach { (exercise, sets) ->
                                val displayName = exerciseDatabase?.find { it.id == exercise }?.name ?: exercise
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(displayName, color = LightGray, fontSize = 15.sp)
                                    Text("$sets sets", color = Purple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Exercise Evolution Chart Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FitnessCenter,
                            contentDescription = "Exercise Chart Icon",
                            tint = Purple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Exercise Evolution",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }

                    // Exercise Evolution Chart Placeholder
                    // You need to replace this placeholder with the actual chart composable from your library.
                    // Pass 'exerciseChartData' to the chart composable.
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (weeklyStats.isNotEmpty()) {
                                // Data preparation for Exercise Evolution Chart (Cumulative Exercises)
                                var cumulativeExercises = 0
                                val exerciseChartData = weeklyStats.map { stats ->
                                    cumulativeExercises += stats.completedExercises.values.sum()
                                    // Assuming the chart library uses a Pair of (x-value, y-value)
                                    // Replace 'stats.lastUpdated.toFloat()' with appropriate x-axis value (e.g., date index or formatted date string)
                                    Pair(stats.lastUpdated.toFloat(), cumulativeExercises.toFloat())
                                }

                                // Replace this Text with your chart composable, e.g.:
                                // LineChart(data = exerciseChartData, ...) or similar
                                // Text("Exercise Evolution Chart Placeholder", color = LightGray)
                                 /* Example using a hypothetical library:
                                 LineChart(
                                     data = exerciseChartData,
                                     modifier = Modifier.fillMaxSize().padding(16.dp),
                                     lineColor = Purple,
                                     pointColor = White
                                     // ... other chart customization parameters
                                 )
                                 */

                            } else {
                                 Text("No exercise data available", color = LightGray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                     // Calories Evolution Chart Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Calories Chart Icon",
                            tint = Purple,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Calories Evolution",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }

                     // Calories Evolution Chart Placeholder
                    // You need to replace this placeholder with the actual chart composable from your library.
                    // Pass 'caloriesChartData' to the chart composable.
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (weeklyStats.isNotEmpty()) {
                                // Data preparation for Calories Evolution Chart (Cumulative Calories)
                                var cumulativeCalories = 0
                                 val caloriesChartData = weeklyStats.map { stats ->
                                    cumulativeCalories += stats.totalCaloriesBurned
                                     // Assuming the chart library uses a Pair of (x-value, y-value)
                                    // Replace 'stats.lastUpdated.toFloat()' with appropriate x-axis value (e.g., date index or formatted date string)
                                    Pair(stats.lastUpdated.toFloat(), cumulativeCalories.toFloat())
                                }
                                // Replace this Text with your chart composable, e.g.:
                                // LineChart(data = caloriesChartData, ...) or similar
                                 //Text("Calories Evolution Chart Placeholder", color = LightGray)
                                 /* Example using a hypothetical library:
                                 LineChart(
                                     data = caloriesChartData,
                                     modifier = Modifier.fillMaxSize().padding(16.dp),
                                     lineColor = Purple,
                                     pointColor = White
                                     // ... other chart customization parameters
                                 )
                                 */
                            } else {
                                 Text("No calorie data available", color = LightGray)
                            }
                        }
                    }

                     Spacer(modifier = Modifier.height(24.dp))

                    // Weekly History Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkGray),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Weekly History", fontWeight = FontWeight.SemiBold, color = White, fontSize = 18.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            weeklyStats.forEach { stats ->
                                val date = Date(stats.lastUpdated)
                                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
                                val formattedDate = LocalDate.ofEpochDay(stats.lastUpdated / (24 * 60 * 60 * 1000))
                                    .format(formatter)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(formattedDate, color = LightGray, fontSize = 15.sp)
                                    Text("${stats.totalCaloriesBurned} kcal", color = Purple, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Placeholder for charting library integration
// You would typically use a charting library like MPAndroidChart (View-based) or Compose Chart (Compose-based)
// Add the dependency to your app/build.gradle file:
// example for Compose Chart:
// implementation "com.github.aayushatharva.broton:compose-chart:<version>"
// implementation "com.github.aayushatharva.broton:compose-chart-common:<version>"

// Example of how you might define a composable for a chart (requires a charting library)
/*
@Composable
fun ExerciseEvolutionChart(weeklyStats: List<WorkoutStats>) {
    // Data transformation and charting logic here
    // For example, extract dates and total exercises for each entry
    val chartData = weeklyStats.map { stats ->
        val date = LocalDate.ofEpochDay(stats.lastUpdated / (24 * 60 * 60 * 1000))
        val totalExercises = stats.completedExercises.values.sum()
        // Use this data with your chosen charting library composable
        // Example: LineChart(data = chartData, ...)
    }
}

@Composable
fun CaloriesEvolutionChart(weeklyStats: List<WorkoutStats>) {
    // Data transformation and charting logic here
    // For example, extract dates and total calories burned for each entry
     val chartData = weeklyStats.map { stats ->
        val date = LocalDate.ofEpochDay(stats.lastUpdated / (24 * 60 * 60 * 1000))
        val totalCalories = stats.totalCaloriesBurned
        // Use this data with your chosen charting library composable
        // Example: LineChart(data = chartData, ...)
    }
}
*/ 