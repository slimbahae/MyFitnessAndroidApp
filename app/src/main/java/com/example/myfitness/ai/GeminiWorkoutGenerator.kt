package com.example.myfitness.ai

import android.content.Context
import android.util.Log
import com.example.myfitness.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiWorkoutGenerator {

    private var exerciseList: List<Exercise>? = null

    @Serializable
    data class Exercise(
        val id: String,
        val name: String,
        val bodyPart: String,
        val target: String,
        val equipment: String,
        val gifUrl: String,
        val secondaryMuscles: List<String>,
        val instructions: List<String>
    )

    private fun loadExercises(context: Context): List<Exercise> {
        if (exerciseList != null) return exerciseList!!
        
        return try {
            val jsonString = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val exercises = json.decodeFromString<List<Exercise>>(jsonString)
            exerciseList = exercises
            exercises
        } catch (e: Exception) {
            Log.e(TAG, "Error loading exercises: ${e.message}", e)
            emptyList()
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private const val TAG = "GeminiWorkoutGen"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    suspend fun generateWorkoutPlan(user: Map<String, Any>, context: Context): JsonElement? = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting workout generation for user: $user")

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            Log.e(TAG, "API key is blank!")
            return@withContext null
        }

        Log.d(TAG, "API key found, length: ${apiKey.length}")

        try {
            val prompt = buildPrompt(user, context)
            Log.d(TAG, "Built prompt: $prompt")

            val jsonRequest = buildGeminiRequest(prompt)
            Log.d(TAG, "Built request body")

            val request = Request.Builder()
                .url("$GEMINI_URL?key=$apiKey")
                .post(jsonRequest)
                .header("Content-Type", "application/json")
                .build()

            Log.d(TAG, "Making API request to Gemini...")

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string()

                Log.d(TAG, "Response received - Status: ${response.code}")
                Log.d(TAG, "Response body length: ${rawBody?.length ?: 0}")

                if (!response.isSuccessful) {
                    Log.e(TAG, "API request failed: ${response.code} - ${response.message}")
                    Log.e(TAG, "Response body: $rawBody")
                    return@withContext null
                }

                if (rawBody == null) {
                    Log.e(TAG, "Response body is null")
                    return@withContext null
                }

                // Log first 500 characters of response for debugging
                Log.d(TAG, "Raw response preview: ${rawBody.take(500)}")

                return@withContext parseResponse(rawBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call: ${e.message}", e)
            return@withContext null
        }
    }

    private fun parseResponse(rawBody: String): JsonElement? {
        return try {
            val root = json.parseToJsonElement(rawBody)
            Log.d(TAG, "Parsed root JSON successfully")

            val candidates = root.jsonObject["candidates"]?.jsonArray
            if (candidates == null || candidates.isEmpty()) {
                Log.e(TAG, "No candidates found in response")
                return null
            }

            val firstCandidate = candidates.first().jsonObject
            val content = firstCandidate["content"]?.jsonObject
            val parts = content?.get("parts")?.jsonArray
            val text = parts?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content

            if (text == null) {
                Log.e(TAG, "No text content found in response")
                Log.d(TAG, "First candidate structure: $firstCandidate")
                return null
            }

            Log.d(TAG, "Extracted text from response: ${text.take(200)}...")

            // Clean up the JSON text
            val cleanedJson = text
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            Log.d(TAG, "Cleaned JSON preview: ${cleanedJson.take(200)}...")

            // Try to parse the cleaned JSON
            val workoutPlan = json.parseToJsonElement(cleanedJson)
            Log.d(TAG, "Successfully parsed workout plan JSON")

            return workoutPlan
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            Log.e(TAG, "Raw body that failed to parse: $rawBody")
            return null
        }
    }

    private fun buildPrompt(user: Map<String, Any>, context: Context): String {
        val exercises = loadExercises(context)
        val exercisesJson = Json { prettyPrint = true }.encodeToString(
            ListSerializer(Exercise.serializer()),
            exercises
        )

        return """
            You are a professional fitness coach. Your task is to create a JSON workout plan for a user with the following information:
            Personal Details:
                    - Age: ${user["age"]} years
                    - Gender: ${user["gender"]}
                    - Height: ${user["heightCm"]} cm  
                    - Weight: ${user["weightKg"]} kg
                    - Fitness Goal: ${user["goal"]}

            Please create a weekly workout plan (7 days) in the following JSON format.
            Include rest days and make sure exercises are appropriate for the user's goal.

            Return ONLY valid JSON in this exact format (no additional text):

            [
              {
                "day": "Monday",
                "type": "Strength Training",
                "muscleGroup": "Upper Body",
                "exerciseIds": ["0007", "0015"],
                "duration": 45,
                "calories": 300,
                "notes": "Focus on proper form"
              },
              {
                "day": "Tuesday",
                "type": "Cardio",
                "muscleGroup": "Full Body",
                "exerciseIds": ["0020"],
                "duration": 30,
                "calories": 250,
                "notes": "Keep hydrated"
              },
              {
                "day": "Wednesday",
                "type": "Rest",
                "muscleGroup": "Recovery",
                "exerciseIds": [],
                "duration": 0,
                "calories": 0,
                "notes": "Active recovery day"
              }
            ]

            Important: For each day, only return a list of exercise IDs (from the list below) in the "exerciseIds" field. Do not include exercise names, sets, reps, or instructions in the plan. Use only IDs from this list:
            $exercisesJson
            Do not return any markdown formatting or additional text, only the JSON array.
        """.trimIndent()
    }

    private fun buildGeminiRequest(prompt: String): RequestBody {
        val body = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    putJsonArray("parts") {
                        addJsonObject {
                            put("text", prompt)
                        }
                    }
                }
            }
            // Add generation config for more consistent responses
            putJsonObject("generationConfig") {
                put("temperature", 0.3) // Lower temperature for more consistent JSON
                put("topK", 1)
                put("topP", 0.8)
                put("maxOutputTokens", 2048)
            }
        }

        val bodyString = json.encodeToString(JsonElement.serializer(), body)
        Log.d(TAG, "Request body: $bodyString")

        return bodyString.toRequestBody("application/json".toMediaType())
    }
}