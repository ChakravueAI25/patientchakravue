package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.Patient
// Peekaboo Image Picker imports
import com.preat.peekaboo.image.picker.SelectionMode
import com.preat.peekaboo.image.picker.rememberImagePickerLauncher
import kotlinx.coroutines.launch

@Composable
fun AfterCareScreen(
    patient: Patient,
    onBack: () -> Unit,
    showSnackbar: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiRepository() }

    // State for Symptom Values (Mapped to 0, 3, 6, 10)
    var blurredVision by remember { mutableIntStateOf(0) }
    var pain by remember { mutableIntStateOf(0) }
    var redness by remember { mutableIntStateOf(0) }
    var watering by remember { mutableIntStateOf(0) }
    var itching by remember { mutableIntStateOf(0) }
    var discharge by remember { mutableIntStateOf(0) }

    var comments by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Image Picker with explicit type handling to fix inference errors
    val picker = rememberImagePickerLauncher(
        selectionMode = SelectionMode.Single,
        scope = scope,
        onResult = { bytesList: List<ByteArray> ->
            imageBytes = bytesList.firstOrNull()
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Text("Daily Eye Check-In", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
        Text("Tell us how your eye feels today", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))

        // 1. Blurred Vision (Moved to top as requested)
        SymptomRow("Blurred Vision", Icons.Outlined.Visibility, blurredVision) { blurredVision = it }

        // 2. Original Symptoms
        SymptomRow("Pain", Icons.Filled.Favorite, pain) { pain = it }
        SymptomRow("Redness", Icons.Filled.RemoveRedEye, redness) { redness = it }
        SymptomRow("Watering", Icons.Filled.WaterDrop, watering) { watering = it }
        SymptomRow("Itching/Irritation", Icons.Filled.PanToolAlt, itching) { itching = it }

        // 3. Discharge (Added to the list)
        SymptomRow("Discharge", Icons.Filled.Opacity, discharge) { discharge = it }

        Spacer(Modifier.height(16.dp))

        // Image Upload Section
        OutlinedButton(
            onClick = { picker.launch() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.PhotoCamera, null)
            Spacer(Modifier.width(8.dp))
            Text(if (imageBytes == null) "Add Eye Photo (Recommended)" else "Photo Added ✓")
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = comments,
            onValueChange = { comments = it },
            label = { Text("Describe anything unusual...") },
            modifier = Modifier.fillMaxWidth().height(120.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (imageBytes == null) {
                    showSnackbar("Please add a photo of your eye")
                    return@Button
                }
                isSubmitting = true
                scope.launch {
                    val fields = mapOf(
                        "patient_id" to patient.id,
                        "patient_name" to (patient.name ?: "Unknown"),
                        "doctor_id" to (patient.doctorId ?: "68f0a9a6038e7bdb2b37d58f"),
                        "pain_scale" to pain.toString(),
                        "vision_blur" to blurredVision.toString(),
                        "redness" to redness.toString(),
                        "watering" to watering.toString(),
                        "itching" to itching.toString(),
                        "discharge" to discharge.toString(),
                        "comments" to comments
                    )
                    val success = api.submitReport(imageBytes!!, fields)
                    if (success) {
                        showSnackbar("Report submitted successfully!")
                        onBack()
                    } else {
                        showSnackbar("Failed to submit. Check your internet connection.")
                    }
                    isSubmitting = false
                }
            },
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDAA520))
        ) {
            if (isSubmitting) CircularProgressIndicator(color = Color.White)
            else Text("SUBMIT", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        // Extra space at the bottom to ensure submit button is visible above bottom nav
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SymptomRow(label: String, icon: ImageVector, selectedValue: Int, onValueChange: (Int) -> Unit) {
    // Mapping: None=0, Mild=3, Moderate=6, Severe=10
    val levels = listOf("None" to 0, "Mild" to 3, "Moderate" to 6, "Severe" to 10)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            levels.forEach { (name, value) ->
                FilterChip(
                    selected = selectedValue == value,
                    onClick = { onValueChange(value) },
                    label = { Text(name, fontSize = 11.sp) }
                )
            }
        }
        // FIXED: Using HorizontalDivider instead of deprecated Divider
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 0.5.dp)
    }
}