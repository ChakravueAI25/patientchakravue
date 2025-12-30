package com.org.patientchakravue.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    submissionId: String,
    onBack: () -> Unit
) {
    val api = remember { ApiRepository() }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    // Get current language to trigger recomposition when it changes
    val currentLang = LocalLanguageManager.current.currentLanguage

    // AUTO-REFRESH: Poll every 3 seconds to see new Doctor replies
    LaunchedEffect(submissionId) {
        while (isActive) {
            val newMessages = api.getConversation(submissionId)
            // If we have new messages, update and scroll
            if (newMessages.size != messages.size || isLoading) {
                messages = newMessages
                isLoading = false
                if (messages.isNotEmpty()) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
            delay(3000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.LightGray,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("Dr", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Dr. Chakra", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(localizedString("dr_online"), fontSize = 12.sp, color = Color.Green)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF0F2F5))
            )
        },
        bottomBar = {
            // Placeholder Input Area (Visual only for now)
            ChatInputArea()
        },
        containerColor = Color(0xFFEFE7DE) // WhatsApp-like beige background
    ) { padding ->

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    if (msg.sender == "patient") {
                        if (msg.type == "report") {
                            ReportBubble(msg)
                        } else {
                            // Future: Standard patient text messages
                            TextBubble(msg, isUser = true)
                        }
                    } else {
                        // Doctor or System
                        TextBubble(msg, isUser = false)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportBubble(msg: ChatMessage) {
    // FIX: Changed "/files/" to "/images/" to match Patient Backend main.py
    val imageUrl = if (msg.imageId != null) {
        "${ApiRepository.BASE_URL}/images/${msg.imageId}"
    } else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD9FDD3)), // WhatsApp Green
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.width(280.dp) // Fixed width for report card
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // 1. Image Header
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Eye Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // 2. Symptoms List
                if (msg.symptoms != null) {
                    Text(localizedString("report_summary"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF075E54))
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Black.copy(alpha = 0.1f))

                    msg.symptoms.entries.filter { it.value > 0 }.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key, fontSize = 12.sp, color = Color.DarkGray)
                            // Color code severity
                            val color = if (value >= 7) Color.Red else if (value >= 4) Color(0xFFD89F2B) else Color.Black
                            Text("$value/10", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // 3. User Comment
                if (!msg.content.isNullOrBlank()) {
                    Text(msg.content, fontSize = 14.sp, color = Color.Black)
                }

                // 4. Time
                Text(
                    text = formatTimestamp(msg.timestamp),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TextBubble(msg: ChatMessage, isUser: Boolean) {
    val align = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) Color(0xFFD9FDD3) else Color.White
    val shape = if (isUser)
        RoundedCornerShape(topStart = 12.dp, topEnd = 0.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
    else
        RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Card(
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = bg),
            elevation = CardDefaults.cardElevation(1.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = msg.content ?: "",
                    fontSize = 15.sp,
                    color = Color.Black
                )
                Text(
                    text = formatTimestamp(msg.timestamp),
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputArea() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F2F5))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(
            value = "",
            onValueChange = {},
            placeholder = { Text(localizedString("message_placeholder"), fontSize = 14.sp) },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.width(8.dp))
        FloatingActionButton(
            onClick = {},
            containerColor = Color(0xFF075E54), // WhatsApp Dark Green
            contentColor = Color.White,
            modifier = Modifier.size(48.dp),
            shape = CircleShape
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
        }
    }
}

// Helper to format ISO timestamp (Simple version)
fun formatTimestamp(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    // Take HH:MM from ISO string "2025-12-20T09:47:55..." -> Index 11 to 16
    return try {
        iso.substring(11, 16)
    } catch (e: Exception) {
        ""
    }
}

