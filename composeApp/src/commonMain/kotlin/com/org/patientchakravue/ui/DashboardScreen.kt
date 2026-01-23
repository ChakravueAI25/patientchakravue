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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.dose.DoseRefreshBus
import com.org.patientchakravue.model.*
import com.org.patientchakravue.platform.currentEpochSeconds
import com.org.patientchakravue.ui.language.LanguageSwitcherIcon
import com.org.patientchakravue.ui.language.LocalLanguageManager
import com.org.patientchakravue.ui.language.localizedString
import com.org.patientchakravue.ui.theme.AppBackground
import com.org.patientchakravue.ui.theme.PrescriptionCard
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
fun DashboardScreen(
    patient: Patient,
    onNavigateToProfile: () -> Unit,
    onNavigateToAdherence: () -> Unit,
    onNavigateToMedicineList: () -> Unit
) {
    val apiRepository = remember { ApiRepository() }
    var todayDoses by remember { mutableStateOf<List<DoseItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Get current language to trigger recomposition when it changes
    @Suppress("UNUSED_VARIABLE")
    val currentLang = LocalLanguageManager.current.currentLanguage

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

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent
        ) { padding ->
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
                        // Language Switcher Icon
                        LanguageSwitcherIcon(tint = Color(0xFF1A3B5D))
                        IconButton(onClick = onNavigateToAdherence) {
                            Icon(
                                Icons.AutoMirrored.Filled.ShowChart,
                                null,
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.Black)
                        }
                    }
                }

                /* ---------- MEDICINES / PIE ---------- */
                item {
                    Text(
                        localizedString("section_medicines"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A3B5D)
                    )
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
                                trackColor = Color.White
                            )
                            Icon(
                                Icons.Default.LocalPharmacy,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        Spacer(Modifier.width(24.dp))
                        Column {
                            Text(
                                "$taken/$total",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                localizedString("doses_taken_today"),
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                /* ---------- PRESCRIPTION ---------- */
                item {
                    Text(
                        localizedString("section_prescription"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1A3B5D)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                item {
                    PrescriptionCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {

                            Text(
                                localizedString("doctor_label"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )

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
                                    localizedString("next_appointment") + " ",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                                Text(
                                    nextAppointment ?: localizedString("not_scheduled"),
                                    color = Color(0xFF1976D2),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            Spacer(Modifier.height(12.dp))

                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.CenterHorizontally),
                                    color = Color(0xFF4CAF50)
                                )
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
                                    Text(localizedString("no_doses_today"), color = Color.Gray)
                                    Text(
                                        localizedString("check_back_tomorrow"),
                                        fontSize = 12.sp,
                                        color = Color.LightGray
                                    )
                                }
                            } else {
                                todayDoses.forEach { dose ->
                                    val enabled =
                                        !dose.taken &&
                                                Instant.parse(dose.scheduled_iso).epochSeconds <= currentEpochSeconds()

                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                    ) {

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Status Checkmark
                                            Icon(
                                                if (dose.taken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (dose.taken) Color(0xFF4CAF50) else Color.Gray,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(Modifier.width(12.dp))

                                            // 2. NEW: Medicine Image/Icon
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFE3F2FD), // Light blue background
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Medication,
                                                        contentDescription = "Medicine",
                                                        tint = Color(0xFF1976D2),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            // 3. Medicine Details
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    dose.medicine_name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )
                                                Text(
                                                    "${dose.dose_label} · ${dose.scheduled_time}",
                                                    fontSize = 13.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Spacer(Modifier.height(12.dp))

                                        // Mark Taken Button
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val ok = apiRepository.markDoseTaken(
                                                        patient.id,
                                                        dose.id
                                                    )
                                                    if (ok) {
                                                        refreshData()
                                                        DoseRefreshBus.emit()
                                                    }
                                                }
                                            },
                                            enabled = enabled,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4CAF50),
                                                disabledContainerColor = Color(0xFFE0E0E0)
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                if (dose.taken) Icons.Default.Check else Icons.Default.Medication,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                if (dose.taken) localizedString("btn_taken") else localizedString(
                                                    "btn_mark_taken"
                                                )
                                            )
                                        }
                                    }

                                    if (dose != todayDoses.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            thickness = 0.5.dp,
                                            color = Color.LightGray.copy(alpha = 0.5f)
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
}

/**
 * Format appointment date from ISO string to readable format like "Jan 10, 2026"
 */
private fun formatAppointmentDate(isoDate: String?): String? {
    if (isoDate.isNullOrBlank()) return null
    return try {
        // Parse the ISO date string - handle various formats
        val datePart = when {
            isoDate.contains("T") -> isoDate.substringBefore("T")
            isoDate.length >= 10 -> isoDate.take(10)
            else -> isoDate
        }
        val date = LocalDate.parse(datePart)
        formatDate(date)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Calculate next appointment date based on surgery milestones:
 * Surgery + 2 days, Surgery + 10 days, Surgery + 15 days.
 * This is a FALLBACK when backend has no scheduled appointments.
 */
private fun calculateNextAppointment(patient: Patient): String? {
    return try {
        // 1. Identify the Surgery Date (Start Date) - Try multiple sources
        var surgeryDateStr: String? = null

        // Try visits list first (most reliable - latest visit is surgery date)
        val visits = patient.visits
        if (!visits.isNullOrEmpty()) {
            // Get the latest visit
            val lastVisit = visits.lastOrNull()
            if (lastVisit != null) {
                // Try multiple date field names as backend uses different ones
                // Priority: nested stage dates first, then root-level visit dates
                surgeryDateStr = lastVisit.stages?.doctor?.stageCompletedAt?.takeIf { it.isNotBlank() }
                    ?: lastVisit.stages?.reception?.stageCompletedAt?.takeIf { it.isNotBlank() }
                    ?: lastVisit.visitDate?.takeIf { it.isNotBlank() }
                    ?: lastVisit.date?.takeIf { it.isNotBlank() }
                    ?: lastVisit.stageCompletedAt?.takeIf { it.isNotBlank() }
                    ?: lastVisit.createdAt?.takeIf { it.isNotBlank() }
            }
        }

        // Fallback to patient's createdAt if no visit date found
        if (surgeryDateStr.isNullOrBlank()) {
            surgeryDateStr = patient.createdAt?.takeIf { it.isNotBlank() }
        }

        println("[DEBUG] Surgery date string: $surgeryDateStr")
        println("[DEBUG] Patient visits count: ${visits?.size ?: 0}")
        if (!visits.isNullOrEmpty()) {
            val lastVisit = visits.lastOrNull()
            println("[DEBUG] Last visit - stages?.doctor?.stageCompletedAt: ${lastVisit?.stages?.doctor?.stageCompletedAt}")
            println("[DEBUG] Last visit - stages?.reception?.stageCompletedAt: ${lastVisit?.stages?.reception?.stageCompletedAt}")
            println("[DEBUG] Last visit - date: ${lastVisit?.date}, visitDate: ${lastVisit?.visitDate}, createdAt: ${lastVisit?.createdAt}")
        }

        if (!surgeryDateStr.isNullOrBlank()) {
            // Parse the ISO date string - handle various formats
            val datePart = when {
                surgeryDateStr.contains("T") -> surgeryDateStr.substringBefore("T")
                surgeryDateStr.length >= 10 -> surgeryDateStr.take(10)
                else -> surgeryDateStr
            }

            println("[DEBUG] Parsed date part: $datePart")

            val surgeryDate = LocalDate.parse(datePart)
            println("[DEBUG] Surgery LocalDate: $surgeryDate")

            // 2. Get Current Date to find the "Next" upcoming visit
            val today = Instant.fromEpochSeconds(currentEpochSeconds())
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
            println("[DEBUG] Today's date: $today")

            // 3. Define the Milestones (2, 10, 15 days)
            val milestones = listOf(2, 10, 15)

            // 4. Find the first milestone that hasn't passed yet
            for (days in milestones) {
                val scheduledDate = surgeryDate.plus(days, DateTimeUnit.DAY)
                println("[DEBUG] Checking milestone $days days: $scheduledDate")
                if (scheduledDate >= today) {
                    val result = formatDate(scheduledDate)
                    println("[DEBUG] Next appointment: $result")
                    return result
                }
            }

            // If all milestones passed, show the final one or a "Follow-up Completed" message
            return "Follow-up Completed"
        }

        println("[DEBUG] No surgery date found, returning null")
        null
    } catch (e: Exception) {
        println("[DEBUG] Error calculating appointment: ${e.message}")
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
