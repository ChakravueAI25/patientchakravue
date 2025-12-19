package com.org.patientchakravue.ui

import androidx.compose.foundation.clickable
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

data class UiMedicine(
    val name: String,
    val time: String,
    val doctorName: String
)

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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        adherence = api.getAdherenceStats(patient.id)
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

                val visits = patient.visits ?: emptyList()

                items(visits) { visit ->

                    val doctorData = visit.stages?.doctor?.data
                    val prescription = doctorData?.prescription as? Map<*, *> ?: return@items
                    val itemsList = prescription["items"] as? List<Map<String, Any>> ?: emptyList()

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

                            Text(
                                text = "Next Appointment: TBD",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(12.dp))

                            itemsList.forEach { med ->

                                val medName = med["name"]?.toString() ?: "Medicine"
                                val time = med["timing"]?.toString() ?: "—"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Text(
                                        text = medName,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = time,
                                        modifier = Modifier.weight(0.5f),
                                        fontSize = 12.sp
                                    )

                                    Button(
                                        onClick = {
                                            // REAL BACKEND CALL
                                            scope.launch {
                                                api.recordAdherence(
                                                    patientId = patient.id,
                                                    medicine = medName,
                                                    taken = 1
                                                )
                                                adherence = api.getAdherenceStats(patient.id)
                                            }
                                        },
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Taken", fontSize = 12.sp)
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