package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.model.VisionTestRecord

@Composable
fun VisionScreen(
    patient: Patient,
    onNavigateToAmsler: () -> Unit,
    onNavigateToTumblingE: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val api = remember { ApiRepository() }
    var history by remember { mutableStateOf<List<VisionTestRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch History on Load
    LaunchedEffect(Unit) {
        history = api.getVisionHistory(patient.id)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Text(
            "Vision Tests",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A3B5D)
        )
        Text(
            "Select a test to begin",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))

        // --- 1. HORIZONTAL BUTTONS ROW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amsler Grid Button
            VisionTestCard(
                title = "Amsler\nGrid",
                icon = Icons.Default.GridOn,
                color = Color(0xFFE3F2FD), // Light Blue
                iconColor = Color(0xFF1976D2),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToAmsler
            )

            // Tumbling E Button
            VisionTestCard(
                title = "Tumbling E\nTest",
                icon = Icons.Default.Visibility,
                color = Color(0xFFF3E5F5), // Light Purple
                iconColor = Color(0xFF7B1FA2),
                modifier = Modifier.weight(1f),
                onClick = onNavigateToTumblingE
            )
        }

        Spacer(Modifier.height(32.dp))

        // --- 2. HISTORY SECTION ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Recent Results",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A3B5D)
            )
        }
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No test history found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { record ->
                    HistoryItemCard(record)
                }
            }
        }
    }
}

@Composable
fun VisionTestCard(
    title: String,
    icon: ImageVector,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryItemCard(record: VisionTestRecord) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon based on type
            val icon = if (record.testType.contains("Amsler")) Icons.Default.GridOn else Icons.Default.Visibility
            val iconColor = if (record.testType.contains("Amsler")) Color(0xFF1976D2) else Color(0xFF7B1FA2)
            Icon(icon, null, tint = iconColor)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(record.testType, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Eye: ${record.eyeSide}", fontSize = 14.sp, color = Color.Gray)

                // Show finalAcuity for Tumbling E tests
                if (!record.finalAcuity.isNullOrBlank()) {
                    Text(
                        "Result: ${record.finalAcuity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                }

                if (!record.notes.isNullOrBlank()) {
                    Text("Note: ${record.notes}", fontSize = 12.sp, color = Color.DarkGray, maxLines = 1)
                }
            }

            Text(
                // Simple date parsing (you can format this better)
                text = record.timestamp.take(10),
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
    }
}