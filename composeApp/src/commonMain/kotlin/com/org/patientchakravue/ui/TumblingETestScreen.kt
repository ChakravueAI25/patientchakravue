package com.org.patientchakravue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.LevelResult
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.ui.language.LocalLanguageManager
import com.org.patientchakravue.ui.language.localizedString
import kotlinx.coroutines.launch
import kotlin.math.abs

// 1. Data Class for Fixed Medical Levels
data class AcuityLevel(
    val name: String,   // e.g., "6/60"
    val sizeDp: Dp,     // Fixed size based on 3m distance
    val logMar: Double
)

// 2. Define the Standard Levels (Largest to Smallest)
val MEDICAL_LEVELS = listOf(
    AcuityLevel("6/60", 200.dp, 1.0),
    AcuityLevel("6/36", 120.dp, 0.78),
    AcuityLevel("6/24", 80.dp, 0.6),
    AcuityLevel("6/18", 60.dp, 0.48),
    AcuityLevel("6/12", 40.dp, 0.3),
    AcuityLevel("6/9", 30.dp, 0.18),
    AcuityLevel("6/6", 20.dp, 0.0)
)

enum class E_Direction { UP, DOWN, LEFT, RIGHT }
enum class TestState { INSTRUCTIONS, TESTING, COMPLETED }

@Composable
fun TumblingETestScreen(
    patient: Patient,
    onBack: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    var gameState by remember { mutableStateOf(TestState.INSTRUCTIONS) }

    // Get current language to trigger recomposition when it changes
    val currentLang = LocalLanguageManager.current.currentLanguage

    // Pre-capture localized strings for use in non-composable lambda contexts
    val resultsSavedMsg = localizedString("results_saved")
    val submissionFailedMsg = localizedString("submission_failed")

    // --- MEDICAL LOGIC STATE ---
    var currentLevelIndex by remember { mutableIntStateOf(0) } // Index in MEDICAL_LEVELS
    var trialsInCurrentLevel by remember { mutableIntStateOf(0) } // 0 to 5
    var correctInCurrentLevel by remember { mutableIntStateOf(0) }
    var currentDirection by remember { mutableStateOf(E_Direction.RIGHT) }

    // Results Storage
    var finalResultString by remember { mutableStateOf("") }
    val levelResults = remember { mutableStateListOf<LevelResult>() }
    var selectedEye by remember { mutableStateOf("Right") } // Default eye

    val api = remember { ApiRepository() }
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (gameState) {
            TestState.INSTRUCTIONS -> {
                TumblingEInstructionScreen(
                    onStart = { eye ->
                        // RESET LOGIC
                        selectedEye = eye
                        currentLevelIndex = 0
                        trialsInCurrentLevel = 0
                        correctInCurrentLevel = 0
                        levelResults.clear()
                        currentDirection = E_Direction.entries.toTypedArray().random()
                        gameState = TestState.TESTING
                    }
                )
            }
                TestState.TESTING -> {
                    // Get current level data
                    val currentLevelData = MEDICAL_LEVELS.getOrElse(currentLevelIndex) { MEDICAL_LEVELS.last() }

                    TumblingEGameScreen(
                        direction = currentDirection,
                        sizeDp = currentLevelData.sizeDp,
                        headerText = "Acuity: ${currentLevelData.name}",
                        subText = "Trial ${trialsInCurrentLevel + 1} / 5",
                        progress = (trialsInCurrentLevel / 5f),
                        onInput = { inputDirection ->
                            // --- CLINICAL LOGIC START ---

                            // 1. Record Result of this Trial
                            trialsInCurrentLevel++
                            if (inputDirection == currentDirection) {
                                correctInCurrentLevel++
                            }

                            // 2. Check if Level is Finished (5 Trials)
                            if (trialsInCurrentLevel >= 5) {
                                val passed = correctInCurrentLevel >= 3 // Threshold: 3/5

                                // Record Level stats
                                levelResults.add(
                                    LevelResult(
                                        levelName = currentLevelData.name,
                                        correct = correctInCurrentLevel,
                                        total = 5,
                                        passed = passed
                                    )
                                )

                                if (passed) {
                                    // PASS: Move to next level (smaller)
                                    if (currentLevelIndex < MEDICAL_LEVELS.lastIndex) {
                                        currentLevelIndex++
                                        trialsInCurrentLevel = 0
                                        correctInCurrentLevel = 0
                                        currentDirection = E_Direction.entries.toTypedArray().random()
                                    } else {
                                        // Completed all levels (Perfect Vision)
                                        finalResultString = "6/6 (Normal)"
                                        gameState = TestState.COMPLETED
                                    }
                                } else {
                                    // FAIL: Stop immediately.
                                    // Result is the PREVIOUS level passed.
                                    val previousIdx = currentLevelIndex - 1
                                    finalResultString = if (previousIdx >= 0) {
                                        MEDICAL_LEVELS[previousIdx].name
                                    } else {
                                        "< 6/60 (Low Vision)"
                                    }
                                    gameState = TestState.COMPLETED
                                }
                            } else {
                                // Continue trials in same level
                                currentDirection = E_Direction.entries.toTypedArray().random()
                            }
                            // --- CLINICAL LOGIC END ---
                        }
                    )
                }
                TestState.COMPLETED -> {
                    TumblingEResultScreen(
                        finalResult = finalResultString,
                        eye = selectedEye,
                        isSubmitting = isSubmitting,
                        onRetry = { gameState = TestState.INSTRUCTIONS },
                        onSubmit = {
                            isSubmitting = true
                            scope.launch {
                                val success = api.submitVisionTest(
                                    patientId = patient.id,
                                    patientName = patient.name ?: "Unknown",
                                    eyeSide = selectedEye,
                                    finalAcuity = finalResultString,
                                    details = levelResults.toList()
                                )
                                isSubmitting = false
                                if (success) {
                                    showSnackbar(resultsSavedMsg)
                                    onBack()
                                } else {
                                    showSnackbar(submissionFailedMsg)
                                }
                            }
                        }
                    )
                }
            }
        }
}

