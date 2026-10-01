package com.vizualx.app.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Gemini Generative AI Client for Cloud Multimodal Perception & Visual Reasoning.
 *
 * Provides high-level visual understanding, document summarization, and scene description
 * for blind users via Google's Gemini 1.5 Flash API.
 */
class GeminiApiClient(
    private val context: Context? = null,
    private val initialApiKey: String? = null
) {
    companion object {
        private const val TAG = "GeminiApiClient"
        private const val PREFS_NAME = "vizualx_gemini_prefs"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        // Default public demo key or user-configured key
        @Volatile
        var globalApiKey: String? = null
    }

    private val prefs = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var currentApiKey: String?
        get() = globalApiKey ?: initialApiKey ?: prefs?.getString(KEY_API_KEY, null)
        set(value) {
            globalApiKey = value
            prefs?.edit()?.putString(KEY_API_KEY, value)?.apply()
        }

    val isConfigured: Boolean
        get() = !currentApiKey.isNullOrBlank()

    /**
     * Sends an image frame + conversational prompt to Gemini 1.5 Flash for multimodal reasoning.
     */
    suspend fun analyzeImageWithPrompt(
        bitmap: Bitmap,
        userPrompt: String,
        maxOutputTokens: Int = 150
    ): String? = withContext(Dispatchers.IO) {
        val key = currentApiKey
        if (key.isNullOrBlank()) {
            Log.d(TAG, "Gemini API key not configured; falling back to local on-device AI.")
            return@withContext null
        }

        try {
            // Compress bitmap to JPEG Base64
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            val endpointUrl = "$BASE_URL?key=$key"
            val url = URL(endpointUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.connectTimeout = 8000
            connection.readTimeout = 12000
            connection.doOutput = true

            // Build Gemini Multimodal Payload
            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            // 1. Text prompt part
            val systemInstructions = "You are an empathetic, concise mobility assistant for a blind person. Describe what is relevant for navigation and safety in 1 to 2 clear, spoken sentences. Never use Markdown formatting or bullet points."
            val textPart = JSONObject().apply {
                put("text", "$systemInstructions\n\nUser Question: $userPrompt")
            }
            partsArray.put(textPart)

            // 2. Inline image part
            val inlineData = JSONObject().apply {
                put("mime_type", "image/jpeg")
                put("data", base64Image)
            }
            val imagePart = JSONObject().apply {
                put("inline_data", inlineData)
            }
            partsArray.put(imagePart)

            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            // Generation config
            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", maxOutputTokens)
            }
            root.put("generationConfig", generationConfig)

            // Send request
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                parseGeminiResponse(responseText)
            } else {
                val errorText = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                Log.w(TAG, "Gemini API error ($responseCode): $errorText")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception during Gemini API call: ${e.message}", e)
            null
        }
    }

    /**
     * Sends a pure text prompt to Gemini 1.5 Flash.
     */
    suspend fun generateText(
        prompt: String,
        systemInstruction: String = "You are an assistive vision assistant for the blind. Respond in 1 to 2 concise spoken sentences."
    ): String? = withContext(Dispatchers.IO) {
        val key = currentApiKey ?: return@withContext null

        try {
            val endpointUrl = "$BASE_URL?key=$key"
            val url = URL(endpointUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.connectTimeout = 6000
            connection.readTimeout = 10000
            connection.doOutput = true

            val root = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()

            val textPart = JSONObject().apply {
                put("text", "$systemInstruction\n\nPrompt: $prompt")
            }
            partsArray.put(textPart)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            root.put("contents", contentsArray)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("maxOutputTokens", 120)
            }
            root.put("generationConfig", generationConfig)

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(root.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                parseGeminiResponse(responseText)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating text from Gemini API: ${e.message}")
            null
        }
    }

    private fun parseGeminiResponse(jsonResponse: String): String? {
        return try {
            val root = JSONObject(jsonResponse)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            val text = parts.getJSONObject(0).optString("text", "").trim()
            text.ifBlank { null }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Gemini response JSON: ${e.message}")
            null
        }
    }
}
