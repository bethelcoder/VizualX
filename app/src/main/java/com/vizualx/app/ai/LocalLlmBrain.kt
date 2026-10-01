package com.vizualx.app.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.vizualx.app.perception.heuristics.ProcessedObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * On-Device Multimodal Auditory Brain powered by Google MediaPipe LLM Inference API.
 *
 * Supports quantized mobile LLMs such as Gemma-2B-IT or Phi-3-Mini running entirely offline on-device.
 *
 * ===============================================================================================
 * MODEL SETUP & ASSETS INSTRUCTIONS:
 * 1. Model Format: MediaPipe LLM Inference requires Task-formatted models (.bin).
 *    Convert Gemma-2B-IT or Phi-3 using Google's GenAI MediaPipe converter tools.
 * 2. Option A (Recommended for fast dev/adb):
 *    Push directly to device tmp storage:
 *    `adb push gemma-2b-it-gpu.bin /data/local/tmp/gemma-2b-it-gpu.bin`
 * 3. Option B (Bundled in Android Assets):
 *    Drop the `.bin` model file in `app/src/main/assets/gemma-2b-it-gpu.bin`
 *    The engine will automatically copy it to internal cache upon first launch if needed.
 * ===============================================================================================
 */
class LocalLlmBrain(
    private val context: Context,
    private val preferredModelPath: String = DEFAULT_DEVICE_MODEL_PATH
) : AutoCloseable {

    companion object {
        private const val TAG = "LocalLlmBrain"

        /**
         * Default adb push path for on-device MediaPipe models
         */
        const val DEFAULT_DEVICE_MODEL_PATH = "/data/local/tmp/gemma-2b-it-gpu.bin"

        /**
         * Fallback asset file name if bundled in app/src/main/assets/
         */
        const val ASSET_MODEL_FILE_NAME = "gemma-2b-it-gpu.bin"

        /**
         * Action-oriented system prompt template for blind navigation.
         * Enforces directive physical actions (Stop, Slow down, Step aside, Sweep cane)
         * and strictly forbids inaccurate spatial directions (left/right).
         */
        const val SYSTEM_PROMPT_TEMPLATE =
            "You are the mobility navigator for a blind user. Convert detected obstacles ahead into one immediate, directive action command under 8 words. Never say left or right. Tell the user exactly what physical action to take (e.g., 'Stop immediately, car moving ahead', 'Slow down, crowd blocking path', 'Obstacle close, sweep cane', 'Stop, descending stairs ahead'). Input obstacles: %s."

        /**
         * Formats the detected objects list into the strict Action Prompt.
         *
         * @param objects List of prioritized objects from spatial filtering.
         * @return Fully formatted system prompt string.
         */
        fun formatSystemPrompt(objects: List<ProcessedObject>): String {
            if (objects.isEmpty()) return ""

            val objectListText = objects.joinToString(", ") { obj ->
                val distanceDesc = when {
                    obj.relativeDistance <= 1.5f -> "1 meter ahead"
                    obj.relativeDistance <= 2.5f -> "2 meters ahead"
                    else -> "${String.format(Locale.US, "%.1f", obj.relativeDistance)} meters ahead"
                }
                "${obj.label} at $distanceDesc"
            }

            return String.format(Locale.US, SYSTEM_PROMPT_TEMPLATE, objectListText)
        }

        /**
         * Cleans up LLM raw token outputs: trims quotes, limits word count to under 8 words,
         * and ensures clean voice articulation.
         */
        fun sanitizeResponse(rawOutput: String): String {
            var clean = rawOutput.trim()
                .replace(Regex("(?i)^(action:|warning:|alert:|caution:|system:|model:)\\s*"), "")
                .replace("\"", "")
                .replace("\n", " ")
                .trim()

            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size > 8) {
                clean = words.take(8).joinToString(" ")
                if (!clean.endsWith(".")) {
                    clean += "."
                }
            }
            return clean
        }

        /**
         * Deterministic, high-speed fallback generator ensuring immediate directive action commands
         * for blind navigation even when the LLM binary is offline.
         */
        fun generateDeterministicFallback(objects: List<ProcessedObject>): String {
            val closest = objects.minByOrNull { it.relativeDistance } ?: return ""
            val lower = closest.label.lowercase()

            return when {
                lower.contains("car") || lower.contains("bus") || lower.contains("truck") || lower.contains("motorcycle") ->
                    "Stop immediately. Vehicle moving ahead."
                lower.contains("stair") ->
                    "Stop. Descending stairs ahead, check cane."
                lower.contains("door") || lower.contains("entrance") ->
                    "Entrance ahead. Path open."
                lower.contains("person") ->
                    if (objects.size >= 3) "Crowd blocking path. Slow down." else "Pedestrian ahead. Proceed cautiously."
                closest.relativeDistance <= 1.5f ->
                    "Obstacle close. Slow down, sweep cane."
                else ->
                    "Obstacle ahead. Proceed cautiously."
            }
        }
    }

    private var llmInference: LlmInference? = null

    /**
     * Flag indicating whether the hardware-accelerated LLM engine is initialized and ready.
     */
    var isModelReady: Boolean = false
        private set

    init {
        initializeLlm()
    }

    /**
     * Initializes the MediaPipe LlmInference engine with GPU/NPU backend options.
     */
    private fun initializeLlm() {
        try {
            val resolvedPath = resolveModelFilePath()
            if (resolvedPath == null) {
                Log.w(
                    TAG,
                    "MediaPipe model file not found at $preferredModelPath or in assets. " +
                            "Will operate in deterministic heuristic fallback mode until model binary is provided."
                )
                return
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(resolvedPath)
                .setMaxTokens(64) // Short token budget to keep latency ultra-low (<250ms)
                .setTopK(40)
                .setTemperature(0.2f) // Low temperature for high factual consistency
                .setRandomSeed(42)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isModelReady = true
            Log.i(TAG, "MediaPipe LlmInference successfully initialized from: $resolvedPath")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPipe LlmInference: ${e.message}", e)
            isModelReady = false
        }
    }

    /**
     * Resolves the model path from device filesystem or extracts from assets.
     */
    private fun resolveModelFilePath(): String? {
        // 1. Check direct path (e.g. /data/local/tmp/gemma-2b-it-gpu.bin)
        val directFile = File(preferredModelPath)
        if (directFile.exists() && directFile.canRead() && directFile.length() > 0) {
            return directFile.absolutePath
        }

        // 2. Check internal files directory (app private storage)
        val internalFile = File(context.filesDir, ASSET_MODEL_FILE_NAME)
        if (internalFile.exists() && internalFile.length() > 0) {
            return internalFile.absolutePath
        }

        // 3. Try to copy from assets folder (app/src/main/assets/)
        try {
            context.assets.open(ASSET_MODEL_FILE_NAME).use { inputStream ->
                FileOutputStream(internalFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            if (internalFile.exists() && internalFile.length() > 0) {
                Log.i(TAG, "Extracted model from assets to ${internalFile.absolutePath}")
                return internalFile.absolutePath
            }
        } catch (_: Exception) {
            // Asset not bundled
        }

        return null
    }

    /**
     * Executes LLM inference asynchronously on a background coroutine dispatcher.
     * Guaranteed never to block the main UI or camera capture thread.
     *
     * @param objects Prioritized objects from spatial filtering.
     * @return Short, active auditory warning under 8 words.
     */
    suspend fun generateAuditoryWarning(objects: List<ProcessedObject>): String = withContext(Dispatchers.Default) {
        if (objects.isEmpty()) return@withContext ""

        val prompt = formatSystemPrompt(objects)
        val engine = llmInference

        if (engine != null && isModelReady) {
            try {
                // MediaPipe LlmInference synchronous generateResponse executed off the main thread
                val rawResponse = engine.generateResponse(prompt)
                val sanitized = sanitizeResponse(rawResponse)
                if (sanitized.isNotBlank()) {
                    return@withContext sanitized
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing on-device LLM inference: ${e.message}", e)
            }
        }

        // Deterministic on-device rule fallback if model binary is not loaded or fails
        return@withContext generateDeterministicFallback(objects)
    }

    override fun close() {
        try {
            llmInference?.close()
            llmInference = null
            isModelReady = false
        } catch (e: Exception) {
            Log.e(TAG, "Error closing LlmInference: ${e.message}")
        }
    }
}
