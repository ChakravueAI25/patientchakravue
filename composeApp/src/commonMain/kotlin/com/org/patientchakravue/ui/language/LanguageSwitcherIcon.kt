package com.org.patientchakravue.ui.language

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A reusable language switcher component that displays a translation icon (文 ↔ A).
 * When clicked, it shows a dropdown menu with available languages.
 *
 * @param tint The color of the icon (used for arrows and 文 character)
 * @param modifier Optional modifier for the component
 */
@Composable
fun LanguageSwitcherIcon(
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    val languageManager = LocalLanguageManager.current
    var expanded by remember { mutableStateOf(false) }

    // Gold/Yellow color for the "A" letter
    val primaryColor = Color(0xFF4CAF50)

    Box(modifier = modifier) {
        // Custom Translation Icon (文 ↔ A with circular arrows)
        Canvas(
            modifier = Modifier
                .size(32.dp)
                .clickable { expanded = true }
        ) {
            val w = size.width
            val h = size.height
            val centerX = w / 2
            val centerY = h / 2
            val radius = w * 0.42f

            // Draw circular arrows (dark color matching tint)
            drawCircularArrows(
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                color = tint,
                strokeWidth = w * 0.06f
            )

            // Draw "文" character on the left (dark color)
            drawWenCharacter(
                centerX = centerX - w * 0.15f,
                centerY = centerY - h * 0.05f,
                size = w * 0.32f,
                color = tint,
                strokeWidth = w * 0.055f
            )

            // Draw "A" character on the right (gold color)
            drawLetterA(
                centerX = centerX + w * 0.18f,
                centerY = centerY + h * 0.12f,
                size = w * 0.28f,
                color = primaryColor,
                strokeWidth = w * 0.055f
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = { Text(language.displayName) },
                    onClick = {
                        expanded = false
                        languageManager.changeLanguage(language)
                    },
                    trailingIcon = {
                        // Show a checkmark next to the currently selected language
                        if (language == languageManager.currentLanguage) {
                            Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Draws two curved arrows forming a circular exchange pattern
 */
private fun DrawScope.drawCircularArrows(
    centerX: Float,
    centerY: Float,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    val arrowHeadSize = radius * 0.25f

    // Top-right arrow (going clockwise from top to right)
    val topArrowPath = Path().apply {
        // Arc from ~135° to ~45° (top-left to top-right)
        val startAngle = -135.0
        val sweepAngle = 90.0

        // Draw arc points manually
        val steps = 20
        var isFirst = true
        for (i in 0..steps) {
            val angle = (startAngle + (sweepAngle * i / steps)) * PI / 180.0
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (isFirst) {
                moveTo(x, y)
                isFirst = false
            } else {
                lineTo(x, y)
            }
        }
    }

    drawPath(
        path = topArrowPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Arrow head at end of top arrow (pointing down-right)
    val topArrowEndAngle = -45.0 * PI / 180.0
    val topArrowEndX = centerX + radius * cos(topArrowEndAngle).toFloat()
    val topArrowEndY = centerY + radius * sin(topArrowEndAngle).toFloat()

    val topArrowHead = Path().apply {
        moveTo(topArrowEndX, topArrowEndY)
        lineTo(topArrowEndX - arrowHeadSize * 0.7f, topArrowEndY - arrowHeadSize * 0.3f)
        moveTo(topArrowEndX, topArrowEndY)
        lineTo(topArrowEndX - arrowHeadSize * 0.3f, topArrowEndY + arrowHeadSize * 0.7f)
    }

    drawPath(
        path = topArrowHead,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Bottom-left arrow (going counter-clockwise from bottom to left)
    val bottomArrowPath = Path().apply {
        val startAngle = 45.0
        val sweepAngle = 90.0

        val steps = 20
        var isFirst = true
        for (i in 0..steps) {
            val angle = (startAngle + (sweepAngle * i / steps)) * PI / 180.0
            val x = centerX + radius * cos(angle).toFloat()
            val y = centerY + radius * sin(angle).toFloat()
            if (isFirst) {
                moveTo(x, y)
                isFirst = false
            } else {
                lineTo(x, y)
            }
        }
    }

    drawPath(
        path = bottomArrowPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Arrow head at end of bottom arrow (pointing up-left)
    val bottomArrowEndAngle = 135.0 * PI / 180.0
    val bottomArrowEndX = centerX + radius * cos(bottomArrowEndAngle).toFloat()
    val bottomArrowEndY = centerY + radius * sin(bottomArrowEndAngle).toFloat()

    val bottomArrowHead = Path().apply {
        moveTo(bottomArrowEndX, bottomArrowEndY)
        lineTo(bottomArrowEndX + arrowHeadSize * 0.7f, bottomArrowEndY + arrowHeadSize * 0.3f)
        moveTo(bottomArrowEndX, bottomArrowEndY)
        lineTo(bottomArrowEndX + arrowHeadSize * 0.3f, bottomArrowEndY - arrowHeadSize * 0.7f)
    }

    drawPath(
        path = bottomArrowHead,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

/**
 * Draws a simplified Chinese character "文" (meaning text/language)
 */
private fun DrawScope.drawWenCharacter(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color,
    strokeWidth: Float
) {
    val halfSize = size / 2

    // Top horizontal stroke (shorter)
    drawLine(
        color = color,
        start = Offset(centerX - halfSize * 0.6f, centerY - halfSize * 0.7f),
        end = Offset(centerX + halfSize * 0.6f, centerY - halfSize * 0.7f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Left diagonal stroke (from top-center going down-left)
    drawLine(
        color = color,
        start = Offset(centerX, centerY - halfSize * 0.3f),
        end = Offset(centerX - halfSize * 0.8f, centerY + halfSize * 0.8f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Right diagonal stroke (from top-center going down-right)
    drawLine(
        color = color,
        start = Offset(centerX, centerY - halfSize * 0.3f),
        end = Offset(centerX + halfSize * 0.8f, centerY + halfSize * 0.8f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Middle horizontal stroke (crossing the diagonals)
    drawLine(
        color = color,
        start = Offset(centerX - halfSize * 0.5f, centerY + halfSize * 0.15f),
        end = Offset(centerX + halfSize * 0.5f, centerY + halfSize * 0.15f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

/**
 * Draws the letter "A"
 */
private fun DrawScope.drawLetterA(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color,
    strokeWidth: Float
) {
    val halfSize = size / 2

    // Left diagonal of A
    drawLine(
        color = color,
        start = Offset(centerX, centerY - halfSize),
        end = Offset(centerX - halfSize * 0.6f, centerY + halfSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Right diagonal of A
    drawLine(
        color = color,
        start = Offset(centerX, centerY - halfSize),
        end = Offset(centerX + halfSize * 0.6f, centerY + halfSize),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    // Horizontal bar of A
    drawLine(
        color = color,
        start = Offset(centerX - halfSize * 0.35f, centerY + halfSize * 0.2f),
        end = Offset(centerX + halfSize * 0.35f, centerY + halfSize * 0.2f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}
