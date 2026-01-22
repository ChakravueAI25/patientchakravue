package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.org.patientchakravue.model.DoctorNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDetailScreen(
    note: DoctorNote,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ORIGINAL IMAGE
            if (note.details?.imageId != null) {
                Card(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                    AsyncImage(
                        model = "https://patient.chakravue.co.in/images/${note.details.imageId}",
                        contentDescription = "Eye Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // 2. DOCTOR'S NOTE
            Text("Doctor's Advice", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A3B5D))
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))
            ) {
                Text(
                    text = note.noteText ?: "No instructions provided.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3. YOUR SYMPTOMS (Linked Data)
            Text("Submitted Symptoms", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1A3B5D))
            Spacer(Modifier.height(8.dp))

            val details = note.details
            if (details != null) {
                DetailRow("Pain", details.pain)
                DetailRow("Blurry Vision", details.vision)
                DetailRow("Redness", details.redness)
                DetailRow("Watering", details.watering)
                DetailRow("Itching", details.itching)
                DetailRow("Discharge", details.discharge)

                if (!details.comments.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Your Comments:", fontWeight = FontWeight.SemiBold)
                    Text(details.comments, color = Color.Gray)
                }
            } else {
                Text("No submission data linked to this note.", color = Color.Gray)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: Int) {
    val levelText = when (value) {
        0 -> "None"
        3 -> "Mild"
        6 -> "Moderate"
        10 -> "Severe"
        else -> "N/A"
    }
    val color = if (value >= 6) Color.Red else Color.Black

    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(levelText, fontWeight = FontWeight.Bold, color = color)
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
}

