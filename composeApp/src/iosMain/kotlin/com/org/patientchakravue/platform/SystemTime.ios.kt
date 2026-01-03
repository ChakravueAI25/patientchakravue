package com.org.patientchakravue.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS-specific implementation for getting the current system time as epoch seconds.
 */
actual fun currentEpochSeconds(): Long =
    NSDate().timeIntervalSince1970().toLong()

