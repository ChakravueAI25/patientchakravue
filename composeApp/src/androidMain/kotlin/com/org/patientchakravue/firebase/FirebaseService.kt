package com.org.patientchakravue.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.org.patientchakravue.MainActivity
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.dose.DoseRefreshBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Android-specific service to handle Firebase Cloud Messages.
 * Handles token refresh, incoming calls, and medicine reminders.
 */
class FirebaseService : FirebaseMessagingService() {

    /**
     * Called when a new FCM token is generated.
     * This happens on first app launch and token refresh.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Sync token to backend if user is logged in
        val session = SessionManager()
        val patient = session.getPatient()
        if (patient != null) {
            CoroutineScope(Dispatchers.IO).launch {
                ApiRepository().registerFcmToken(patient.id, token)
            }
        }
    }

    /**
     * Called when a message is received.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val type = data["type"]

        when (type) {
            // Handle Incoming Video Call (High Priority)
            "incoming_call" -> {
                val channelName = data["channel_name"] ?: ""
                val doctorId = data["doctor_id"] ?: ""
                showIncomingCallNotification(channelName, doctorId)
            }
            // Handle Medicine Reminder
            "medicine_reminder", "medicine_dose" -> {
                // Trigger UI refresh instantly
                DoseRefreshBus.emit()

                // Show notification if payload contains one
                message.notification?.let {
                    showStandardNotification(
                        it.title ?: "Medicine Reminder",
                        it.body ?: "Time to take your medication"
                    )
                }
            }
            // Default: Any other message triggers a data refresh
            else -> {
                DoseRefreshBus.emit()

                // Show notification if present
                message.notification?.let {
                    showStandardNotification(it.title ?: "Notification", it.body ?: "")
                }
            }
        }
    }

    private fun showIncomingCallNotification(channelName: String, doctorId: String) {
        val channelId = "call_channel_id" // Must match backend expectation
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel (Required for Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming video calls"
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open App
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("target_screen", "call_screen")
            putExtra("channel_name", channelName)
            putExtra("doctor_id", doctorId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Doctor Call")
            .setContentText("Dr. Chakra is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showStandardNotification(title: String, message: String) {
        val channelId = "default_channel_id"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
