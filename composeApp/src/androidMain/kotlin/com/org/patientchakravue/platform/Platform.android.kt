package com.org.patientchakravue.platform

import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.org.patientchakravue.data.ApiRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
