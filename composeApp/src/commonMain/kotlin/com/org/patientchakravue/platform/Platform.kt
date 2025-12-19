package com.org.patientchakravue.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

