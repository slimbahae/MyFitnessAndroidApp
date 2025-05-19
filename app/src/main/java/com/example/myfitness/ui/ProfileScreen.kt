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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var userProfile by remember { mutableStateOf(UserProfile()) }
    var isLoading by remember { mutableStateOf(true) }

    // State for editable fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // Password change states
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Delete account states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletePassword by remember { mutableStateOf("") }

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
                    email = profile.email
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
            email = email,
            age = userProfile.age,
            heightCm = userProfile.heightCm,
            weightKg = userProfile.weightKg,
            goal = userProfile.goal
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

    // Function to change password
    fun changePassword() {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Please fill in all password fields") }
            return
        }

        if (newPassword != confirmPassword) {
            coroutineScope.launch { snackbarHostState.showSnackbar("New passwords don't match") }
            return
        }

        val user = auth.currentUser
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)

        user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        coroutineScope.launch { 
                            snackbarHostState.showSnackbar("Password updated successfully")
                            showPasswordDialog = false
                            currentPassword = ""
                            newPassword = ""
                            confirmPassword = ""
                        }
                    }
                    .addOnFailureListener { e ->
                        coroutineScope.launch { snackbarHostState.showSnackbar("Error updating password: ${e.message}") }
                    }
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Current password is incorrect") }
            }
        }
    }

    // Function to delete account
    fun deleteAccount() {
        if (deletePassword.isBlank()) {
            coroutineScope.launch { snackbarHostState.showSnackbar("Please enter your password") }
            return
        }

        val user = auth.currentUser
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, deletePassword)

        user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                // Delete user data from Firestore
                db.collection("users").document(uid!!).delete()
                    .addOnSuccessListener {
                        // Delete user account
                        user.delete()
                            .addOnSuccessListener {
                                coroutineScope.launch { 
                                    snackbarHostState.showSnackbar("Account deleted successfully")
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                coroutineScope.launch { snackbarHostState.showSnackbar("Error deleting account: ${e.message}") }
                            }
                    }
                    .addOnFailureListener { e ->
                        coroutineScope.launch { snackbarHostState.showSnackbar("Error deleting user data: ${e.message}") }
                    }
            } else {
                coroutineScope.launch { snackbarHostState.showSnackbar("Password is incorrect") }
            }
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
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Email Icon", tint = Purple) },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPasswordDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray, contentColor = White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Change Password", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { navController.navigate("completeProfile") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGray, contentColor = White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Update Workout Plan", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red, contentColor = White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Delete Account", fontSize = 18.sp)
                    }
                }
            }
        }
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password", color = White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
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
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
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
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
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
                }
            },
            confirmButton = {
                Button(
                    onClick = { changePassword() },
                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                ) {
                    Text("Change Password")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text("Cancel", color = Purple)
                }
            },
            containerColor = DarkGray
        )
    }

    // Delete Account Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", color = White) },
            text = {
                Column {
                    Text("Are you sure you want to delete your account? This action cannot be undone.", color = LightGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = deletePassword,
                        onValueChange = { deletePassword = it },
                        label = { Text("Enter Password to Confirm") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
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
                }
            },
            confirmButton = {
                Button(
                    onClick = { deleteAccount() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = Purple)
                }
            },
            containerColor = DarkGray
        )
    }
} 