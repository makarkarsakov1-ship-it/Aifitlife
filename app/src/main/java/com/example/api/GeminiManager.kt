package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL = "gemini-3.5-flash"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun generateAdvice(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is not configured. Please enter your GEMINI_API_KEY in the AI Studio Secrets panel to activate your personal AI Coach."
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        
        val jsonRequest = """
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": ${escapeJson(prompt)}
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonRequest.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: ${response.code} $errBody")
                    return@withContext "Error: API call failed with code ${response.code}."
                }
                val responseBody = response.body?.string() ?: return@withContext "Error: Empty response."
                return@withContext parseResponseText(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during gemini call", e)
            return@withContext "Connection error: ${e.localizedMessage ?: "Please check your network and try again."}"
        }
    }

    private fun escapeJson(string: String): String {
        return moshi.adapter(String::class.java).toJson(string)
    }

    private fun parseResponseText(json: String): String {
        return try {
            val mapAdapter = moshi.adapter(Map::class.java)
            val root = mapAdapter.fromJson(json) as? Map<*, *>
            val candidates = root?.get("candidates") as? List<*>
            val candidate = candidates?.firstOrNull() as? Map<*, *>
            val content = candidate?.get("content") as? Map<*, *>
            val parts = content?.get("parts") as? List<*>
            val part = parts?.firstOrNull() as? Map<*, *>
            val text = part?.get("text") as? String
            text ?: "Could not extract text from Gemini response."
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON response", e)
            "Error parsing advice."
        }
    }
}
