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
    val address: String? = null,
    @SerialName("doctor_id") val doctorId: String? = null,
    val visits: List<Visit>? = null,
    val prescription: JsonElement? = null,
    val bloodType: String? = null,
    val registrationId: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    @SerialName("recent_encounter") val recentEncounter: JsonElement? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val id: String get() = mongoId
}

@Serializable
data class Visit(
    val stages: Stages? = null,
    val date: String? = null,
    @SerialName("created_at") val createdAt: String? = null
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
    @SerialName("_id") val id: String,
    val medicine_name: String,
    val dose_label: String,
    val scheduled_time: String,
    val scheduled_iso: String,
    val taken: Boolean
)

@Serializable
data class VisionTestRecord(
    val id: String,
    @SerialName("test_type") val testType: String, // "Amsler Grid" or "Tumbling E"
    @SerialName("eye_side") val eyeSide: String,   // "Left", "Right"
    @SerialName("timestamp") val timestamp: String,
    @SerialName("image_file_id") val imageId: String? = null,
    val notes: String? = null,
    // Tumbling E specific fields
    @SerialName("final_acuity") val finalAcuity: String? = null
)

// --- Vision Test Models (Tumbling E) ---
@Serializable
data class LevelResult(
    val levelName: String, // "6/60"
    val correct: Int,
    val total: Int,
    val passed: Boolean
)

@Serializable
data class VisionTestResult(
    val patientId: String,
    val patientName: String,
    val eyeSide: String, // "Right" or "Left"
    val finalAcuity: String, // e.g., "6/12"
    val finalLogMAR: Double, // e.g., 0.3
    val levelsAttempted: List<LevelResult>
)

// --- Vision Test Request Models (for API submission) ---
@Serializable
data class VisionTestSession(
    val level: String,
    val correct: Boolean,
    val score: String
)

@Serializable
data class VisionTestRequest(
    @SerialName("patient_id") val patientId: String,
    @SerialName("patient_name") val patientName: String,
    val timestamp: String,
    @SerialName("test_eye") val testEye: String,
    @SerialName("final_acuity") val finalAcuity: String,
    @SerialName("logMAR_levels") val logMARLevels: List<Double>,
    val sessions: List<VisionTestSession>
)

// --- Adherence Graph Data Model ---
@Serializable
data class GraphData(
    val title: String,
    @SerialName("x_axis") val xAxis: List<String>,
    @SerialName("y_axis") val yAxis: List<Int>,
    @SerialName("view_mode") val viewMode: String
)

