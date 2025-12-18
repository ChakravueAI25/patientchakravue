package com.org.patientchakravue

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

// 1. The Client Setup
object NetworkClient {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }
}

// 2. The API Functions
class ApiRepository {
    private val baseUrl = "https://patient.chakravue.co.in"

    suspend fun login(email: String, password: String): Patient? {
        val response = NetworkClient.client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }
        return if (response.status == HttpStatusCode.OK) {
            response.body<Patient>()
        } else {
            null
        }
    }

    suspend fun getAdherenceStats(patientId: String): Map<String, Any>? {
        return try {
            // Ktor returns a Map if the JSON is dynamic
            NetworkClient.client.get("$baseUrl/adherence/stats/week/$patientId").body()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getMessages(patientId: String): List<Map<String, Any>> {
        return try {
            NetworkClient.client.get("$baseUrl/patients/$patientId/messages").body()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Function to submit post-op report with image
    suspend fun submitReport(
        imageBytes: ByteArray,
        fields: Map<String, String>
    ): Boolean {
        return try {
            val response = NetworkClient.client.submitFormWithBinaryData(
                url = "$baseUrl/submissions",
                formData = formData {
                    // Add text fields
                    fields.forEach { (key, value) ->
                        append(key, value)
                    }
                    // Add image file
                    append("image", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(HttpHeaders.ContentDisposition, "filename=\"upload.jpg\"")
                    })
                }
            )
            response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun recordAdherence(patientId: String, medicine: String, taken: Int) {
        try {
            NetworkClient.client.post("$baseUrl/adherence") {
                contentType(ContentType.Application.Json)
                setBody(mapOf(
                    "patient_id" to patientId,
                    "medicine" to medicine,
                    "taken" to taken
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}