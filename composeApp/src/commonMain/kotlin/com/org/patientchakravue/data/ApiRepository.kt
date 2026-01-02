package com.org.patientchakravue.data

import com.org.patientchakravue.model.AdherenceResponse
import com.org.patientchakravue.model.ChatMessage
import com.org.patientchakravue.model.DoctorNote
import com.org.patientchakravue.model.DoseItem
import com.org.patientchakravue.model.LoginRequest
import com.org.patientchakravue.model.Patient
import com.org.patientchakravue.model.VisionTestRecord
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
    // PUBLIC BASE URL for use by UI components (e.g., image loading)
    companion object {
        const val BASE_URL = "https://patient.chakravue.co.in"
    }

    private val baseUrl = BASE_URL

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

    suspend fun getConversation(submissionId: String): List<ChatMessage> {
        return try {
            val response = NetworkClient.client.get("$baseUrl/submissions/$submissionId/conversation")
            if (response.status == HttpStatusCode.OK) {
                response.body<List<ChatMessage>>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getTodayDoses(patientId: String): List<DoseItem> {
        return try {
            val response = NetworkClient.client.get("$baseUrl/patients/$patientId/doses/today")
            if (response.status == HttpStatusCode.OK) {
                response.body<List<DoseItem>>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun markDoseTaken(patientId: String, doseId: String): Boolean {
        return try {
            val response = NetworkClient.client.post("$baseUrl/patients/$patientId/doses/$doseId/take")
            response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getPatientProfile(patientId: String): Patient? {
        return try {
            val response = NetworkClient.client.get("$baseUrl/patients/$patientId")
            if (response.status == HttpStatusCode.OK) {
                response.body<Patient>()
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getVisionHistory(patientId: String): List<VisionTestRecord> {
        return try {
            val response = NetworkClient.client.get("$baseUrl/vision/patient/$patientId")
            if (response.status == HttpStatusCode.OK) {
                response.body<List<VisionTestRecord>>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun submitAmslerTest(
        imageBytes: ByteArray,
        patientId: String,
        eyeSide: String,
        notes: String
    ): Boolean {
        return try {
            val response = NetworkClient.client.submitFormWithBinaryData(
                url = "$baseUrl/vision/amsler",
                formData = formData {
                    append("patient_id", patientId)
                    append("eye_side", eyeSide)
                    append("notes", notes)
                    append("image", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"amsler_test.jpg\"")
                    })
                }
            )
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun getNextDose(patientId: String): DoseItem? {
        return try {
            val response = NetworkClient.client.get("$baseUrl/patients/$patientId/doses/next")
            if (response.status == HttpStatusCode.OK) {
                response.body<DoseItem>()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Tumbling E Vision Test submission
    suspend fun submitVisionTest(
        patientId: String,
        patientName: String,
        eyeSide: String,
        finalAcuity: String,
        details: List<com.org.patientchakravue.model.LevelResult>
    ): Boolean {
        return try {
            // Map medical results to backend structure
            val logMARLevels = details.map { levelResult ->
                when (levelResult.levelName) {
                    "6/6" -> 0.0
                    "6/9" -> 0.18
                    "6/12" -> 0.3
                    "6/18" -> 0.48
                    "6/24" -> 0.6
                    "6/36" -> 0.78
                    "6/60" -> 1.0
                    else -> 1.0
                }
            }

            val sessions = details.map { levelResult ->
                com.org.patientchakravue.model.VisionTestSession(
                    level = levelResult.levelName,
                    correct = levelResult.passed,
                    score = "${levelResult.correct}/${levelResult.total}"
                )
            }

            val requestBody = com.org.patientchakravue.model.VisionTestRequest(
                patientId = patientId,
                patientName = patientName,
                timestamp = kotlinx.datetime.Clock.System.now().toString(),
                testEye = eyeSide,
                finalAcuity = finalAcuity,
                logMARLevels = logMARLevels,
                sessions = sessions
            )

            val response = NetworkClient.client.post("$baseUrl/vision-tests") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Adherence Graph Data
    suspend fun getGraphData(patientId: String, viewMode: String): com.org.patientchakravue.model.GraphData? {
        return try {
            val response = NetworkClient.client.get("$baseUrl/adherence/graph") {
                parameter("patient_id", patientId)
                parameter("view_mode", viewMode)
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<com.org.patientchakravue.model.GraphData>()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // FCM Token Registration for Push Notifications
    suspend fun registerFcmToken(patientId: String, token: String): Boolean {
        return try {
            val response = NetworkClient.client.post("$baseUrl/patients/$patientId/fcm-token") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "patient_id" to patientId,
                    "token" to token,
                    "platform" to "android"
                ))
            }
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
