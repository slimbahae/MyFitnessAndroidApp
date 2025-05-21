import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import com.example.myfitness.ai.GeminiWorkoutGenerator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GeminiTestScreen(context: android.content.Context) {
    var responseText by remember { mutableStateOf<String>("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = {
            scope.launch {
                // Example user data, replace with real user info as needed
                val user = mapOf(
                    "age" to 25,
                    "heightCm" to 175,
                    "weightKg" to 70,
                    "goal" to "Build muscle"
                )
                val result = GeminiWorkoutGenerator.generateWorkoutPlan(user, context)
                responseText = result?.toString() ?: "No response or error"
            }
        }) {
            Text("Generate Gemini Workout")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = responseText,
            color = Color.White,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
