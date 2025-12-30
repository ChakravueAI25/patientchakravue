package com.org.patientchakravue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.DoctorNote
import com.org.patientchakravue.model.Patient

@Composable
fun NotificationsScreen(
    patient: Patient,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onNoteClick: (DoctorNote) -> Unit
) {
    val api = remember { ApiRepository() }
    var notes by remember { mutableStateOf<List<DoctorNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Get current language to trigger recomposition when it changes
    val currentLang = LocalLanguageManager.current.currentLanguage

    // Fetch notifications on screen launch
    LaunchedEffect(Unit) {
        notes = api.getMessages(patient.id)
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
    ) {
        Text(
            text = localizedString("notifications_title"),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A3B5D)
        )
        Text(
            text = localizedString("notifications_subtitle"),
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(20.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (notes.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text(localizedString("no_notifications"), color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(notes) { note ->
                    NotificationItem(
                        note = note,
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationItem(note: DoctorNote, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = note.noteText ?: "System Message",
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = Color.Black
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note.timestamp ?: "Just now",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
