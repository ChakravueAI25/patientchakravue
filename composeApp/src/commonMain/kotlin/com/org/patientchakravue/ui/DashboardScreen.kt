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

@Suppress("UNUSED_PARAMETER")
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

    Scaffold(
        bottomBar = bottomBar
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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

            // MEDICINES SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Description, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("PRESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                // Render one card per distinct prescription/doctor grouping using today's doses
                // Group by doctor_id for card separation while keeping each dose independent
                val grouped = todayDoses.groupBy { it.doctor_id ?: "_unknown" }

                grouped.entries.forEach { (doctorId, doses) ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {

                                Text(
                                    text = "Doctor",
                                    fontWeight = FontWeight.Bold
                                )

                                // NEXT VISIT: backend must provide a field; show only if present in patient.prescription or top-level
                                // Per rules: Do NOT guess — display only if backend provided a top-level known key "next_visit"
                                // Patient model does not define next_visit; check patient.prescription JsonElement raw if available
                                val nextVisitText = remember(patient) {
                                    var nv: String? = null
                                    try {
                                        val p = patient.prescription
                                        // don't parse or guess structure; only check if there is a direct key named "next_visit" at top level
                                        if (p != null && p is JsonObject) {
                                            val el = p["next_visit"]
                                            if (el != null) nv = el.toString()
                                        }
                                    } catch (_: Exception) { /* ignore */ }
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

                                // Each dose row
                                doses.forEach { dose ->
                                    // Line 1 (Primary): medicine name + strength -> backend provides medicine_name; use it directly
                                    val primary = dose.medicine_name

                                    // Look up prescription metadata by medicine name to obtain dosage and frequency strings
                                    val presItem = prescriptionByName[dose.medicine_name]
                                    val dosage = presItem?.get("dosage")?.jsonPrimitive?.contentOrNull
                                    val frequency = presItem?.get("frequency")?.jsonPrimitive?.contentOrNull

                                    // Line 2 (Secondary - same line): show dosage · frequency if provided by backend
                                    val secondary = listOfNotNull(dosage, frequency).joinToString(" · ")

                                    // Line 3 (Tertiary): dose_label comes directly from backend dose record
                                    val tertiary = dose.dose_label

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = primary, fontWeight = FontWeight.SemiBold)

                                            Spacer(Modifier.height(4.dp))

                                            if (secondary.isNotEmpty()) {
                                                Text(text = secondary, color = Color.Gray, fontSize = 12.sp)
                                            } else {
                                                // If backend didn't provide dosage/frequency, fall back to scheduled_time as a visible value
                                                Text(text = dose.scheduled_time, color = Color.Gray, fontSize = 12.sp)
                                            }

                                            Spacer(Modifier.height(2.dp))

                                            Text(text = tertiary, color = Color.Gray, fontSize = 12.sp)
                                        }

                                        // Taken button: backend-driven
                                        var buttonEnabled by remember(dose.id, dose.taken) { mutableStateOf(!dose.taken) }

                                        Button(
                                            onClick = {
                                                // Disable immediately
                                                buttonEnabled = false
                                                scope.launch {
                                                    val ok = api.markDoseTaken(patient.id, dose.id)
                                                    // After backend response: re-fetch doses and adherence stats
                                                    todayDoses = api.getTodayDoses(patient.id)
                                                    adherence = api.getAdherenceStats(patient.id)
                                                    // Button remains disabled until backend exposes next eligible dose (handled by refetch)
                                                }
                                            },
                                            enabled = buttonEnabled,
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text(
                                                if (dose.taken) "Taken" else "Mark Taken",
                                                fontSize = 12.sp
                                            )
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
    }
}