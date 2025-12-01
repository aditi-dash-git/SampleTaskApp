package com.example.sampletaskapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult

import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import com.example.sampletaskapp.Task
import com.example.sampletaskapp.TaskType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                SampleTaskApp()
            }
        }
    }
}

sealed class Screen {
    object Start : Screen()
    object NoiseTest : Screen()
    object TaskSelection : Screen()
    object TextReading : Screen()
    object ImageDescription : Screen()
    object PhotoCapture : Screen()
    object TaskHistory : Screen()
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SampleTaskApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Start) }
    val tasks = remember { mutableStateListOf<Task>() }

    fun addTask(task: Task) {
        tasks.add(task.copy(id = tasks.size + 1L))
    }

    when (currentScreen) {
        Screen.Start -> StartScreen { currentScreen = Screen.NoiseTest }
        Screen.NoiseTest -> NoiseTestScreen { currentScreen = Screen.TaskSelection }
        Screen.TaskSelection -> TaskSelectionScreen(
            onTextReading = { currentScreen = Screen.TextReading },
            onImageDescription = { currentScreen = Screen.ImageDescription },
            onPhotoCapture = { currentScreen = Screen.PhotoCapture },
            onHistory = { currentScreen = Screen.TaskHistory }
        )
        Screen.TextReading -> TextReadingTaskScreen(
            onSubmit = { addTask(it); currentScreen = Screen.TaskSelection },
            onBack = { currentScreen = Screen.TaskSelection }
        )
        Screen.ImageDescription -> ImageDescriptionTaskScreen(
            onSubmit = { addTask(it); currentScreen = Screen.TaskSelection },
            onBack = { currentScreen = Screen.TaskSelection }
        )
        Screen.PhotoCapture -> PhotoCaptureTaskScreen(
            onSubmit = { addTask(it); currentScreen = Screen.TaskSelection },
            onBack = { currentScreen = Screen.TaskSelection }
        )
        Screen.TaskHistory -> TaskHistoryScreen(
            tasks = tasks,
            onBack = { currentScreen = Screen.TaskSelection }
        )
    }
}

fun isoTimestamp(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

// ------------ Start Screen ---------------

@Composable
fun StartScreen(onStartClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Let's start with a Sample Task for practice.", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStartClick) { Text("Start Sample Task") }
    }
}

// ------------ Noise Test Screen ----------

@Composable
fun NoiseTestScreen(onPass: () -> Unit) {
    var dB by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Noise Test", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("Current Noise: $dB dB")
        LinearProgressIndicator((dB / 60f).coerceIn(0f, 1f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            dB = Random.nextInt(25, 55)
            message = if (dB < 40) "Good to proceed!" else "Please move to a quieter place"
            if (dB < 40) onPass()
        }) { Text("Start Test") }
        Spacer(Modifier.height(12.dp))
        Text(message)
    }
}

// ------------ Task Selection Screen ------

@Composable
fun TaskSelectionScreen(
    onTextReading: () -> Unit,
    onImageDescription: () -> Unit,
    onPhotoCapture: () -> Unit,
    onHistory: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Select a Task", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onTextReading, modifier = Modifier.fillMaxWidth()) { Text("Text Reading") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onImageDescription, modifier = Modifier.fillMaxWidth()) { Text("Image Description") }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onPhotoCapture, modifier = Modifier.fillMaxWidth()) { Text("Photo Capture") }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Task History") }
    }
}

// ------------ Hold-to-Record Button ------

@Composable
fun HoldToRecordButton(onValidRecording: (Int, String) -> Unit) {
    var isRecording by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(0L) }
    var errorMessage by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(80.dp)
                .clip(CircleShape)
                .background(if (isRecording) Color.Red else Color.DarkGray)
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        isRecording = true
                        startTime = System.currentTimeMillis()
                        tryAwaitRelease()
                        isRecording = false
                        val duration = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                        when {
                            duration < 10 -> errorMessage = "Recording too short (min 10s)"
                            duration > 20 -> errorMessage = "Recording too long (max 20s)"
                            else -> onValidRecording(duration, "/local/audio_${System.currentTimeMillis()}.mp3")
                        }
                    })
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Text(errorMessage, color = Color.Red, fontSize = 12.sp)
    }
}

