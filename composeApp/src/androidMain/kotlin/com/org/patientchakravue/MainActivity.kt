package com.org.patientchakravue

import android.os.Bundle
import android.graphics.Color
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.org.patientchakravue.app.App
import com.org.patientchakravue.data.ApiRepository
import com.org.patientchakravue.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Make content draw behind system bars but use safe insets in Compose (WindowInsets.safeDrawing)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Set status bar background to white and request dark icons
        window.statusBarColor = Color.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        // Initialize FCM token registration
        initializeFirebaseMessaging()

        setContent {
            App()
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
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}