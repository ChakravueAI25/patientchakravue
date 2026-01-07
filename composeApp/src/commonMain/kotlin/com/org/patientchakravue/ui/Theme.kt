package com.org.patientchakravue.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),
    secondary = Color(0xFF1976D2),
    tertiary = Color(0xFF4CAF50),
    // Keep transparent so surfaces don't paint a solid background over our gradient
    background = Color.Transparent,
    surface = Color.Transparent,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // Wrap MaterialTheme but draw a locked gradient using explicit colors so Material can't tone
    MaterialTheme(colorScheme = LightColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBFE6D3), // brand green – ~3x darker
                            Color(0xFFC6D9F2)  // brand blue – ~3x darker
                        )
                    )
                )
        ) {
            content()
        }
    }
}


@Composable
fun AppBackground(content: @Composable () -> Unit) {
    // AppBackground remains a passthrough — AppTheme owns the global gradient
    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }
}

// PrescriptionCard: translucent card that blends with the global background (no elevation, no borders)
@Composable
fun PrescriptionCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.92f),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Column(content = content)
    }
}
