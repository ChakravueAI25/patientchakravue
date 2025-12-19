package com.org.patientchakravue.data

import com.org.patientchakravue.model.Patient
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SessionManager {
    private val settings: Settings = Settings()

    fun savePatient(patient: Patient) {
        val json = Json.encodeToString(patient)
        settings.putString("patient_data", json)
    }

    fun getPatient(): Patient? {
        val json = settings.getStringOrNull("patient_data") ?: return null
        return try {
            Json.decodeFromString<Patient>(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearSession() {
        settings.clear()
    }
}

