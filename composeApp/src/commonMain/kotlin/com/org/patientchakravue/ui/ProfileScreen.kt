package com.org.patientchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.org.patientchakravue.ui.language.LocalLanguageManager
import com.org.patientchakravue.ui.language.localizedString
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSwitchAccount: (Patient) -> Unit,
    onAddAccount: () -> Unit
) {
    val initialPatient = remember { sessionManager.getPatient() }
    val patientId = initialPatient?.id
    var patient by remember { mutableStateOf<Patient?>(initialPatient) }
    var isLoading by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val apiRepository = remember { ApiRepository() }

    // Trigger recomposition on language change
    LocalLanguageManager.current.currentLanguage

    LaunchedEffect(patientId) {
        if (patientId != null) {
            scope.launch {
                val remotePatient = apiRepository.getPatientProfile(patientId)
                if (remotePatient != null) {
                    patient = remotePatient
                    sessionManager.savePatient(remotePatient)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header row: back arrow + title/subtitle (same line)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1A3B5D),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            localizedString("profile_title"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3B5D)
                        )
                        Text(
                            "Your personal information",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }

                    // Spacer to keep title centered (balances IconButton width)
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Basic Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(localizedString("basic_info"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
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
                        Text(localizedString("contact_info"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
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
                            Text(localizedString("emergency_contact"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
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
                        Text(localizedString("system_info"), style = MaterialTheme.typography.titleMedium, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D))
                        Spacer(modifier = Modifier.height(8.dp))
                        ProfileRow(localizedString("label_patient_id"), patient?.id)
                        ProfileRow(localizedString("label_registered"), patient?.registrationId)
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

                Spacer(Modifier.height(24.dp))

                OutlinedButton(
                    onClick = { showAccounts = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change account")
                }

                Spacer(Modifier.height(12.dp))

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

                Spacer(Modifier.height(16.dp))
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Could not load profile.")
            }
        }
    } // Close main Box

    if (showAccounts) {
        AccountSwitcherDialog(
            sessionManager = sessionManager,
            currentId = patientId,
            onDismiss = { showAccounts = false },
            onSwitch = { showAccounts = false; onSwitchAccount(it) },
            onAdd = { showAccounts = false; onAddAccount() }
        )
    }
}

/** Gmail-style account list: tap to switch, X to forget a saved account, "Add account" to sign in another. */
@Composable
private fun AccountSwitcherDialog(
    sessionManager: SessionManager,
    currentId: String?,
    onDismiss: () -> Unit,
    onSwitch: (Patient) -> Unit,
    onAdd: () -> Unit
) {
    var accounts by remember { mutableStateOf(sessionManager.getSavedAccounts()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an account", fontWeight = FontWeight.Bold, color = Color(0xFF1A3B5D)) },
        text = {
            Column {
                accounts.forEach { acc ->
                    val isCurrent = acc.id == currentId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCurrent) { onSwitch(acc) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFF1976D2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (acc.name?.firstOrNull() ?: '?').uppercase(),
                                color = Color.White, fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(acc.name ?: "Patient", fontWeight = FontWeight.Medium)
                            Text(
                                acc.registrationId ?: acc.phone ?: "",
                                fontSize = 12.sp, color = Color.Gray
                            )
                        }
                        if (isCurrent) {
                            Icon(Icons.Default.Check, "Current account", tint = Color(0xFF4CAF50))
                        } else {
                            IconButton(onClick = {
                                sessionManager.removeAccount(acc.id)
                                accounts = sessionManager.getSavedAccounts()
                            }) { Icon(Icons.Default.Close, "Remove account", tint = Color.Gray) }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).border(1.dp, Color.Gray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.Add, null, tint = Color(0xFF1976D2)) }
                    Spacer(Modifier.width(12.dp))
                    Text("Add account", fontWeight = FontWeight.Medium, color = Color(0xFF1976D2))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
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
