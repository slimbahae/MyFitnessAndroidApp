package com.example.myfitness.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myfitness.ui.theme.Purple
import com.example.myfitness.ui.theme.DarkGray
import com.example.myfitness.ui.theme.White
import com.example.myfitness.ui.theme.LightGray
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person

@Composable
fun RegisterScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

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
            Text(text = "Register", style = MaterialTheme.typography.headlineMedium.copy(color = White))

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkGray),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = LightGray) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Email, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = LightGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = LightGray) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = LightGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = LightGray) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Purple, modifier = Modifier.size(24.dp))
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = LightGray,
                            focusedLabelColor = Purple,
                            unfocusedLabelColor = LightGray,
                            cursorColor = Purple,
                            focusedTextColor = White,
                            unfocusedTextColor = White
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (password != confirmPassword) {
                                Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true

                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                        navController.navigate("completeProfile")
                                    } else {
                                        Toast.makeText(context, task.exception?.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                                    }
                                }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isLoading) "Registering..." else "Register")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { navController.navigate("login") }) {
                        Text("Already have an account? Login", color = Purple)
                    }
                }
            }
        }
    }
}