// ------------ Text Reading Task ----------

@Composable
fun TextReadingTaskScreen(onSubmit: (Task) -> Unit, onBack: () -> Unit) {
    val desc = "Read this passage aloud in your native language."
    var duration by remember { mutableStateOf<Int?>(null) }
    var audio by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row {
            Text("Text Reading", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(12.dp))
        Text(desc)

        Spacer(Modifier.height(16.dp))
        HoldToRecordButton { d, p -> duration = d; audio = p }

        duration?.let {
            Spacer(Modifier.height(8.dp))
            Text("Recorded: ${it}s")
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = duration != null && audio != null,
            onClick = {
                onSubmit(
                    Task(
                        id = 0, taskType = TaskType.TEXT_READING,
                        text = desc, audioPath = audio, durationSec = duration!!,
                        timestamp = isoTimestamp()
                    )
                )
            }
        ) { Text("Submit") }
    }
}

// ------------ Image Description -----------

@Composable
fun ImageDescriptionTaskScreen(onSubmit: (Task) -> Unit, onBack: () -> Unit) {
    val img = "https://cdn.dummyjson.com/product-images/14/2.jpg"
    var duration by remember { mutableStateOf<Int?>(null) }
    var audio by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row {
            Text("Image Description", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }
        Spacer(Modifier.height(12.dp))
        AsyncImage(img, contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp))

        Spacer(Modifier.height(16.dp))
        HoldToRecordButton { d, p -> duration = d; audio = p }

        duration?.let { Text("Recorded: ${it}s") }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = duration != null && audio != null,
            onClick = {
                onSubmit(
                    Task(
                        id = 0, taskType = TaskType.IMAGE_DESCRIPTION,
                        imageUrl = img, audioPath = audio,
                        durationSec = duration!!, timestamp = isoTimestamp()
                    )
                )
            }
        ) { Text("Submit") }
    }
}

// ------------ Photo Capture Task ----------

@Composable
fun PhotoCaptureTaskScreen(onSubmit: (Task) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var path by remember { mutableStateOf<String?>(null) }
    var text by remember { mutableStateOf("") }

    val camLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            if (it != null) {
                bmp = it
                val file = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out -> it.compress(Bitmap.CompressFormat.JPEG, 90, out) }
                path = file.absolutePath
            }
        }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row {
            Text("Photo Capture", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("Back") }
        }

        Spacer(Modifier.height(12.dp))

        if (bmp != null)
            Image(
                rememberAsyncImagePainter(bmp),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        else
            Box(
                Modifier.fillMaxWidth().height(200.dp).border(1.dp, Color.Gray),
                contentAlignment = Alignment.Center
            ) { Text("No Image Captured") }

        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val activity = context as? ComponentActivity
            if (activity?.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                camLauncher.launch(null)
            } else {
                activity?.requestPermissions(
                    arrayOf(Manifest.permission.CAMERA), 100
                )
            }
        }) {
            Text("Capture Image")
        }


        Spacer(Modifier.height(16.dp))
        OutlinedTextField(text, onValueChange = { text = it }, label = { Text("Describe the photo") })

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = path != null && text.isNotEmpty(),
            onClick = {
                onSubmit(
                    Task(
                        id = 0,
                        taskType = TaskType.PHOTO_CAPTURE,
                        text = text,
                        imagePath = path,
                        durationSec = 0,
                        timestamp = isoTimestamp()
                    )
                )
            }
        ) { Text("Submit") }
    }
}

// ------------ Task History Screen ----------

@Composable
fun TaskHistoryScreen(tasks: List<Task>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Tasks: ${tasks.size}")
            Text("Total Duration: ${tasks.sumOf { it.durationSec }}s")
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.weight(1f)) {
            items(tasks) { t ->
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Task ID: ${t.id}")
                    Text("Type: ${t.taskType}")
                    Text("Duration: ${t.durationSec}s")
                    Text("Timestamp: ${t.timestamp}")
                }
                Divider()
            }
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
