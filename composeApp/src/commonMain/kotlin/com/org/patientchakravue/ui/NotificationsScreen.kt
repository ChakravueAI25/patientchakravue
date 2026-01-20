package com.org.patientchakravue.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.DoctorNote
import com.org.patientchakravue.model.DoctorThread
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.ui.language.localizedString

@Composable
fun NotificationsScreen(
    patient: Patient,
    onNavigateToChat: (doctorId: String, doctorName: String, submissionIds: List<String>) -> Unit, // Navigate with full doctor thread data
    bottomBar: @Composable () -> Unit
) {
    val api = remember { ApiRepository() }
    var messages by remember { mutableStateOf<List<DoctorNote>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        messages = api.getMessages(patient.id)
        isLoading = false
    }

    Scaffold(bottomBar = bottomBar) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else {
            // Group messages by doctor_id to show unique doctor threads
            val doctorThreads = remember(messages) {
                messages
                    .filter { it.submissionId != null }
                    .groupBy { it.doctorId ?: patient.doctorId ?: "unknown" }
                    .map { (doctorId, notes) ->
                        // Get the latest message from this doctor conversation
                        val latestNote = notes.maxByOrNull { it.timestamp ?: "" }
                        // Collect all unique submission IDs for this doctor
                        val submissionIds = notes.mapNotNull { it.submissionId }.distinct()

                        DoctorThread(
                            doctorId = doctorId,
                            doctorName = latestNote?.doctorName ?: latestNote?.sender ?: "Dr. Chakra",
                            latestMessage = latestNote?.noteText ?: "View conversation...",
                            latestTimestamp = latestNote?.timestamp ?: "",
                            submissionIds = submissionIds,
                            unreadCount = 0
                        )
                    }
                    .sortedByDescending { it.latestTimestamp }
            }

            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            localizedString("hospital_updates"),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A3B5D)
                        )
                        Text(
                            localizedString("view_messages"),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (doctorThreads.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                localizedString("no_messages"),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(doctorThreads) { thread ->
                        DoctorThreadItem(
                            thread = thread,
                            onClick = {
                                // Navigate to chat with doctorId, name, and ALL submissionIds
                                onNavigateToChat(
                                    thread.doctorId,
                                    thread.doctorName,
                                    thread.submissionIds
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DoctorThreadItem(
    thread: DoctorThread,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Doctor Avatar
            Surface(
                shape = CircleShape,
                color = Color(0xFF1976D2),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = thread.doctorName.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Doctor Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.doctorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A3B5D)
                    )
                    Text(
                        text = formatTime(thread.latestTimestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = thread.latestMessage,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Submission count indicator
                if (thread.submissionIds.size > 1) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${thread.submissionIds.size} reports",
                        fontSize = 12.sp,
                        color = Color(0xFF1976D2)
                    )
                }
            }

            // Chat icon
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = "Chat",
                tint = Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Helper function to format timestamp to readable time
private fun formatTime(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        // Extract date part (YYYY-MM-DD)
        val datePart = timestamp.substring(0, 10)
        val timePart = timestamp.substring(11, 16)
        "$datePart $timePart"
    } catch (e: Exception) {
        timestamp.take(16)
    }
}