// --- UI COMPONENTS ---

@Composable
fun TumblingEInstructionScreen(onStart: (String) -> Unit) {
    var selectedEye by remember { mutableStateOf("Right") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Info, null, modifier = Modifier.size(64.dp), tint = Color(0xFF4CAF50))
        Spacer(Modifier.height(24.dp))
        Text(localizedString("visual_acuity_test"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
        Text("Follow the instructions to test your vision", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(localizedString("tumbling_inst_1"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(localizedString("tumbling_inst_2"), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(localizedString("tumbling_inst_3"), fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(localizedString("tumbling_inst_4"), fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // Eye Selector
        Text(localizedString("select_eye"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A3B5D))
        Spacer(Modifier.height(8.dp))
        Row {
            FilterChip(
                selected = selectedEye == "Right",
                onClick = { selectedEye = "Right" },
                label = { Text(localizedString("eye_right")) },
                modifier = Modifier.padding(end = 8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = selectedEye == "Left",
                onClick = { selectedEye = "Left" },
                label = { Text(localizedString("eye_left")) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF4CAF50),
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onStart(selectedEye) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(localizedString("start_test_btn"), fontSize = 18.sp)
        }
    }
}

@Composable
fun TumblingEGameScreen(
    direction: E_Direction,
    sizeDp: Dp,
    headerText: String,
    subText: String,
    progress: Float,
    onInput: (E_Direction) -> Unit
) {
    // Gesture state
    var dragDirection by remember { mutableStateOf<E_Direction?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        dragDirection?.let { onInput(it) }
                        dragDirection = null
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (x, y) = dragAmount
                        if (abs(x) > abs(y)) {
                            dragDirection = if (x > 0) E_Direction.RIGHT else E_Direction.LEFT
                        } else {
                            dragDirection = if (y > 0) E_Direction.DOWN else E_Direction.UP
                        }
                    }
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(0xFF4CAF50)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(headerText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
            Text(subText, fontSize = 14.sp, color = Color.Gray)
        }

        Spacer(Modifier.weight(1f))

        // DRAW THE E (Standard Optotype)
        Canvas(modifier = Modifier.size(sizeDp)) {
            val rotation = when (direction) {
                E_Direction.RIGHT -> 0f
                E_Direction.DOWN -> 90f
                E_Direction.LEFT -> 180f
                E_Direction.UP -> 270f
            }

            rotate(rotation) {
                val w = size.width
                val h = size.height
                val barThick = h / 5

                drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(barThick, h)) // Vertical
                drawRect(Color.Black, topLeft = Offset(0f, 0f), size = Size(w, barThick)) // Top
                drawRect(Color.Black, topLeft = Offset(0f, h / 2 - barThick / 2), size = Size(w, barThick)) // Mid
                drawRect(Color.Black, topLeft = Offset(0f, h - barThick), size = Size(w, barThick)) // Bot
            }
        }

        Spacer(Modifier.weight(1f))

        Text(localizedString("swipe_hint"), color = Color.LightGray)
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun TumblingEResultScreen(
    finalResult: String,
    eye: String,
    isSubmitting: Boolean,
    onRetry: () -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp), tint = Color(0xFF4CAF50))
        Spacer(Modifier.height(24.dp))
        Text(localizedString("test_complete"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
        Text("View your results below", color = Color.Gray, fontSize = 14.sp)

        Card(
            modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${localizedString("tested_eye")} $eye", fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(localizedString("visual_acuity_label"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(finalResult, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSubmit,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(localizedString("save_record_btn"), fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onRetry, enabled = !isSubmitting) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text(localizedString("retake_btn"))
        }
    }
}
