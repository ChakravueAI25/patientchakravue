package com.org.patientchakravue.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vision") }
            )
        }
    ) { paddingValues ->
        Text("Vision details go here", modifier = Modifier.padding(paddingValues).padding(16.dp))
    }
}