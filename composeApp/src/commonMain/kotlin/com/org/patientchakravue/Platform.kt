package com.org.patientchakravue

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform