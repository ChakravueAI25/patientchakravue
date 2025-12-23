package com.org.patientchakravue.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.org.patientchakravue.dose.DoseRefreshBus

/**
 * Android-specific service to handle Firebase Cloud Messages.
 * Its only job is to signal that backend data has changed.
 */
class FirebaseService : FirebaseMessagingService() {
    /**
     * Called when a message is received.
     *
     * @param message The remote message that was received.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        // On ANY message, assume backend state has changed and trigger a refresh.
        // This is a simple and robust way to keep the UI in sync.
        DoseRefreshBus.emit()
    }
}
