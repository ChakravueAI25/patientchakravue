package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.org.patientchakravue.app.LegalConfig

/**
 * One-time consent screen shown after login until the patient accepts the current
 * Terms version. Pure UI — the caller (App) persists acceptance in [onAccept].
 */
@Composable
fun TermsScreen(onAccept: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    var checked by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                "Terms & Conditions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(LegalConfig.SUMMARY, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { uriHandler.openUri(LegalConfig.TERMS_URL) }) {
                    Text("Read full Terms of Service", fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { uriHandler.openUri(LegalConfig.PRIVACY_URL) }) {
                    Text("Read Privacy Policy", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { checked = it })
                Spacer(Modifier.width(4.dp))
                Text(
                    "I have read and agree to the Terms & Conditions and Privacy Policy",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAccept,
                enabled = checked,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Agree & Continue", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
