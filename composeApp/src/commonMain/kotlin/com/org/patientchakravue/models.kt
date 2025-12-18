package com.org.patientchakravue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class Patient(
    val _id: String, // MongoDB ID often comes as "_id"
    val name: String?,
    val phone: String?,
    val age: String? = null,
    val sex: String? = null,
    val email: String? = null,
    val doctor_id: String? = null,
    val visits: List<Visit>? = null,
    val prescription: JsonElement? = null // Flexible field
) {
    // Helper to get a clean ID string
    val id: String get() = _id
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

@Serializable
data class Message(
    val from: String,
    val content: String,
    val timestamp: String
)
