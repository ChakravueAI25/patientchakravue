package com.org.patientchakravue.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.SerialName

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class Patient(
    @SerialName("_id") val mongoId: String,
    val name: String? = null,
    val phone: String? = null,
    val age: String? = null,
    val sex: String? = null,
    val email: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    val visits: List<Visit>? = null,
    val prescription: JsonElement? = null
) {
    val id: String get() = mongoId
}

@Serializable
data class Visit(
    val stages: Stages? = null
)

@Serializable
data class Stages(
    val doctor: DoctorStage? = null
)

@Serializable
data class DoctorStage(
    val data: DoctorData? = null
)

@Serializable
data class DoctorData(
    val prescription: JsonElement? = null
)

// --- NEW TYPED MODELS TO FIX SERIALIZATION ERROR ---

@Serializable
data class AdherenceResponse(
    val weekly: List<AdherenceDay> = emptyList()
)

@Serializable
data class AdherenceDay(
    val day: String,
    val taken: Int,
    val expected: Int,
    val date: String? = null // backend returns date for each day
)

@Serializable
data class DoctorNote(
    @SerialName("note_text") val noteText: String? = null,
    val timestamp: String? = null
)

// --- Dose models added to mirror backend /patients/{patient_id}/today-doses ---
@Serializable
data class DoseItem(
    @SerialName("_id") val id: String,
    val patient_id: String,
    val medicine_name: String,
    val date: String,
    val dose_label: String,
    val scheduled_time: String,
    val scheduled_iso: String,
    val taken: Boolean,
    val taken_at: String? = null,
    val notified: Boolean = false,
    val created_at: String,
    val notification_id: String? = null,
    val doctor_id: String? = null
)

@Serializable
data class NextDoseResponse(
    val next: DoseItem? = null
)
