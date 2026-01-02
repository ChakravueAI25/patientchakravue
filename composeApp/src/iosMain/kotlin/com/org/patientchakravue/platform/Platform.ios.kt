package com.org.patientchakravue.platform

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

/**
 * iOS implementation: No-op for now.
 * iOS uses APNS which would be configured separately.
 */
actual fun registerFcmTokenAfterLogin(patientId: String) {
    // iOS push notifications would be handled via APNS
    // This is a placeholder for future iOS push notification implementation
    println("iOS FCM registration placeholder for patient: $patientId")
}
