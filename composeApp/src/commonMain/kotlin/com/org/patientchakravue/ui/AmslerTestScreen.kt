package com.org.patientchakravue.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.platform.captureAmslerBitmap
import com.org.patientchakravue.ui.language.LocalLanguageManager
import com.org.patientchakravue.ui.language.localizedString
import kotlinx.coroutines.launch

// Steps for the Test Flow
enum class AmslerStep { INSTRUCTIONS, DRAWING }

@Composable
fun AmslerTestScreen(
    patient: Patient,
    onBack: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    var currentStep by remember { mutableStateOf(AmslerStep.INSTRUCTIONS) }
    var selectedEye by remember { mutableStateOf("Left") }

    // Get current language to trigger recomposition when it changes
    val currentLang = LocalLanguageManager.current.currentLanguage

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentStep) {
            AmslerStep.INSTRUCTIONS -> AmslerInstructions(
                onNext = { eye ->
                    selectedEye = eye
                    currentStep = AmslerStep.DRAWING
                }
            )
            AmslerStep.DRAWING -> AmslerCanvasDrawing(
                patient = patient,
                eyeSide = selectedEye,
                onSuccess = {
                    showSnackbar("Test submitted successfully!")
                    onBack()
                },
                onError = { showSnackbar("Failed to submit test.") }
            )
        }
    }
}

// --- STEP 1: INSTRUCTIONS ---
@Composable
fun AmslerInstructions(onNext: (String) -> Unit) {
    var selectedEye by remember { mutableStateOf("Right") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text(localizedString("instructions_title"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
        Text("Follow the steps below to start the test", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))

        // Static Grid Preview
        Box(
            modifier = Modifier
                .size(220.dp)
                .background(Color.White)
                .padding(4.dp)
        ) {
            AmslerGridVisual()
        }

        Spacer(Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                InstructionItem(localizedString("amsler_inst_1"))
                InstructionItem(localizedString("amsler_inst_2"))
                InstructionItem(localizedString("amsler_inst_3"))
                InstructionItem(localizedString("amsler_inst_4"))
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(localizedString("select_eye"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A3B5D))
        Row(Modifier.padding(vertical = 8.dp)) {
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

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { onNext(selectedEye) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
        ) {
            Text(localizedString("start_test_btn"))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun InstructionItem(text: String) {
    Text(text, fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
}

// --- STEP 2: DRAWING CANVAS ---
@Composable
fun AmslerCanvasDrawing(
    patient: Patient,
    eyeSide: String,
    onSuccess: () -> Unit,
    onError: () -> Unit
) {
    val api = remember { ApiRepository() }
    val scope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }

    // Store drawing paths
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Text("${localizedString("mark_distortions")} ($eyeSide)", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
        Text(localizedString("draw_hint"), color = Color.Gray, fontSize = 14.sp)

        Spacer(Modifier.height(16.dp))

        // THE CANVAS
        Box(
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .background(Color.White)
                .align(Alignment.CenterHorizontally)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val newPath = Path().apply { moveTo(it.x, it.y) }
                            currentPath = newPath
                        },
                        onDragEnd = {
                            currentPath?.let { paths.add(it) }
                            currentPath = null
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentPath?.relativeLineTo(dragAmount.x, dragAmount.y)
                        }
                    )
                }
        ) {
            // 1. Draw the Base Grid
            AmslerGridVisual()

            // 2. Draw User Paths
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw completed paths
                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Red.copy(alpha = 0.6f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
                // Draw current path being dragged
                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = Color.Red.copy(alpha = 0.6f),
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // CONTROLS
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedButton(onClick = { paths.clear() }) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(4.dp))
                Text(localizedString("clear_btn"))
            }

            Button(
                onClick = {
                    isSubmitting = true
                    // Capture Bitmap logic
                    val bitmapBytes = captureAmslerBitmap(paths.toList(), 800)

                    scope.launch {
                        val success = api.submitAmslerTest(
                            imageBytes = bitmapBytes,
                            patientId = patient.id,
                            eyeSide = eyeSide,
                            notes = "User drawing submission"
                        )
                        isSubmitting = false
                        if (success) onSuccess() else onError()
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(4.dp))
                    Text(localizedString("submit_btn"))
                }
            }
        }
    }
}

// --- HELPER: Visual Grid ---
@Composable
fun AmslerGridVisual() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = width / 20

        // Draw Grid Lines
        for (i in 0..20) {
            val pos = step * i
            // Vertical
            drawLine(
                color = Color.Black,
                start = Offset(pos, 0f),
                end = Offset(pos, height),
                strokeWidth = 1.dp.toPx()
            )
            // Horizontal
            drawLine(
                color = Color.Black,
                start = Offset(0f, pos),
                end = Offset(width, pos),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Center Dot
        drawCircle(
            color = Color.Black,
            radius = 6.dp.toPx(),
            center = center
        )
    }
}
