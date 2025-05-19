package com.example.myfitness.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myfitness.R
import com.example.myfitness.ui.theme.DarkGray
import com.example.myfitness.ui.theme.LightGray
import com.example.myfitness.ui.theme.MyFitnessTheme
import com.example.myfitness.ui.theme.Purple
import com.example.myfitness.ui.theme.White
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(navController: NavController, auth: FirebaseAuth) {
    val context = LocalContext.current
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        LoginForm(
            email           = email,
            password        = password,
            isLoading       = isLoading,
            onEmailChange   = { email = it },
            onPasswordChange= { password = it },
            onLoginClick    = {
                isLoading = true
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, task.exception?.message ?: "Login failed", Toast.LENGTH_LONG).show()
                        }
                    }
            },
            onRegisterClick = { navController.navigate("register") }
        )
    }
}

@Composable
fun LoginForm(
    email: String,
    password: String,
    isLoading: Boolean,
    onEmailChange:    (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick:     () -> Unit,
    onRegisterClick:  () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        // 1) Icône de l’application (mipmap)
        Image(
            painter = painterResource(id = R.drawable.ic_app_photo),
            contentDescription = "App Photo",
            contentScale = ContentScale.Crop,    // adapter l’image sur l’espace
            modifier = Modifier
                .size(200.dp)                       // par exemple, plus grand qu’une icône
                .clip(RoundedCornerShape(12.dp))    // optional : coins arrondis
                .padding(bottom = 24.dp)
        )
        // 2) Titre "Login"
        Text(
            text  = "Login",
            style = MaterialTheme.typography.headlineMedium.copy(color = White)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3) Card du formulaire
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(18.dp),
            colors   = CardDefaults.cardColors(containerColor = DarkGray),
            elevation= CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                /** Email **/
                OutlinedTextField(
                    value             = email,
                    onValueChange     = onEmailChange,
                    label             = { Text("Email", color = LightGray) },
                    leadingIcon       = {
                        Icon(
                            imageVector     = Icons.Outlined.Email,
                            contentDescription = null,
                            tint            = Purple,
                            modifier        = Modifier.size(24.dp)
                        )
                    },
                    modifier          = Modifier.fillMaxWidth(),
                    singleLine        = true,
                    colors            = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor    = Purple,
                        unfocusedBorderColor  = LightGray,
                        focusedLabelColor     = Purple,
                        unfocusedLabelColor   = LightGray,
                        cursorColor           = Purple,
                        focusedTextColor      = White,
                        unfocusedTextColor    = White
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                /** Password **/
                OutlinedTextField(
                    value             = password,
                    onValueChange     = onPasswordChange,
                    label             = { Text("Password", color = LightGray) },
                    leadingIcon       = {
                        Icon(
                            imageVector     = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint            = Purple,
                            modifier        = Modifier.size(24.dp)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier            = Modifier.fillMaxWidth(),
                    singleLine          = true,
                    colors              = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Purple,
                        unfocusedBorderColor = LightGray,
                        focusedLabelColor    = Purple,
                        unfocusedLabelColor  = LightGray,
                        cursorColor          = Purple,
                        focusedTextColor     = White,
                        unfocusedTextColor   = White
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                /** Bouton Login **/
                Button(
                    onClick    = onLoginClick,
                    enabled    = !isLoading,
                    modifier   = Modifier.fillMaxWidth(),
                    colors     = ButtonDefaults.buttonColors(containerColor = Purple, contentColor = White),
                    shape      = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLoading) "Logging in..." else "Login")
                }

                Spacer(modifier = Modifier.height(16.dp))

                /** Lien vers l’inscription **/
                TextButton(onClick = onRegisterClick) {
                    Text("Don't have an account? Register", color = Purple)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginFormPreview() {
    MyFitnessTheme {
        LoginForm(
            email            = "user@example.com",
            password         = "password",
            isLoading        = false,
            onEmailChange    = {},
            onPasswordChange = {},
            onLoginClick     = {},
            onRegisterClick  = {}
        )
    }
}
