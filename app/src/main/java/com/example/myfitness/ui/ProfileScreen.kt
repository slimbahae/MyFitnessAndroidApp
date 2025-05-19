package com.example.myfitness.ui

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myfitness.ui.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }

    // State for editable fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var weightKg by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Load user profile data
    LaunchedEffect(uid) {
        if (uid != null) {
            try {
                val doc = db.collection("users").document(uid).get().await()
                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    userProfile = profile
                    firstName = profile.firstName
                    lastName = profile.lastName
                    age = profile.age.toString()
                    heightCm = profile.heightCm.toString()
                    weightKg = profile.weightKg.toString()
                    goal = profile.goal
                }
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Error loading user profile: ${e.message}")
                coroutineScope.launch { snackbarHostState.showSnackbar("Error loading profile") }
            } finally {
                isLoading = false
            }
        }
    }

    // Function to save profile updates
    fun saveProfile() {
        if (uid == null) return
        isLoading = true
        val updatedProfile = UserProfile(
            uid = uid,
            firstName = firstName,
            lastName = lastName,
            email = userProfile.email, // Keep email from loaded profile
            age = age.toIntOrNull() ?: 0,
            heightCm = heightCm.toIntOrNull() ?: 0,
            weightKg = weightKg.toIntOrNull() ?: 0,
            goal = goal
        )

        db.collection("users").document(uid).set(updatedProfile)
            .addOnSuccessListener {
                Log.d("ProfileScreen", "Profile updated successfully")
                coroutineScope.launch { snackbarHostState.showSnackbar("Profile updated!") }
                isLoading = false
            }
            .addOnFailureListener { e ->
                Log.e("ProfileScreen", "Error updating profile: ${e.message}")
                coroutineScope.launch { snackbarHostState.showSnackbar("Error updating profile") }
                isLoading = false
            }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "My Profile",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                if (isLoading) {
                    CircularProgressIndicator(color = Purple)
                } else {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "First Name Icon", tint = Purple) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "Last Name Icon", tint = Purple) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                         colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                    OutlinedTextField(
                        value = age,
                        onValueChange = { age = it },
                        label = { Text("Age") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Cake, contentDescription = "Age Icon", tint = Purple) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                    OutlinedTextField(
                        value = heightCm,
                        onValueChange = { heightCm = it },
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Height, contentDescription = "Height Icon", tint = Purple) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                    OutlinedTextField(
                        value = weightKg,
                        onValueChange = { weightKg = it },
                        label = { Text("Weight (kg)") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.FitnessCenter, contentDescription = "Weight Icon", tint = Purple) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                     OutlinedTextField(
                        value = goal,
                        onValueChange = { goal = it },
                        label = { Text("Fitness Goal") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = "Goal Icon", tint = Purple) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = DarkGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedContainerColor = DarkGray,
                            unfocusedContainerColor = DarkGray
                        ),
                        textStyle = LocalTextStyle.current.copy(color = White)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { saveProfile() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        Text("Save Profile", fontSize = 18.sp)
                    }
                }
            }
        }
    }
} 