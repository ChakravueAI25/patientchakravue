package com.org.patientchakravue.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.dose.DoseRefreshBus
import com.org.patientchakravue.model.GraphData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceGraphScreen(onBack: () -> Unit) {
    val sessionManager = remember { SessionManager() }
    val api = remember { ApiRepository() }
    val patient = sessionManager.getPatient()

    // Get current language to trigger recomposition when it changes
    val currentLang = LocalLanguageManager.current.currentLanguage

    // State
    var selectedView by remember { mutableStateOf("day") } // day, week, medicine
    var graphData by remember { mutableStateOf<GraphData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data Logic
    suspend fun loadData() {
        if (patient == null) return
        isLoading = true
        val data = api.getGraphData(patient.id, selectedView)
        graphData = data
        isLoading = false
    }

    // Initial Load & Refresh Listener
    LaunchedEffect(selectedView) { loadData() }
    LaunchedEffect(Unit) {
        DoseRefreshBus.events.collect { loadData() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedString("adherence_title")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F5F5))
            )
        },
        containerColor = Color(0xFFFAFAFA)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 1. View Selectors
            Text(localizedString("view_by"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ViewFilterChip(localizedString("view_day"), selectedView == "day") { selectedView = "day" }
                ViewFilterChip(localizedString("view_week"), selectedView == "week") { selectedView = "week" }
                ViewFilterChip(localizedString("view_medicine"), selectedView == "medicine") { selectedView = "medicine" }
            }

            Spacer(Modifier.height(24.dp))

            // 2. The Graph Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF4CAF50))
                    } else if (graphData != null) {
                        Column {
                            Text(
                                text = graphData!!.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A3B5D)
                            )
                            Spacer(Modifier.height(24.dp))

                            // Render the Native Bar Chart
                            SimpleBarChart(
                                xAxisLabels = graphData!!.xAxis,
                                yValues = graphData!!.yAxis,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        }
                    } else {
                        Text(localizedString("no_data"), modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // 3. Summary Text
            if (graphData != null) {
                val totalTaken = graphData!!.yAxis.sum()
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(localizedString("summary_title"), fontWeight = FontWeight.Bold)
                        Text("${localizedString("summary_total")} $totalTaken")
                        if (selectedView == "day") {
                            Text(localizedString("desc_day"), fontSize = 12.sp, color = Color.Gray)
                        } else if (selectedView == "week") {
                            Text(localizedString("desc_week"), fontSize = 12.sp, color = Color.Gray)
                        } else {
                            Text(localizedString("desc_medicine"), fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF4CAF50),
            selectedLabelColor = Color.White
        )
    )
}

/**
 * A Custom Bar Chart using Compose Canvas (Platform Independent).
 * Automatically scales Y-axis based on max value.
 */
@Composable
fun SimpleBarChart(
    xAxisLabels: List<String>,
    yValues: List<Int>,
    modifier: Modifier = Modifier
) {
    val maxValue = (yValues.maxOrNull() ?: 1).coerceAtLeast(1)
    val barColor = Color(0xFF42A5F5)
    val textMeasurer = rememberTextMeasurer()

    // Animation for bar heights
    val animatedProgress by animateFloatAsState(targetValue = 1f, label = "bar")

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        if (xAxisLabels.isEmpty()) return@Canvas

        val barSpace = w / xAxisLabels.size
        val barWidth = barSpace * 0.5f
        val maxBarHeight = h * 0.75f
        val bottomPadding = 50f

        // Draw Bars
        yValues.forEachIndexed { index, value ->
            val barHeight = if (maxValue > 0) {
                (value.toFloat() / maxValue.toFloat()) * maxBarHeight * animatedProgress
            } else 0f

            val x = (index * barSpace) + (barSpace / 2) - (barWidth / 2)
            val y = h - barHeight - bottomPadding

            // Bar Rectangle
            drawRect(
                color = if (value > 0) barColor else Color.LightGray.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Value Label (on top of bar) - Using drawText from Compose
            if (value > 0) {
                val valueText = value.toString()
                val textLayoutResult = textMeasurer.measure(
                    text = valueText,
                    style = TextStyle(fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x + barWidth / 2 - textLayoutResult.size.width / 2,
                        y - textLayoutResult.size.height - 4
                    )
                )
            }

            // X-Axis Label (bottom) - Split into two lines if exceeds 10 characters
            val label = xAxisLabels.getOrElse(index) { "" }
            val maxChars = 10
            val (line1, line2) = if (label.length > maxChars) {
                label.take(maxChars) to label.drop(maxChars)
            } else {
                label to ""
            }

            val line1LayoutResult = textMeasurer.measure(
                text = line1,
                style = TextStyle(fontSize = 10.sp, color = Color.DarkGray)
            )
            drawText(
                textLayoutResult = line1LayoutResult,
                topLeft = Offset(
                    x + barWidth / 2 - line1LayoutResult.size.width / 2,
                    h - bottomPadding + 8
                )
            )

            // Draw second line if label was split
            if (line2.isNotEmpty()) {
                val line2LayoutResult = textMeasurer.measure(
                    text = line2,
                    style = TextStyle(fontSize = 10.sp, color = Color.DarkGray)
                )
                drawText(
                    textLayoutResult = line2LayoutResult,
                    topLeft = Offset(
                        x + barWidth / 2 - line2LayoutResult.size.width / 2,
                        h - bottomPadding + 8 + line1LayoutResult.size.height + 2
                    )
                )
            }
        }

        // Base Line
        drawLine(
            color = Color.Gray,
            start = Offset(0f, h - bottomPadding),
            end = Offset(w, h - bottomPadding),
            strokeWidth = 2f
        )
    }
}
