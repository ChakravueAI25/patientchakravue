package com.org.patientchakravue.data

import com.org.patientchakravue.model.AdherenceResponse
import com.org.patientchakravue.model.DoctorNote
import com.org.patientchakravue.model.LoginRequest
import com.org.patientchakravue.model.Patient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.*

// 1. The Client Setup
object NetworkClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                // IMPORTANT: This prevents crashes if the backend sends
                // extra fields we haven't defined in Models.kt
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        // Useful for debugging network calls in Logcat
        install(Logging) {
            level = LogLevel.INFO
        }
    }
}

// 2. The API Functions
class ApiRepository {
    private val baseUrl = "https://patient.chakravue.co.in"

    suspend fun login(email: String, password: String): Patient? {
        return try {
            val response = NetworkClient.client.post("$baseUrl/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<Patient>()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // FIXED: Now returns AdherenceResponse instead of Map<String, Any>
    suspend fun getAdherenceStats(patientId: String): AdherenceResponse? {
        return try {
            val response = NetworkClient.client.get("$baseUrl/adherence/stats/week/$patientId")
            if (response.status == HttpStatusCode.OK) {
                response.body<AdherenceResponse>()
            } else null
        } catch (e: Exception) {
            // This is where the Map error was happening
            e.printStackTrace()
            null
        }
    }

    // FIXED: Now returns List<DoctorNote> instead of List<Map>
    suspend fun getMessages(patientId: String): List<DoctorNote> {
        return try {
            val response = NetworkClient.client.get("$baseUrl/patients/$patientId/messages")
            if (response.status == HttpStatusCode.OK) {
                response.body<List<DoctorNote>>()
            } else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun recordAdherence(patientId: String, medicine: String, taken: Int): Boolean {
        return try {
            val response = NetworkClient.client.post("$baseUrl/adherence") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "patient_id" to patientId,
                    "medicine" to medicine,
                    "taken" to taken
                ))
            }
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            false
        }
    }

    suspend fun submitReport(
        imageBytes: ByteArray,
        fields: Map<String, String>
    ): Boolean {
        return try {
            val response = NetworkClient.client.submitFormWithBinaryData(
                url = "$baseUrl/submissions",
                formData = formData {
                    fields.forEach { (key, value) -> append(key, value) }
                    append("image", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"upload.jpg\"")
                    })
                }
            )
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }
}

