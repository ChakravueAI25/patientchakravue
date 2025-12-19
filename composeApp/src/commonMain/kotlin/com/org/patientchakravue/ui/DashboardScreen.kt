package com.org.patientchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    LaunchedEffect(Unit) {
        adherence = api.getAdherenceStats(patient.id)
    }

    Scaffold(
        bottomBar = bottomBar
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
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
                    Icon(Icons.Default.ShowChart, contentDescription = "Adherence")
                }

                IconButton(onClick = onNavigateToProfile) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                }
            }

            // PIE CHART (simplified circular indicator)
            Card(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clickable { onNavigateToMedicineList() },
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Prescriptions", fontWeight = FontWeight.SemiBold)

                    Spacer(Modifier.height(16.dp))

                    val weekly = adherence?.weekly ?: emptyList()
                    val taken = weekly.sumOf { it.taken }
                    val expected = weekly.sumOf { it.expected }
                    val progress =
                        if (expected > 0) taken.toFloat() / expected else 0f

                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            strokeWidth = 10.dp,
                            modifier = Modifier.size(120.dp)
                        )
                        Text("${(progress * 100).toInt()}%", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // PRESCRIPTION LIST
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                val visits = patient.visits ?: emptyList()

                items(visits) { visit ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Doctor Visit",
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(8.dp))

                            Text("Next visit: TBD", color = Color.Gray)

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = { 
                                    // backend record call can be wired here
                                },
                                shape = CircleShape
                            ) {
                                Text("Mark Medicine Taken")
                            }
                        }
                    }
                }
            }
        }
    }
}