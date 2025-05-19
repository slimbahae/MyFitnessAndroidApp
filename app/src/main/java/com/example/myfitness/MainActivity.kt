package com.example.myfitness

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.myfitness.ui.CompleteProfileScreen
import com.example.myfitness.ui.HomeScreen
import com.example.myfitness.ui.LoginScreen
import com.example.myfitness.ui.ProfileScreen
import com.example.myfitness.ui.RegisterScreen
import com.example.myfitness.ui.StatisticsScreen
import com.example.myfitness.ui.theme.MyFitnessTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyFitnessTheme {
                val navController = rememberNavController()
                val auth = FirebaseAuth.getInstance()
                val firestore = FirebaseFirestore.getInstance()
                var startDestination by remember { mutableStateOf<String?>(null) }

                // 🔄 Determine where to start
                LaunchedEffect(Unit) {
                    val user = auth.currentUser
                    startDestination = when {
                        user == null -> "login"
                        else -> {
                            val userRef = firestore.collection("users").document(user.uid)
                            val userDoc = userRef.get().await()
                            val workoutDoc = userRef.collection("workouts")
                                .document("current").get().await()

                            if (!userDoc.exists()) {
                                "completeProfile"
                            } else if (!workoutDoc.exists()) {
                                "completeProfile"
                            } else {
                                "home"
                            }
                        }
                    }
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
                                startDestination = startDestination!!
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
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("register") {
            RegisterScreen(navController = navController, auth = auth)
        }
        composable("login") {
            LoginScreen(navController = navController, auth = auth)
        }
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("completeProfile") {
            CompleteProfileScreen(
                navController = navController,
                auth = auth
            )
        }
        composable("statistics") {
            StatisticsScreen(navController = navController)
        }
        composable("profile") {
            ProfileScreen(navController = navController)
        }
    }
}
