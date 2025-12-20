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
    val expected: Int
)

@Serializable
data class SubmissionDetails(
    @SerialName("image_file_id") val imageId: String? = null,
    @SerialName("pain_scale") val pain: Int = 0,
    @SerialName("vision_blur") val vision: Int = 0,
    @SerialName("redness") val redness: Int = 0,
    @SerialName("watering") val watering: Int = 0,
    @SerialName("itching") val itching: Int = 0,
    @SerialName("discharge") val discharge: Int = 0,
    val comments: String? = null
)

@Serializable
data class DoctorNote(
    @SerialName("note_text") val noteText: String? = null,
    val timestamp: String? = null,
    @SerialName("submission_id") val submissionId: String? = null,
    @SerialName("submission_details") val details: SubmissionDetails? = null
)

@Serializable
data class ChatMessage(
    val id: String,
    val sender: String,
    val type: String,
    val content: String? = null,
    val timestamp: String? = null,
    @SerialName("image_file_id") val imageId: String? = null,
    val symptoms: Map<String, Int>? = null
)

@Serializable
data class DoseItem(
    val id: String,
    val medicine_name: String,
    val dose_label: String,
    val scheduled_time: String,
    val scheduled_iso: String,
    val taken: Boolean
)

