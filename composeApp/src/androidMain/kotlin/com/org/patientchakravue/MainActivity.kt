package com.org.patientchakravue

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.org.patientchakravue.app.App
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import com.org.patientchakravue.platform.androidContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Runtime permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d("FCM", "Notification permission granted: $isGranted")
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize androidContext for platform-specific functions (like PDF download)
        androidContext = applicationContext

        // Make content draw behind system bars but use safe insets in Compose (WindowInsets.safeDrawing)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set status bar background to white and request dark icons
        window.statusBarColor = Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // STEP 1: Create ALL notification channels ONCE at app startup
        createNotificationChannels()

        // STEP 2: Request notification permission for Android 13+
        requestNotificationPermission()

        // STEP 3: Initialize FCM token registration
        initializeFirebaseMessaging()

        // Check if launched from a call notification
        val targetScreen = intent.getStringExtra("target_screen")
        val channelName = intent.getStringExtra("channel_name")
        val doctorId = intent.getStringExtra("doctor_id")

        setContent {
            App(
                initialCallData = if (targetScreen == "call_screen" && channelName != null) {
                    Pair(channelName, doctorId ?: "unknown")
                } else null
            )
        }
    }

    /**
     * Creates ALL notification channels at startup.
     * This ensures channels exist BEFORE any notification is sent.
     * Required for Android 8.0 (API 26) and above.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // 1. Medicine Reminders Channel (HIGH importance for visibility)
            val medicineChannel = NotificationChannel(
                "medicine_channel",
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Medicine dose reminders"
                enableVibration(true)
                setShowBadge(true)
            }

            // 2. Incoming Calls Channel (HIGH importance with ringtone)
            val callChannel = NotificationChannel(
                "call_channel_id",
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming doctor video calls"
                enableVibration(true)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    null
                )
            }

            // 3. Default/General Channel
            val generalChannel = NotificationChannel(
                "default_channel_id",
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications"
            }

            manager.createNotificationChannels(
                listOf(medicineChannel, callChannel, generalChannel)
            )
            Log.d("FCM", "Notification channels created")
        }
    }

    /**
     * Request POST_NOTIFICATIONS permission for Android 13+ (API 33+)
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("FCM", "Notification permission already granted")
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * Fetches FCM token and registers it with the backend if user is logged in.
     */
    private fun initializeFirebaseMessaging() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Token: $token")

            // Send to Backend if user is logged in
            val session = SessionManager()
            val patient = session.getPatient()
            if (patient != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    val success = ApiRepository().registerFcmToken(patient.id, token)
                    Log.d("FCM", "Token registration ${if (success) "successful" else "failed"}")
                }
            }
        }
    }

    companion object {
        /**
         * Static helper to register FCM token after login.
         * Call this from LoginScreen after successful authentication.
         */
        fun registerFcmTokenAfterLogin(patientId: String) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(Dispatchers.IO).launch {
                    val success = ApiRepository().registerFcmToken(patientId, token)
                    Log.d("FCM", "Post-login token registration ${if (success) "successful" else "failed"}")
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(initialCallData = null)
}
