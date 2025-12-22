package com.org.patientchakravue.platform

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import java.io.ByteArrayOutputStream

/**
 * Android implementation of bitmap capture for Amsler Grid drawings
 */
actual fun captureAmslerBitmap(paths: List<Path>, sizePx: Int): ByteArray {
    // Create an Android Bitmap
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    // 1. Draw White Background
    canvas.drawColor(android.graphics.Color.WHITE)

    // 2. Draw Grid (Programmatically using Android Canvas calls)
    val paintGrid = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    val step = sizePx / 20f
    for (i in 0..20) {
        val pos = step * i
        canvas.drawLine(pos, 0f, pos, sizePx.toFloat(), paintGrid)
        canvas.drawLine(0f, pos, sizePx.toFloat(), pos, paintGrid)
    }

    // Draw Center Dot
    val paintDot = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 15f, paintDot)

    // 3. Draw User Paths
    val paintPath = Paint().apply {
        color = android.graphics.Color.RED
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = 150
    }

    paths.forEach { composePath ->
        val androidPath = composePath.asAndroidPath()
        canvas.drawPath(androidPath, paintPath)
    }

    // 4. Compress to JPEG
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
    return stream.toByteArray()
}

