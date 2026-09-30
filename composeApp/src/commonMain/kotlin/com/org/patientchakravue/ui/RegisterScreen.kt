package com.org.patientchakravue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.model.RegisterRequest
import com.org.patientchakravue.model.RegisterResponse
import com.org.patientchakravue.platform.getDeviceId
import com.org.patientchakravue.platform.registerFcmTokenAfterLogin
import kotlinx.coroutines.launch

private val Navy = Color(0xFF1A3B5D)
private val Green = Color(0xFF00D25B)
private val SexOptions = listOf("Male", "Female", "Other")
private val BloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

@Composable
fun RegisterScreen(
    onBack: () -> Unit,
    onRegistered: (RegisterResponse) -> Unit,
    showSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiRepository() }

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var ecName by remember { mutableStateOf("") }
    var ecPhone by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var aadhaar by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var insType by remember { mutableStateOf("") }
    var insCompany by remember { mutableStateOf("") }
    var insTpa by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Same rules the backend enforces (register.py); the server stays the authority.
    val errors = buildMap<String, String> {
        if (name.isBlank()) put("name", "Name is required")
        if (age.toIntOrNull()?.let { it in 1..129 } != true) put("age", "Enter a valid age")
        if (sex.isBlank()) put("sex", "Select sex")
        if (!Regex("^\\d{10}$").matches(phone)) put("phone", "Enter a 10-digit phone number")
        if (email.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) put("email", "Invalid email")
        if (aadhaar.isNotBlank() && !Regex("^\\d{12}$").matches(aadhaar)) put("aadhaar", "Aadhaar must be 12 digits")
        if (pan.isNotBlank() && !Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(pan.uppercase())) put("pan", "Invalid PAN")
    }
    fun err(key: String) = if (submitted) errors[key] else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Patient Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text("Create your patient account", color = Color.Gray, fontSize = 14.sp)
            }
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))

        FormSection("Basic Information") {
            FormField("Full name *", name, { name = it }, err("name"))
            FormField("Age *", age, { age = it.filter(Char::isDigit).take(3) }, err("age"), KeyboardType.Number)
            DropdownField("Sex *", sex, SexOptions, { sex = it }, err("sex"))
            DropdownField("Blood group", bloodType, BloodTypes, { bloodType = it })
            FormField("Allergies (comma separated)", allergies, { allergies = it })
        }
        FormSection("Contact Information") {
            FormField("Phone number *", phone, { phone = it.filter(Char::isDigit).take(10) }, err("phone"), KeyboardType.Phone)
            FormField("Email", email, { email = it.trim() }, err("email"), KeyboardType.Email)
            FormField("Address", address, { address = it }, singleLine = false)
        }
        FormSection("Emergency Contact") {
            FormField("Contact name", ecName, { ecName = it })
            FormField("Contact phone", ecPhone, { ecPhone = it.filter(Char::isDigit).take(10) }, keyboard = KeyboardType.Phone)
        }
        FormSection("Identity & Insurance (optional)") {
            FormField("Aadhaar number", aadhaar, { aadhaar = it.filter(Char::isDigit).take(12) }, err("aadhaar"), KeyboardType.Number)
            FormField("PAN number", pan, { pan = it.uppercase().take(10) }, err("pan"))
            FormField("Insurance type", insType, { insType = it })
            FormField("Insurance company", insCompany, { insCompany = it })
            FormField("Insurance TPA", insTpa, { insTpa = it })
        }

        if (isLoading) {
            CircularProgressIndicator(color = Green)
        } else {
            Button(
                onClick = {
                    submitted = true
                    if (errors.isNotEmpty()) {
                        showSnackbar("Please fix the highlighted fields")
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val (res, message) = api.registerPatient(
                                RegisterRequest(
                                    name = name.trim(), age = age, sex = sex, phone = phone,
                                    email = email, address = address.trim(), bloodType = bloodType,
                                    aadhaarNumber = aadhaar, panNumber = pan.uppercase(),
                                    insuranceType = insType.trim(), insuranceCompany = insCompany.trim(),
                                    insuranceTPA = insTpa.trim(), allergies = allergies.trim(),
                                    emergencyContactName = ecName.trim(), emergencyContactPhone = ecPhone,
                                    deviceId = getDeviceId()
                                )
                            )
                            if (res != null) onRegistered(res) else showSnackbar(message ?: "Registration failed")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) { Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Shown once after registration: the generated username + password, then signs the patient in. */
@Composable
fun RegistrationSuccessScreen(
    credentials: RegisterResponse,
    onContinue: () -> Unit,
    showSnackbar: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val api = remember { ApiRepository() }
    val sessionManager = remember { SessionManager() }
    val clipboard = LocalClipboardManager.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Registration Successful", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
        Spacer(Modifier.height(4.dp))
        Text(
            "Save these details. You will use them to sign in.",
            color = Color.Gray, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                CredentialRow("Username", credentials.username) {
                    clipboard.setText(AnnotatedString(credentials.username)); showSnackbar("Username copied")
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CredentialRow("Password", credentials.password) {
                    clipboard.setText(AnnotatedString(credentials.password)); showSnackbar("Password copied")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "This password is shown only once.",
            color = Color(0xFFD32F2F), fontSize = 13.sp, fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(24.dp))
        if (isLoading) {
            CircularProgressIndicator(color = Green)
        } else {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            val patient = api.login(credentials.username, credentials.password)
                            if (patient != null) {
                                sessionManager.savePatient(patient)
                                registerFcmTokenAfterLogin(patient.id)
                                onContinue()
                            } else {
                                showSnackbar("Could not sign in. Please try again.")
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) { Text("Sign in", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CredentialRow(label: String, value: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Navy)
        }
        TextButton(onClick = onCopy) { Text("Copy") }
    }
}

@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
            content()
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth()
        )
        // Transparent overlay: a readOnly text field swallows clicks, so catch them here.
        Box(
            Modifier.matchParentSize().padding(bottom = if (error != null) 20.dp else 0.dp)
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onSelect(it); expanded = false })
            }
        }
    }
}
