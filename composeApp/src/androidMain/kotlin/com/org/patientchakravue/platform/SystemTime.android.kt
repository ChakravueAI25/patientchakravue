package com.org.patientchakravue.platform

/**
 * Android-specific implementation for getting the current system time as epoch seconds.
 */
actual fun currentEpochSeconds(): Long =
    System.currentTimeMillis() / 1000
