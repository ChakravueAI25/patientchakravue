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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

    // Multi-step form state
    var step by remember { mutableIntStateOf(1) }

    // Step 1 Fields
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

    // Step 2 Fields
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var submittedStep1 by remember { mutableStateOf(false) }
    var submittedStep2 by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Step 1 Validation
    val errorsStep1 = buildMap<String, String> {
        if (name.isBlank()) put("name", "Name is required")
        if (age.toIntOrNull()?.let { it in 1..129 } != true) put("age", "Enter a valid age")
        if (sex.isBlank()) put("sex", "Select sex")
        if (!Regex("^\\d{10}$").matches(phone)) put("phone", "Enter a 10-digit phone number")
        if (email.isNotBlank() && !Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email)) put("email", "Invalid email")
        if (aadhaar.isNotBlank() && !Regex("^\\d{12}$").matches(aadhaar)) put("aadhaar", "Aadhaar must be 12 digits")
        if (pan.isNotBlank() && !Regex("^[A-Z]{5}[0-9]{4}[A-Z]$").matches(pan.uppercase())) put("pan", "Invalid PAN")
    }

    // Step 2 Validation
    val errorsStep2 = buildMap<String, String> {
        if (password.length < 8) put("password", "Password must be at least 8 characters")
        if (password != confirmPassword) put("confirmPassword", "Passwords do not match")
    }

    fun err1(key: String) = if (submittedStep1) errorsStep1[key] else null
    fun err2(key: String) = if (submittedStep2) errorsStep2[key] else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (step == 2) step = 1 else onBack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Patient Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Navy)
                Text(
                    if (step == 1) "Step 1 of 2: Personal Details" else "Step 2 of 2: Set Password",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))

        if (step == 1) {
            // STEP 1: Personal Details
            FormSection("Basic Information") {
                FormField("Full name *", name, { name = it }, err1("name"))
                FormField("Age *", age, { age = it.filter(Char::isDigit).take(3) }, err1("age"), KeyboardType.Number)
                DropdownField("Sex *", sex, SexOptions, { sex = it }, err1("sex"))
                DropdownField("Blood group", bloodType, BloodTypes, { bloodType = it })
                FormField("Allergies (comma separated)", allergies, { allergies = it })
            }
            FormSection("Contact Information") {
                FormField("Phone number *", phone, { phone = it.filter(Char::isDigit).take(10) }, err1("phone"), KeyboardType.Phone)
                FormField("Email", email, { email = it.trim() }, err1("email"), KeyboardType.Email)
                FormField("Address", address, { address = it }, singleLine = false)
            }
            FormSection("Emergency Contact") {
                FormField("Contact name", ecName, { ecName = it })
                FormField("Contact phone", ecPhone, { ecPhone = it.filter(Char::isDigit).take(10) }, keyboard = KeyboardType.Phone)
            }
            FormSection("Identity & Insurance (optional)") {
                FormField("Aadhaar number", aadhaar, { aadhaar = it.filter(Char::isDigit).take(12) }, err1("aadhaar"), KeyboardType.Number)
                FormField("PAN number", pan, { pan = it.uppercase().take(10) }, err1("pan"))
                FormField("Insurance type", insType, { insType = it })
                FormField("Insurance company", insCompany, { insCompany = it })
                FormField("Insurance TPA", insTpa, { insTpa = it })
            }

            Button(
                onClick = {
                    submittedStep1 = true
                    if (errorsStep1.isNotEmpty()) {
                        showSnackbar("Please fix the highlighted fields")
                        return@Button
                    }
                    step = 2
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Next: Set Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            // STEP 2: Password Setup
            FormSection("Account Password") {
                Text(
                    "Create a password to secure your patient account. You will use your Name or Registration ID with this password to log in.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                PasswordField(
                    label = "Password *",
                    value = password,
                    onChange = { password = it },
                    isVisible = passwordVisible,
                    onToggleVisibility = { passwordVisible = !passwordVisible },
                    error = err2("password")
                )

                PasswordField(
                    label = "Confirm Password *",
                    value = confirmPassword,
                    onChange = { confirmPassword = it },
                    isVisible = confirmPasswordVisible,
                    onToggleVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                    error = err2("confirmPassword")
                )
            }

            Spacer(Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Green)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Back", fontSize = 16.sp, color = Navy)
                    }

                    Button(
                        onClick = {
                            submittedStep2 = true
                            if (errorsStep2.isNotEmpty()) {
                                showSnackbar(errorsStep2.values.first())
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
                                            deviceId = getDeviceId(),
                                            password = password,
                                            confirmPassword = confirmPassword
                                        )
                                    )
                                    if (res != null) {
                                        onRegistered(res)
                                    } else {
                                        showSnackbar(message ?: "Registration failed")
                                    }
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                        modifier = Modifier.weight(2f).height(50.dp),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            "Your patient account is ready! Use your Name or Registration ID to log in.",
            color = Color.Gray, fontSize = 14.sp
        )
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(16.dp)) {
                CredentialRow("Username (Name)", credentials.username) {
                    clipboard.setText(AnnotatedString(credentials.username)); showSnackbar("Username copied")
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                CredentialRow("Registration ID", credentials.registrationId) {
                    clipboard.setText(AnnotatedString(credentials.registrationId)); showSnackbar("Registration ID copied")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (isLoading) {
            CircularProgressIndicator(color = Green)
        } else {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            // Try logging in with the username/name first, fallback to registrationId if needed
                            var patient = api.login(credentials.username, credentials.password)
                            if (patient == null) {
                                patient = api.login(credentials.registrationId, credentials.password)
                            }
                            if (patient != null) {
                                sessionManager.savePatient(patient)
                                registerFcmTokenAfterLogin(patient.id)
                                onContinue()
                            } else {
                                showSnackbar("Could not sign in automatically. Please log in manually.")
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp)
            ) { Text("Sign In Now", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CredentialRow(label: String, value: String, onCopy: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Navy)
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
private fun PasswordField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
    error: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = onToggleVisibility) {
                Icon(imageVector = image, contentDescription = "Toggle password visibility")
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
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
