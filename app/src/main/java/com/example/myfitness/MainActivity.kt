package com.example.myfitness

import GeminiTestScreen
import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.ui.platform.LocalContext
import com.example.myfitness.model.Exercise
import com.example.myfitness.ui.CompleteProfileScreen
import com.example.myfitness.ui.ExerciseDetailScreen
import com.example.myfitness.ui.HomeScreen
import com.example.myfitness.ui.LoginScreen
import com.example.myfitness.ui.ProfileScreen
import com.example.myfitness.ui.RegisterScreen
import com.example.myfitness.ui.ReminderSettingsScreen
import com.example.myfitness.ui.StatisticsScreen
import com.example.myfitness.ui.theme.MyFitnessTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

// Route constants
object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val COMPLETE_PROFILE = "completeProfile"
    const val STATISTICS = "statistics"
    const val PROFILE = "profile"
    const val REMINDER_SETTINGS = "reminderSettings"
}

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "Notification permission granted")
        } else {
            Log.e("MainActivity", "Notification permission denied")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MyFitnessTheme {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                var startDestination by remember { mutableStateOf<String?>(null) }
                val exerciseDatabase = remember { mutableStateOf<List<Exercise>>(emptyList()) }
                val context = LocalContext.current

                // 🔄 Determine where to start
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    startDestination = when {
                        user == null -> Routes.LOGIN
                        else -> {
                            val userRef = firestore.collection("users").document(user.uid)
                            val userDoc = userRef.get().await()
                            val workoutDoc = userRef.collection("workouts")
                                .document("current").get().await()

                            if (!userDoc.exists()) {
                                Routes.COMPLETE_PROFILE
                            } else if (!workoutDoc.exists()) {
                                Routes.COMPLETE_PROFILE
                            } else {
                                Routes.HOME
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
                    exerciseDatabase.value = Json { ignoreUnknownKeys = true }
                        .decodeFromString(jsonString)
                }

                // 🚀 Show UI when startDestination is known
                if (startDestination != null) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background
                    ) { paddingValues ->
                        Box(modifier = Modifier.padding(paddingValues)) {
                            AppNavigation(
                                navController = navController,
                                auth = auth,
                                startDestination = startDestination!!,
                                exerciseDatabase = exerciseDatabase.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    navController: NavHostController,
    auth: FirebaseAuth,
    startDestination: String,
    exerciseDatabase: List<Exercise>
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController, auth = auth)
        }
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController, auth = auth)
        }
        composable(Routes.HOME) {
            HomeScreen(navController = navController, exerciseDatabase = exerciseDatabase)
        }
        composable(Routes.COMPLETE_PROFILE) {
            CompleteProfileScreen(
                navController = navController,
                auth = auth
            )
        }
        composable(Routes.STATISTICS) {
            StatisticsScreen(navController = navController)
        }
        composable(Routes.PROFILE) {
            ProfileScreen(navController = navController)
        }
        composable(Routes.REMINDER_SETTINGS) {
            ReminderSettingsScreen(navController = navController)
        }
        composable(
            "exerciseDetail/{exerciseId}",
            arguments = listOf(navArgument("exerciseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val exerciseId = backStackEntry.arguments?.getString("exerciseId") ?: ""
            ExerciseDetailScreen(
                exerciseId = exerciseId,
                exerciseDatabase = exerciseDatabase,
                onBack = { navController.popBackStack() }
            )
        }
        composable("geminiTest") {
            GeminiTestScreen(context = LocalContext.current)
        }
    }
}
