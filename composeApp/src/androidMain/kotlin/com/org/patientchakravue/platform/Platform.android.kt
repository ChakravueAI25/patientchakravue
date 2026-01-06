package com.org.patientchakravue.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.org.patientchakravue.data.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// Context reference initialized from MainActivity
lateinit var androidContext: Context

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

/**
 * Android implementation: Fetches FCM token and registers with backend.
 * Called after successful login to ensure backend has fresh token.
 */
actual fun registerFcmTokenAfterLogin(patientId: String) {
    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
        Log.d("FCM", "Registering token after login for patient: $patientId")
        CoroutineScope(Dispatchers.IO).launch {
            val success = ApiRepository().registerFcmToken(patientId, token)
            Log.d("FCM", "Post-login token registration ${if (success) "successful" else "failed"}")
        }
    }.addOnFailureListener { e ->
        Log.e("FCM", "Failed to get FCM token after login", e)
    }
}

/**
 * Android implementation: Saves PDF file to Downloads and shows notification.
 * Uses MediaStore for Android 10+ (API 29+) and legacy method for older versions.
 */
actual fun saveAndNotifyDownload(fileName: String, data: ByteArray) {
    try {
        var savedPath: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (API 29+): Use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = androidContext.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(data)
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                savedPath = "Downloads/$fileName"
                Log.d("PDF_DOWNLOAD", "File saved via MediaStore: $savedPath")
            }
        } else {
            // Android 9 and below: Use legacy method
            val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsPath, fileName)
            FileOutputStream(file).use { it.write(data) }
            savedPath = file.absolutePath
            Log.d("PDF_DOWNLOAD", "File saved via legacy method: $savedPath")
        }

        // Show Local Notification
        val notificationManager = androidContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Downloads", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationText = if (savedPath != null) {
            "$fileName saved to Downloads folder"
        } else {
            "Failed to save $fileName"
        }

        val notification = NotificationCompat.Builder(androidContext, channelId)
            .setContentTitle("Download Complete")
            .setContentText(notificationText)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        Log.d("PDF_DOWNLOAD", "Download notification shown. File location: $savedPath")
    } catch (e: Exception) {
        Log.e("PDF_DOWNLOAD", "Error saving file: ${e.message}", e)
        e.printStackTrace()
    }
}

