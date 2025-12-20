package com.org.patientchakravue.platform

actual fun currentEpochSeconds(): Long = System.currentTimeMillis() / 1000L

