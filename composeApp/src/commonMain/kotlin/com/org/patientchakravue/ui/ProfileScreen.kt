package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.platform.saveAndNotifyDownload
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val patientId = sessionManager.getPatient()?.id
    var patient by remember { mutableStateOf<Patient?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apiRepository = remember { ApiRepository() }

    // Get current language to trigger recomposition when it changes
    // Trigger recomposition on language change
    LocalLanguageManager.current.currentLanguage

    LaunchedEffect(patientId) {
        if (patientId != null) {
            isLoading = true
            scope.launch {
                patient = apiRepository.getPatientProfile(patientId)
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedString("profile_title")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (patient != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Basic Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(localizedString("basic_info"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileRow(localizedString("label_name"), patient?.name)
                        ProfileRow(localizedString("label_age"), patient?.age)
                        ProfileRow(localizedString("label_sex"), patient?.sex)
                        ProfileRow(localizedString("label_blood"), patient?.bloodType)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Contact Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(localizedString("contact_info"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileRow(localizedString("label_phone"), patient?.phone)
                        ProfileRow(localizedString("label_email"), patient?.email)
                        ProfileRow(localizedString("label_address"), patient?.address)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Emergency Contact
                if (!patient?.emergencyContactName.isNullOrBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(localizedString("emergency_contact"), style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            ProfileRow(localizedString("label_name"), patient?.emergencyContactName)
                            ProfileRow(localizedString("label_phone"), patient?.emergencyContactPhone)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // System Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(localizedString("system_info"), style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileRow(localizedString("label_patient_id"), patient?.id)
                        ProfileRow(localizedString("label_registered"), patient?.registrationId) // Assuming registrationId is the registration date.
                    }
                }

                Spacer(Modifier.height(24.dp))

                // --- HOSPITAL CARD ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "SPARK EYE & DENTAL HOSPITAL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1976D2)
                            )
                            Text(
                                "Dr. Ajay Chakravarthy",
                                fontSize = 14.sp,
                                color = Color.DarkGray
                            )
                        }

                        // Download Button
                        if (isDownloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF1976D2)
                            )
                        } else {
                            IconButton(onClick = {
                                isDownloading = true
                                scope.launch {
                                    val pdfData = apiRepository.downloadCaseSheet(patientId ?: "")
                                    if (pdfData != null) {
                                        saveAndNotifyDownload("CaseSheet_Spark.pdf", pdfData)
                                    }
                                    isDownloading = false
                                }
                            }) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download Case Sheet",
                                    tint = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        sessionManager.clearSession()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(localizedString("logout_btn"))
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Could not load profile.")
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(120.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
