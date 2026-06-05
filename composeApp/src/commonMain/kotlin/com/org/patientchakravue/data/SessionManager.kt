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

    // --- Terms & Conditions acceptance (per-user, versioned) ---
    fun hasAcceptedTerms(patientId: String, version: Int): Boolean =
        patientId.isNotEmpty() && settings.getInt("terms_accepted_$patientId", 0) >= version

    fun setTermsAccepted(patientId: String, version: Int) {
        if (patientId.isNotEmpty()) settings.putInt("terms_accepted_$patientId", version)
    }

    fun clearSession() {
        // Remove only the session; keep per-user consent flags so a returning user
        // isn't re-prompted. (patient_data is the only session key.)
        settings.remove("patient_data")
    }
}

