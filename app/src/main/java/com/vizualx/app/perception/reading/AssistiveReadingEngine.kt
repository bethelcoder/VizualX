package com.vizualx.app.perception.reading

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * On-Device Assistive Reading Engine for the visually impaired.
 *
 * Capabilities:
 * 1. Reads text on signs, paper documents, door plaques, computer screens, menus, and labels.
 * 2. Orders text blocks top-to-bottom and left-to-right into natural spoken sentences.
 * 3. Formats concise, voice-friendly summaries for long texts or reads verbatim.
 */
class AssistiveReadingEngine(
    private val customRecognizer: com.google.mlkit.vision.text.TextRecognizer? = null
) : AutoCloseable {

    companion object {
        private const val TAG = "AssistiveReadingEngine"
    }

    private var recognizerInstance: com.google.mlkit.vision.text.TextRecognizer? = customRecognizer

    private fun getRecognizer(): com.google.mlkit.vision.text.TextRecognizer {
        if (recognizerInstance == null) {
            recognizerInstance = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        }
        return recognizerInstance!!
    }

    private val _isReading = MutableStateFlow(false)
    val isReading: StateFlow<Boolean> = _isReading.asStateFlow()

    private val _lastReadText = MutableStateFlow<String?>(null)
    val lastReadText: StateFlow<String?> = _lastReadText.asStateFlow()

    /**
     * Analyzes an in-memory Bitmap and extracts structured, formatted text in natural reading order.
     */
    suspend fun readTextFromBitmap(bitmap: Bitmap, rotationDegrees: Int = 0): String = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, rotationDegrees)
        _isReading.value = true

        try {
            val textResult = recognizeText(inputImage)
            val extractedText = formatTextBlocks(textResult)

            _lastReadText.value = extractedText
            _isReading.value = false

            if (extractedText.isBlank()) {
                "No readable text detected in front of the camera."
            } else {
                extractedText
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Assistive Reading OCR: ${e.message}", e)
            _isReading.value = false
            "Could not read text. Please hold camera steady and try again."
        }
    }

    private suspend fun recognizeText(inputImage: InputImage): Text = suspendCoroutine { cont ->
        getRecognizer().process(inputImage)
            .addOnSuccessListener { result -> cont.resume(result) }
            .addOnFailureListener { exception ->
                Log.e(TAG, "ML Kit OCR failed: ${exception.message}")
                cont.resume(Text("", emptyList<Text.TextBlock>()))
            }
    }

    /**
     * Orders text blocks top-to-bottom and left-to-right into coherent paragraphs.
     */
    internal fun formatTextBlocks(text: Text): String {
        if (text.textBlocks.isEmpty()) {
            return text.text.trim()
        }

        // Sort text blocks by top coordinate, then left coordinate
        val sortedBlocks = text.textBlocks.sortedWith(
            compareBy({ it.boundingBox?.top ?: 0 }, { it.boundingBox?.left ?: 0 })
        )

        val paragraphs = mutableListOf<String>()
        for (block in sortedBlocks) {
            val blockLines = block.lines.map { it.text.trim() }.filter { it.isNotBlank() }
            if (blockLines.isNotEmpty()) {
                paragraphs.add(blockLines.joinToString(" "))
            }
        }

        return paragraphs.joinToString(". ").replace(Regex("\\s+"), " ").trim()
    }

    override fun close() {
        try {
            recognizerInstance?.close()
        } catch (_: Exception) {}
    }
}
