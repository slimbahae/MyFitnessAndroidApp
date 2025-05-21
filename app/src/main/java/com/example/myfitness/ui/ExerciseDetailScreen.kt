package com.example.myfitness.ui

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.myfitness.model.Exercise
import com.example.myfitness.ui.theme.Black
import com.example.myfitness.ui.theme.DarkGray
import com.example.myfitness.ui.theme.LightGray
import com.example.myfitness.ui.theme.Purple
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.myfitness.model.ExerciseApiResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import com.example.myfitness.BuildConfig

val okHttpClient = OkHttpClient()

suspend fun fetchExerciseGifUrlOkHttp(exerciseId: String): String? {
    val url = "https://exercisedb.p.rapidapi.com/exercises/exercise/$exerciseId"
    val request = Request.Builder()
        .url(url)
        .addHeader("x-rapidapi-key", "88a24ec063mshe1790fe8edfe267p13ec9ejsn623f2dc53648")
        .addHeader("x-rapidapi-host", "exercisedb.p.rapidapi.com")
        .build()
    return try {
        val response = okHttpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string()
            body?.let {
                val json = Json { ignoreUnknownKeys = true }
                val apiResponse: ExerciseApiResponse = json.decodeFromString(it)
                apiResponse.gifUrl
            }
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    exerciseDatabase: List<Exercise>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val exerciseInfo = exerciseDatabase.find { it.id == exerciseId }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var gifUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(exerciseId) {
        gifUrl = fetchExerciseGifUrlOkHttp(exerciseId)
    }
    LaunchedEffect(Unit) {
        Log.d("ExerciseDetailScreen", "exerciseId: $exerciseId")
        Log.d("ExerciseDetailScreen", "exerciseDatabase: ${exerciseDatabase.map { it.id }}")
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(exerciseInfo?.name ?: "", color = Color.White, fontSize = 26.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Purple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkGray)
            )
        },
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (exerciseInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Exercise not found in database.",
                    color = Color.Red,
                    fontSize = 20.sp
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Exercise GIF
            gifUrl?.let { url ->
                val imageLoader = remember {
                    ImageLoader.Builder(context)
                        .components {
                            if (Build.VERSION.SDK_INT >= 28) {
                                add(ImageDecoderDecoder.Factory())
                            } else {
                                add(GifDecoder.Factory())
                            }
                        }
                        .crossfade(true)
                        .build()
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Black),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(32.dp))
                            .clickable {
                                scope.launch {
                                    snackbarHostState.showSnackbar("GIF Clicked!", duration = SnackbarDuration.Short)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage1(
                            model = url,
                            contentDescription = "Exercise demonstration",
                            imageLoader = imageLoader,
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF18181C)),
                            onLoading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = Purple,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            },
                            onError = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FitnessCenter,
                                        contentDescription = "Exercise icon",
                                        tint = Purple,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Exercise Details
            exerciseInfo?.let { info ->
                // Body Part
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Body Part", tint = Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Body Part: ${info.bodyPart}", color = LightGray, fontSize = 20.sp)
                    }
                }
                // Equipment
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        Icon(Icons.Filled.Build, contentDescription = "Equipment", tint = Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Equipment: ${info.equipment}", color = LightGray, fontSize = 20.sp)
                    }
                }
                // Target Muscle
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp)) {
                        Icon(Icons.Filled.Whatshot, contentDescription = "Target", tint = Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Target: ${info.target}", color = LightGray, fontSize = 20.sp)
                    }
                }
                // Secondary Muscles
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(18.dp)) {
                        Icon(Icons.Filled.FitnessCenter, contentDescription = "Secondary Muscles", tint = Purple, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Secondary Muscles: ${info.secondaryMuscles.joinToString(", ")}",
                            color = LightGray,
                            fontSize = 20.sp
                        )
                    }
                }
                // Instructions
                Text(
                    "Instructions:",
                    color = LightGray,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 28.dp, top = 18.dp, bottom = 8.dp)
                )
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    info.instructions.forEachIndexed { index, instruction ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF23232A)),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "${index + 1}.",
                                    color = Purple,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    instruction,
                                    color = LightGray,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 