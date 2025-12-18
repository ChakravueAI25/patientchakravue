package com.org.patientchakravue

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// --- THEME COLORS ---
val PurplePrimary = Color(0xFF6200EE)
val PurpleLight = Color(0xFFBB86FC)
val BgColor = Color(0xFFF5F5F5)
val CardBg = Color.White

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
            NavigationBar(containerColor = CardBg) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(if(selectedTab==0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PurplePrimary)
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(if(selectedTab==1) Icons.Filled.MedicalServices else Icons.Outlined.MedicalServices, contentDescription = "Post-Op") },
                    label = { Text("Post-Op") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PurplePrimary)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { showVisionPopup = true },
                    icon = { Icon(Icons.Outlined.Visibility, contentDescription = "Vision") },
                    label = { Text("Vision") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PurplePrimary)
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { onLogout() },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PurplePrimary)
                )
            }
        },
        containerColor = BgColor
    ) { padding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 300),
            modifier = Modifier.padding(padding)
        ) { tabIndex ->
            when (tabIndex) {
                0 -> HomeTab(patient, showSnackbar)
                1 -> PostOpTab(patient, showSnackbar)
                else -> Box(Modifier.fillMaxSize())
            }
        }
    }

    if (showVisionPopup) {
        AlertDialog(
            onDismissRequest = { showVisionPopup = false },
            title = { Text("Select Test", color = PurplePrimary) },
            text = { Text("Which test would you like to perform?") },
            confirmButton = {
                Button(
                    onClick = { showVisionPopup = false; showSnackbar("Vision Test Coming Soon") },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) { Text("Vision Test (E)") }
            },
            dismissButton = {
                Button(
                    onClick = { showVisionPopup = false; showSnackbar("Amsler Grid Coming Soon") },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleLight)
                ) { Text("Amsler Grid") }
            },
            containerColor = CardBg
        )
    }
}

@Composable
fun HomeTab(patient: Patient, showSnackbar: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiRepository() }
    var showMedDialog by remember { mutableStateOf(false) }
    var graphData by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var notes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        launch {
            notes = api.getMessages(patient.id)
            val stats = api.getAdherenceStats(patient.id)
            if (stats != null && stats["weekly"] is List<*>) {
                graphData = (stats["weekly"] as List<*>).filterIsInstance<Map<String, Any>>()
            }
        }
    }

    val totalExpected = graphData.sumOf { (it["expected"] as? Number)?.toInt() ?: 0 }
    val totalTaken = graphData.sumOf { (it["taken"] as? Number)?.toInt() ?: 0 }
    val progress = if (totalExpected > 0) totalTaken.toFloat() / totalExpected.toFloat() else 0f

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ... (Header code same as Android version) ...
        Text("Home Dashboard", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

        // MEDICATION RING
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth().clickable { showMedDialog = true }
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                    CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize(), color = PurplePrimary.copy(alpha = 0.2f))
                    CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize(), color = PurplePrimary)
                    Icon(Icons.Filled.Medication, null, tint = PurplePrimary)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Medication", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("$totalTaken / $totalExpected Taken", fontSize = 16.sp, color = Color.Gray)
                }
            }
        }

        // GRAPH (Simplified for KMP)
        Card(
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Weekly Adherence", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val dataMap = graphData.associate { it["day"].toString() to ((it["taken"] as? Number)?.toFloat() ?: 0f) }
                    days.forEach { day ->
                        val value = dataMap[day] ?: 0f
                        val heightFraction = (value / 5f).coerceIn(0.1f, 1f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.width(12.dp).fillMaxHeight(heightFraction).background(PurplePrimary, RoundedCornerShape(4.dp)))
                            Text(day.take(1), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }

    // Popup logic...
    if (showMedDialog) {
        AlertDialog(
            onDismissRequest = { showMedDialog = false },
            title = { Text("Mark Medicines") },
            text = { Text("Medicine list not fully implemented in KMP demo.") }, // Simplified for brevity
            confirmButton = { TextButton(onClick = { showMedDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
fun PostOpTab(patient: Patient, showSnackbar: (String) -> Unit) {
    var pain by remember { mutableFloatStateOf(0f) }
    var swelling by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Daily Recovery", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))

        Card(colors = CardDefaults.cardColors(containerColor = CardBg)) {
            Column(modifier = Modifier.padding(16.dp)) {
                LabelledSlider("Pain Level", pain) { pain = it }
                LabelledSlider("Swelling", swelling) { swelling = it }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showSnackbar("Image Upload requires platform-specific code. Skipping for now.") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Icon(Icons.Default.CameraAlt, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Photo (Coming Soon)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showSnackbar("Report Submitted!") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) { Text("Submit Report") }
            }
        }
    }
}

@Composable
fun LabelledSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Black)
            Text(value.toInt().toString(), color = PurplePrimary, fontWeight = FontWeight.Bold)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..10f, steps = 9)
    }
}
