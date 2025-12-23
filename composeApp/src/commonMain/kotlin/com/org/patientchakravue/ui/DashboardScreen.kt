@file:OptIn(kotlin.time.ExperimentalTime::class)
@file:Suppress("DEPRECATION", "UNUSED_PARAMETER")
package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.org.patientchakravue.dose.DoseRefreshBus
import com.org.patientchakravue.model.*
import com.org.patientchakravue.platform.currentEpochSeconds
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

@Composable
fun DashboardScreen(
    patient: Patient,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdherence: () -> Unit,
    onNavigateToMedicineList: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val apiRepository = remember { ApiRepository() }
    var todayDoses by remember { mutableStateOf<List<DoseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun refreshData() {
        isLoading = true
        todayDoses = apiRepository.getTodayDoses(patient.id)
        isLoading = false
    }

    LaunchedEffect(Unit) { refreshData() }
    LaunchedEffect(Unit) {
        DoseRefreshBus.events.collect { refreshData() }
    }

    Scaffold(bottomBar = bottomBar) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {

            /* ---------- HEADER ---------- */
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        patient.name ?: "Patient",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onNavigateToAdherence) {
                        Icon(Icons.AutoMirrored.Filled.ShowChart, null)
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, null)
                    }
                }
            }

            /* ---------- MEDICINES / PIE ---------- */
            item {
                Text("MEDICINES", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))

                val taken = todayDoses.count { it.taken }
                val total = todayDoses.size
                val progress = if (total > 0) taken.toFloat() / total else 0f

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            strokeWidth = 12.dp,
                            modifier = Modifier.size(130.dp)
                        )
                        Icon(Icons.Default.LocalPharmacy, null, modifier = Modifier.size(40.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                    Text("$taken/$total Taken", fontSize = 22.sp, fontWeight = FontWeight.Medium)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            /* ---------- PRESCRIPTION ---------- */
            item {
                Text("PRESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {

                        Text("Doctor 1", fontWeight = FontWeight.Bold)
                        Text("Next Appointment:", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (todayDoses.isEmpty()) {
                            Text("No doses scheduled for today.", color = Color.Gray)
                        } else {
                            todayDoses.forEach { dose ->
                                val enabled =
                                    !dose.taken &&
                                            Instant.parse(dose.scheduled_iso).epochSeconds <= currentEpochSeconds()

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {

                                    Text(dose.medicine_name, fontWeight = FontWeight.Medium)
                                    Text("1 drop twice daily", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "${dose.dose_label} · ${dose.scheduled_time}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val ok = apiRepository.markDoseTaken(patient.id, dose.id)
                                                if (ok) {
                                                    refreshData()
                                                    DoseRefreshBus.emit()
                                                }
                                            }
                                        },
                                        enabled = enabled,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            disabledContainerColor = Color(0xFFE0E0E0)
                                        )
                                    ) {
                                        Text(if (dose.taken) "Taken" else "Mark Taken")
                                    }
                                }

                                if (dose != todayDoses.last()) Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}
