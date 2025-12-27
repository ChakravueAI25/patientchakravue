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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

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

    // Calculate next appointment (last visit + 15 days)
    val nextAppointment = remember(patient) {
        calculateNextAppointment(patient)
    }

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
                            modifier = Modifier.size(130.dp),
                            color = Color(0xFF4CAF50), // Green color
                            trackColor = Color(0xFFE0E0E0)
                        )
                        Icon(Icons.Default.LocalPharmacy, null, modifier = Modifier.size(40.dp), tint = Color(0xFF4CAF50))
                    }
                    Spacer(Modifier.width(24.dp))
                    Column {
                        Text("$taken/$total", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        Text("Doses Taken Today", fontSize = 14.sp, color = Color.Gray)
                    }
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

                        Text("Doctor", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                        // Next Appointment Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                tint = Color(0xFF1976D2),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Next Appointment: ",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Text(
                                nextAppointment ?: "Not scheduled",
                                color = Color(0xFF1976D2),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                        Spacer(Modifier.height(12.dp))

                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else if (todayDoses.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("No doses scheduled for today.", color = Color.Gray)
                                Text("Check back tomorrow!", fontSize = 12.sp, color = Color.LightGray)
                            }
                        } else {
                            todayDoses.forEach { dose ->
                                val enabled =
                                    !dose.taken &&
                                            Instant.parse(dose.scheduled_iso).epochSeconds <= currentEpochSeconds()

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (dose.taken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (dose.taken) Color(0xFF4CAF50) else Color.Gray,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(dose.medicine_name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                                            Text(
                                                "${dose.dose_label} · ${dose.scheduled_time}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

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
                                            containerColor = if (dose.taken) Color(0xFF4CAF50) else Color(0xFF1976D2),
                                            disabledContainerColor = Color(0xFFE0E0E0)
                                        )
                                    ) {
                                        Icon(
                                            if (dose.taken) Icons.Default.Check else Icons.Default.Medication,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (dose.taken) "Taken ✓" else "Mark as Taken")
                                    }
                                }

                                if (dose != todayDoses.last()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        thickness = 0.5.dp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Add some bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Calculate next appointment date as last visit date + 15 days
 */
private fun calculateNextAppointment(patient: Patient): String? {
    return try {
        // Try to get date from last visit
        val visits = patient.visits
        if (!visits.isNullOrEmpty()) {
            val lastVisit = visits.lastOrNull()
            val dateStr = lastVisit?.date ?: lastVisit?.createdAt

            if (dateStr != null) {
                // Parse the date (handle ISO format like "2025-12-15T10:30:00")
                val datePart = dateStr.substringBefore("T").takeIf { it.isNotEmpty() } ?: dateStr.take(10)
                val localDate = LocalDate.parse(datePart)
                val nextDate = localDate.plus(15, DateTimeUnit.DAY)
                return formatDate(nextDate)
            }
        }

        // Fallback: Try createdAt from patient
        val createdAt = patient.createdAt
        if (createdAt != null) {
            val datePart = createdAt.substringBefore("T").takeIf { it.isNotEmpty() } ?: createdAt.take(10)
            val localDate = LocalDate.parse(datePart)
            val nextDate = localDate.plus(15, DateTimeUnit.DAY)
            return formatDate(nextDate)
        }

        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Format date to readable format like "Jan 10, 2026"
 */
private fun formatDate(date: LocalDate): String {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${months[date.monthNumber - 1]} ${date.dayOfMonth}, ${date.year}"
}
