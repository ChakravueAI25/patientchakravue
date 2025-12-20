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
import com.org.patientchakravue.platform.currentEpochSeconds
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
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

            // Render one card per visit/doctor (prescription card). Each card contains all medicines prescribed by that visit's doctor.
            val visits = patient.visits ?: emptyList()
            items(visits) { visit ->
                val doctorData = visit.stages?.doctor?.data
                val prescription = doctorData?.prescription as? JsonObject ?: return@items
                val itemsList = prescription["items"] as? JsonArray ?: JsonArray(emptyList())

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        // Doctor name (if provided)
                        val docName = try {
                            prescription["doctor_name"]?.jsonPrimitive?.contentOrNull ?: "Doctor"
                        } catch (_: Exception) {
                            "Doctor"
                        }

                        Text(text = docName, fontWeight = FontWeight.Bold)

                        // Next appointment (only if backend provides key 'next_visit' in this prescription)
                        val nextVisitText = prescription["next_visit"]?.toString()
                        if (!nextVisitText.isNullOrEmpty()) {
                            Text(text = "Next Appointment: $nextVisitText", color = Color.Gray, fontSize = 12.sp)
                        } else {
                            Text(text = "Next Appointment: ", color = Color.Gray, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(12.dp))

                        // Render each medicine prescribed in this card
                        itemsList.forEach { medEl ->
                            if (medEl !is JsonObject) return@forEach
                            val medName = medEl["name"]?.jsonPrimitive?.contentOrNull ?: "Medicine"
                            val dosage = medEl["dosage"]?.jsonPrimitive?.contentOrNull
                            val frequency = medEl["frequency"]?.jsonPrimitive?.contentOrNull

                            // Find doses for this medicine from today's doses
                            val medDoses = todayDoses.filter { it.medicine_name == medName }
                            val sorted = medDoses.sortedBy { it.scheduled_iso }
                            val nextUntaken = sorted.firstOrNull { !it.taken }
                            val activeLabel = nextUntaken?.dose_label ?: sorted.firstOrNull()?.dose_label ?: ""

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = medName)
                                    Spacer(Modifier.height(4.dp))
                                    if (!dosage.isNullOrEmpty() || !frequency.isNullOrEmpty()) {
                                        Text(text = listOfNotNull(dosage, frequency).joinToString(" · "), color = Color.Gray, fontSize = 12.sp)
                                    } else {
                                        Text(text = nextUntaken?.scheduled_time ?: "—", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Spacer(Modifier.height(2.dp))
                                    Text(text = activeLabel, color = Color.Gray, fontSize = 12.sp)
                                }

                                val enabled = remember(nextUntaken?.id, nextUntaken?.taken) {
                                    var en = false
                                    if (nextUntaken != null && !nextUntaken.taken) {
                                        try {
                                            val schedInst = Instant.parse(nextUntaken.scheduled_iso)
                                            val nowEpoch = currentEpochSeconds()
                                            en = schedInst.epochSeconds <= nowEpoch
                                        } catch (_: Exception) {
                                            en = false
                                        }
                                    }
                                    en
                                }

                                var buttonEnabled by remember(nextUntaken?.id, nextUntaken?.taken) { mutableStateOf(enabled) }

                                Button(
                                    onClick = {
                                        buttonEnabled = false
                                        scope.launch {
                                            nextUntaken?.let { dose -> api.markDoseTaken(patient.id, dose.id) }
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

                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}