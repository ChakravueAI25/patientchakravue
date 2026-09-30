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
        upsertAccount(patient)
    }

    // --- Saved accounts (Gmail-style account switcher) ---
    // The app authenticates by patient id only (no token), so a saved account is just
    // the patient record; no password is stored on the device.
    fun getSavedAccounts(): List<Patient> {
        val json = settings.getStringOrNull("saved_accounts") ?: return emptyList()
        return try {
            Json.decodeFromString<List<Patient>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun putAccounts(list: List<Patient>) =
        settings.putString("saved_accounts", Json.encodeToString(list))

    private fun upsertAccount(patient: Patient) =
        putAccounts(getSavedAccounts().filter { it.id != patient.id } + patient)

    fun removeAccount(patientId: String) =
        putAccounts(getSavedAccounts().filter { it.id != patientId })

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

