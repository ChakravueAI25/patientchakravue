package com.org.patientchakravue.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
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
 *
 * IMPORTANT: This service handles notifications in ALL app states:
 * - App killed
 * - App in background
 * - App in foreground
 */
class FirebaseService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseService"

        // Channel IDs must match those created in MainActivity
        const val CHANNEL_MEDICINE = "medicine_channel"
        const val CHANNEL_CALLS = "call_channel_id"
        const val CHANNEL_DEFAULT = "default_channel_id"
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on first app launch and token refresh.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        // Sync token to backend if user is logged in
        val session = SessionManager()
        val patient = session.getPatient()
        if (patient != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val success = ApiRepository().registerFcmToken(patient.id, token)
                Log.d(TAG, "Token sync ${if (success) "successful" else "failed"}")
            }
        }
    }

    /**
     * Called when a message is received.
     * ALWAYS shows notification - regardless of app state (foreground/background/killed)
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received: ${message.data}")

        val data = message.data
        val type = data["type"]

        // Get title/body from notification payload OR data payload
        val title = message.notification?.title ?: data["title"] ?: "Notification"
        val body = message.notification?.body ?: data["body"] ?: "You have a new update"

        when (type) {
            // Handle Incoming Video Call (High Priority)
            "incoming_call" -> {
                val channelName = data["channel_name"] ?: ""
                val doctorId = data["doctor_id"] ?: ""
                showIncomingCallNotification(channelName, doctorId)
            }

            // Handle Medicine Reminder - ALWAYS show notification + refresh UI
            "medicine_reminder", "medicine_dose" -> {
                Log.d(TAG, "Medicine reminder received")
                // Trigger UI refresh instantly
                DoseRefreshBus.emit()
                // ALWAYS show notification (even in foreground)
                showNotification(title, body, CHANNEL_MEDICINE)
            }

            // Doctor notification
            "doctor_notification" -> {
                DoseRefreshBus.emit()
                showNotification(title, body, CHANNEL_DEFAULT)
            }

            // Default: Any other message
            else -> {
                DoseRefreshBus.emit()
                showNotification(title, body, CHANNEL_DEFAULT)
            }
        }
    }

    /**
     * Show a standard notification.
     * Works in ALL app states: foreground, background, and killed.
     */
    private fun showNotification(
        title: String,
        message: String,
        channelId: String
    ) {
        Log.d(TAG, "Showing notification: $title - $message on channel $channelId")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists (fallback for when MainActivity hasn't run yet)
        ensureChannelExists(notificationManager, channelId)

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        // Set priority based on channel
        when (channelId) {
            CHANNEL_MEDICINE -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
            }
            CHANNEL_CALLS -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
            }
            else -> {
                notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            }
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    /**
     * Show incoming call notification with full-screen intent.
     */
    private fun showIncomingCallNotification(channelName: String, doctorId: String) {
        Log.d(TAG, "Incoming call notification: channel=$channelName, doctor=$doctorId")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ensure channel exists
        ensureChannelExists(notificationManager, CHANNEL_CALLS)

        // Intent to open App
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("target_screen", "call_screen")
            putExtra("channel_name", channelName)
            putExtra("doctor_id", doctorId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Incoming Doctor Call")
            .setContentText("Dr. Chakra is calling...")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setFullScreenIntent(pendingIntent, true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    /**
     * Ensure notification channel exists.
     * Fallback for when app is killed and MainActivity.createNotificationChannels() hasn't run.
     */
    private fun ensureChannelExists(manager: NotificationManager, channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(channelId) == null) {
                val (name, importance, description) = when (channelId) {
                    CHANNEL_MEDICINE -> Triple("Medicine Reminders", NotificationManager.IMPORTANCE_HIGH, "Medicine dose reminders")
                    CHANNEL_CALLS -> Triple("Incoming Calls", NotificationManager.IMPORTANCE_HIGH, "Incoming video calls")
                    else -> Triple("General Notifications", NotificationManager.IMPORTANCE_DEFAULT, "General notifications")
                }

                val channel = NotificationChannel(channelId, name, importance).apply {
                    this.description = description
                    enableVibration(true)
                    if (channelId == CHANNEL_CALLS) {
                        setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE), null)
                    }
                }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Created notification channel: $channelId")
            }
        }
    }
}
