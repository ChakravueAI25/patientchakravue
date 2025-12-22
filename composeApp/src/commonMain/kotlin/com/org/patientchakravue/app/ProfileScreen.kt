package com.org.patientchakravue.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.Patient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    patientId: String,
    onLogout: () -> Unit
) {
    var patient by remember { mutableStateOf<Patient?>(null) }
    val apiRepository = remember { ApiRepository() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(patientId) {
        coroutineScope.launch {
            patient = apiRepository.getPatientProfile(patientId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Patient Profile") })
        },
        bottomBar = {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Logout")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            patient?.let {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileRow("Full Name", it.name)
                ProfileRow("Age", it.age)
                ProfileRow("Sex", it.sex)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Contact Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileRow("Phone Number", it.phone)
                ProfileRow("Email", it.email)
                ProfileRow("Address", it.address)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Medical Information", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileRow("Blood Group", it.bloodType)
                ProfileRow("Registration ID", it.registrationId)

                Spacer(modifier = Modifier.height(16.dp))
                Text("Emergency Contact", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                ProfileRow("Name", it.emergencyContactName)
                ProfileRow("Phone", it.emergencyContactPhone)
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
