package com.org.patientchakravue.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.AdherenceResponse
import com.org.patientchakravue.model.DoctorNote
import com.org.patientchakravue.model.Patient

@Composable
fun DashboardScreen(
    patient: Patient,
    onLogout: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showVisionPopup by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if(selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if(selectedTab == 1) Icons.Filled.MedicalServices else Icons.Outlined.MedicalServices, null) },
                    label = { Text("Post-Op") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showVisionPopup = true },
                    icon = { Icon(Icons.Outlined.Visibility, null) },
                    label = { Text("Vision") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { onLogout() },
                    icon = { Icon(Icons.Outlined.Settings, null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Crossfade(targetState = selectedTab, animationSpec = tween(300), modifier = Modifier.padding(padding)) { index ->
            when (index) {
                0 -> HomeTab(patient, showSnackbar)
                1 -> PostOpTab(patient, showSnackbar)
            }
        }
    }

    if (showVisionPopup) {
        AlertDialog(
            onDismissRequest = { showVisionPopup = false },
            title = { Text("Select Test") },
            text = { Text("Choose a test to perform.") },
            confirmButton = { Button(onClick = { showVisionPopup = false }) { Text("Vision Test") } },
            dismissButton = { Button(onClick = { showVisionPopup = false }) { Text("Amsler Grid") } }
        )
    }
}

@Composable
fun HomeTab(patient: Patient, showSnackbar: (String) -> Unit) {
    val api = remember { ApiRepository() }
    var adherenceData by remember { mutableStateOf<AdherenceResponse?>(null) }
    var notes by remember { mutableStateOf<List<DoctorNote>>(emptyList()) }

    LaunchedEffect(Unit) {
        adherenceData = api.getAdherenceStats(patient.id)
        notes = api.getMessages(patient.id)
    }

    val stats = adherenceData?.weekly ?: emptyList()
    val totalTaken = stats.sumOf { it.taken }
    val totalExpected = stats.sumOf { it.expected }
    val progress = if (totalExpected > 0) totalTaken.toFloat() / totalExpected else 0f

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Home Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Medication Card
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                    CircularProgressIndicator(progress = { 1f }, color = Color.LightGray)
                    CircularProgressIndicator(progress = { progress }, color = Color(0xFF6200EE))
                    Icon(Icons.Default.Medication, null, tint = Color(0xFF6200EE))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Medication", fontWeight = FontWeight.Bold)
                    Text("$totalTaken / $totalExpected Taken", color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Weekly Adherence Chart
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Weekly Adherence", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    stats.forEach { day ->
                        val barHeight = if (day.expected > 0) day.taken.toFloat() / day.expected else 0.1f
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.width(12.dp).fillMaxHeight(barHeight.coerceAtLeast(0.1f)).background(Color(0xFF6200EE), RoundedCornerShape(4.dp)))
                            Text(day.day.take(1), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Doctor Notes
        Text("Doctor's Notes", fontWeight = FontWeight.Bold)
        notes.forEach { note ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(note.noteText ?: "", Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun PostOpTab(patient: Patient, showSnackbar: (String) -> Unit) {
    // Keep your existing PostOpTab logic here
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Post-Op Tab Ready")
    }
}

