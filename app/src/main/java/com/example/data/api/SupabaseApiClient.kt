package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseException(message: String) : Exception(message)

class SupabaseApiClient(private val sessionManager: SessionManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildHeaders(builder: Request.Builder, includeAuth: Boolean = true) {
        builder.addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY)
        val authToken = if (includeAuth && !sessionManager.token.isNullOrEmpty()) {
            sessionManager.token
        } else {
            SupabaseConfig.SUPABASE_ANON_KEY
        }
        builder.addHeader("Authorization", "Bearer $authToken")
    }

    private fun parseErrorMessage(responseBody: String?, httpCode: Int): String {
        if (responseBody.isNullOrBlank()) {
            return "Request failed with status code $httpCode"
        }
        return try {
            val json = JSONObject(responseBody)
            when {
                json.has("message") -> json.getString("message")
                json.has("error_description") -> json.getString("error_description")
                json.has("msg") -> json.getString("msg")
                json.has("error") -> {
                    val err = json.get("error")
                    if (err is JSONObject && err.has("message")) {
                        err.getString("message")
                    } else {
                        err.toString()
                    }
                }
                json.has("hint") && json.getString("hint").isNotEmpty() -> json.getString("hint")
                json.has("details") && json.getString("details").isNotEmpty() -> json.getString("details")
                else -> responseBody
            }
        } catch (_: Exception) {
            responseBody
        }
    }

    // 1. Auth: Send OTP
    suspend fun signInWithOtp(phone: String, fullName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("phone", phone)
                val options = JSONObject().apply {
                    val data = JSONObject().apply {
                        put("full_name", fullName)
                        put("phone", phone)
                        put("role", "driver")
                        put("device_id", sessionManager.deviceId)
                    }
                    put("data", data)
                }
                put("options", options)
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.AUTH_URL}/otp")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
            buildHeaders(request, includeAuth = false)

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    Result.success("OTP sent to $phone")
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. Auth: Verify OTP
    suspend fun verifyOtp(phone: String, token: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JSONObject().apply {
                put("phone", phone)
                put("token", token)
                put("type", "sms")
            }

            val request = Request.Builder()
                .url("${SupabaseConfig.AUTH_URL}/verify")
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
            buildHeaders(request, includeAuth = false)

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful && respBody != null) {
                    val json = JSONObject(respBody)
                    val accessToken = json.optString("access_token", "")
                    val userObj = json.optJSONObject("user")
                    val userId = userObj?.optString("id", "") ?: ""
                    
                    if (accessToken.isNotEmpty()) {
                        sessionManager.token = accessToken
                    }
                    if (userId.isNotEmpty()) {
                        sessionManager.userId = userId
                    }
                    sessionManager.phone = phone
                    Result.success(json)
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Generic RPC execution (Postgres stored procedure)
    suspend fun callRpc(functionName: String, params: JSONObject = JSONObject()): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.REST_URL}/rpc/$functionName")
                .post(params.toString().toRequestBody(jsonMediaType))
            buildHeaders(request, includeAuth = true)

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    Result.success(respBody ?: "")
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Generic REST GET query
    suspend fun getTable(endpointWithQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (endpointWithQuery.startsWith("http")) endpointWithQuery else "${SupabaseConfig.REST_URL}/$endpointWithQuery"
            val request = Request.Builder()
                .url(url)
                .get()
            buildHeaders(request, includeAuth = true)

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    Result.success(respBody ?: "[]")
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Generic REST POST
    suspend fun postTable(table: String, body: JSONObject, preferReturn: Boolean = true, upsert: Boolean = false): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${SupabaseConfig.REST_URL}/$table")
                .post(body.toString().toRequestBody(jsonMediaType))
            buildHeaders(request, includeAuth = true)
            
            if (upsert) {
                request.addHeader("Prefer", "resolution=merge-duplicates, return=representation")
            } else if (preferReturn) {
                request.addHeader("Prefer", "return=representation")
            }

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    Result.success(respBody ?: "")
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Storage Upload
    suspend fun uploadStorage(bucket: String, path: String, fileBytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "${SupabaseConfig.STORAGE_URL}/object/$bucket/$path"
            val request = Request.Builder()
                .url(url)
                .post(fileBytes.toRequestBody("image/jpeg".toMediaType()))
                .addHeader("x-upsert", "true")
            buildHeaders(request, includeAuth = true)

            client.newCall(request.build()).execute().use { response ->
                val respBody = response.body?.string()
                if (response.isSuccessful) {
                    Result.success("$bucket/$path")
                } else {
                    Result.failure(SupabaseException(parseErrorMessage(respBody, response.code)))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
