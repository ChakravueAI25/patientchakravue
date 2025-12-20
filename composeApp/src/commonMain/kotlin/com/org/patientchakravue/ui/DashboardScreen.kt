@file:OptIn(kotlin.time.ExperimentalTime::class)
@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")
package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Composable
fun DashboardScreen(
    patient: Patient,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdherence: () -> Unit,
    onNavigateToMedicineList: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val api = remember { ApiRepository() }
    var adherence by remember { mutableStateOf<AdherenceResponse?>(null) }
    var todayDoses by remember { mutableStateOf<List<DoseItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Initial load
    LaunchedEffect(Unit) {
        adherence = api.getAdherenceStats(patient.id)
        todayDoses = api.getTodayDoses(patient.id)
    }

    // Build prescription lookup by medicine name from latest visit prescription items (if present)
    val prescriptionByName = remember(patient) {
        val map = mutableMapOf<String, JsonObject>()
        try {
            val visits = patient.visits ?: emptyList()
            val last = if (visits.isNotEmpty()) visits[visits.size - 1] else null
            val presElement = last?.stages?.doctor?.data?.prescription ?: patient.prescription
            if (presElement != null && presElement is JsonObject) {
                val itemsEl = presElement["items"]
                if (itemsEl is JsonArray) {
                    for (el in itemsEl) {
                        if (el is JsonObject) {
                            val name = el["name"]?.jsonPrimitive?.contentOrNull
                            if (name != null) {
                                map[name] = el
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // don't fail; leave map empty
        }
        map
    }

    // Group doses by medicine_name for one-card-per-medicine UX
    val groupedByMedicine = remember(todayDoses) {
        todayDoses.groupBy { it.medicine_name }
    }

    Scaffold(
        bottomBar = bottomBar
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header as first item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = patient.name ?: "Patient",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onNavigateToAdherence) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Adherence")
                    }

                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            }

            // Medicines section (pie + stats)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Medication, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("MEDICINES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(Modifier.height(12.dp))

                    val weekly = adherence?.weekly ?: emptyList()
                    val taken = weekly.sumOf { it.taken }
                    val total = weekly.sumOf { it.expected }.coerceAtLeast(1)
                    val progress = taken.toFloat() / total

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {

                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { progress },
                                strokeWidth = 10.dp,
                                modifier = Modifier.size(120.dp)
                            )
                            Icon(
                                Icons.Default.LocalPharmacy,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(Modifier.width(24.dp))

                        Text(
                            text = "$taken/$total Taken",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Prescription header
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Description, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("PRESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            // One item per medicine group
            items(groupedByMedicine.entries.toList()) { (medicineName, doses) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Text(
                            text = medicineName,
                            fontWeight = FontWeight.Bold
                        )

                        // Next visit display (only if backend provides next_visit at top-level of prescription)
                        val nextVisitText = remember(patient) {
                            var nv: String? = null
                            try {
                                val p = patient.prescription
                                if (p != null && p is JsonObject) {
                                    val el = p["next_visit"]
                                    if (el != null) nv = el.toString()
                                }
                            } catch (_: Exception) { }
                            nv
                        }

                        if (nextVisitText != null) {
                            Text(
                                text = "Next Appointment: $nextVisitText",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        } else {
                            Text(
                                text = "Next Appointment: ",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Determine prescription metadata (dosage/frequency) by name
                        val presItem = prescriptionByName[medicineName]
                        val dosage = presItem?.get("dosage")?.jsonPrimitive?.contentOrNull
                        val frequency = presItem?.get("frequency")?.jsonPrimitive?.contentOrNull

                        if (!dosage.isNullOrEmpty() || !frequency.isNullOrEmpty()) {
                            Text(text = listOfNotNull(dosage, frequency).joinToString(" · "), color = Color.Gray, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Sort doses by scheduled_iso ascending
                        val sorted = doses.sortedBy { it.scheduled_iso }

                        // Pick the first untaken dose (backend-driven)
                        val nextUntaken = sorted.firstOrNull { !it.taken }

                        // Display the active dose label (if present in backend dose record)
                        val activeLabel = nextUntaken?.dose_label ?: sorted.firstOrNull()?.dose_label ?: ""

                        Text(text = activeLabel, color = Color.Gray, fontSize = 12.sp)

                        Spacer(Modifier.height(8.dp))

                        // Taken button logic: enabled only if nextUntaken exists and its scheduled_iso <= now
                        val enabled = remember(nextUntaken?.id, nextUntaken?.taken) {
                            var en = false
                            if (nextUntaken != null && !nextUntaken.taken) {
                                try {
                                    val schedInst = Instant.parse(nextUntaken.scheduled_iso)
                                    val nowInst = Clock.System.now()
                                    val schedEpoch = schedInst.epochSeconds
                                    val nowEpoch = nowInst.epochSeconds
                                    en = schedEpoch <= nowEpoch
                                } catch (_: Exception) {
                                    en = false
                                }
                            }
                            en
                        }

                        var buttonEnabled by remember(nextUntaken?.id, nextUntaken?.taken) { mutableStateOf(enabled) }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(Modifier.weight(1f))

                            Button(
                                onClick = {
                                    // Disable immediately
                                    buttonEnabled = false
                                    scope.launch {
                                        nextUntaken?.let { dose ->
                                            api.markDoseTaken(patient.id, dose.id)
                                            }
                                            // Refresh from backend after response
                                            todayDoses = api.getTodayDoses(patient.id)
                                            adherence = api.getAdherenceStats(patient.id)
                                        }
                                },
                                enabled = buttonEnabled,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(if (nextUntaken?.taken == true) "Taken" else "Mark Taken", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}