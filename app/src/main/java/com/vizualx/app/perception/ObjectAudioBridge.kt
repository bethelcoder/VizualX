package com.vizualx.app.perception

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.google.mediapipe.tasks.components.containers.Detection
import com.vizualx.app.ai.LocalLlmBrain
import com.vizualx.app.perception.heuristics.DetectionFilter
import com.vizualx.app.perception.heuristics.ObjectCooldownTracker
import com.vizualx.app.perception.heuristics.ProcessedObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinator & Glue Bridge connecting MediaPipe Object Detection,
 * Spatial Filtering, On-Device Local LLM Inference, and Android TextToSpeech.
 *
 * Architecture & Concurrency Contract:
 * 1. Low Latency: Filters raw detections before LLM ingestion.
 * 2. Heavy Processing Lock: Uses [isSpeakingOrProcessing] atomic flag to drop
 *    incoming video frames while an LLM prompt is being synthesized or spoken by TTS.
 * 3. Audio Fatigue Mitigation: Integrates [ObjectCooldownTracker] to suppress repeating
 *    the same obstacle within a 5-second window.
 */
class ObjectAudioBridge(
    private val context: Context,
    private val localLlmBrain: LocalLlmBrain = LocalLlmBrain(context),
    private val cooldownTracker: ObjectCooldownTracker = ObjectCooldownTracker(cooldownMs = 5000L),
    private val onWarningGenerated: ((warningText: String, objects: List<ProcessedObject>) -> Unit)? = null
) : TextToSpeech.OnInitListener, AutoCloseable {

    companion object {
        private const val TAG = "ObjectAudioBridge"
        private const val UTTERANCE_PREFIX = "vizualx_bridge_"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Global atomic lock preventing frame ingestion while LLM is generating or TTS is speaking.
     */
    val isSpeakingOrProcessing = AtomicBoolean(false)

    private var tts: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    private val _lastSpokenWarning = MutableStateFlow<String?>(null)
    val lastSpokenWarning: StateFlow<String?> = _lastSpokenWarning.asStateFlow()

    private val _activeProcessedObjects = MutableStateFlow<List<ProcessedObject>>(emptyList())
    val activeProcessedObjects: StateFlow<List<ProcessedObject>> = _activeProcessedObjects.asStateFlow()

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Default TTS language US not supported, falling back to device default.")
                tts?.language = Locale.getDefault()
            }

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Retain busy state while speaking
                    isSpeakingOrProcessing.set(true)
                }

                override fun onDone(utteranceId: String?) {
                    // Release lock once spoken utterance completes
                    isSpeakingOrProcessing.set(false)
                    Log.d(TAG, "TTS playback finished for utterance: $utteranceId")
                }

                override fun onError(utteranceId: String?) {
                    // Release lock on error to prevent deadlock
                    isSpeakingOrProcessing.set(false)
                    Log.e(TAG, "TTS playback error for utterance: $utteranceId")
                }
            })
            isTtsReady = true
            Log.i(TAG, "TextToSpeech successfully initialized in ObjectAudioBridge.")
        } else {
            Log.e(TAG, "Failed to initialize TextToSpeech (status: $status).")
            isTtsReady = false
        }
    }

    /**
     * Primary entry point invoked by the camera pipeline on every detected frame.
     *
     * @param detections Raw detections from MediaPipe ObjectDetector.
     * @param frameWidth Width of the video frame in pixels.
     * @param frameHeight Height of the video frame in pixels.
     */
    fun onDetectionsReceived(
        detections: List<Detection>,
        frameWidth: Int = 640,
        frameHeight: Int = 480
    ) {
        // Fast-fail check: If the system is currently speaking or processing, drop frame immediately
        if (isSpeakingOrProcessing.get()) {
            return
        }

        val now = System.currentTimeMillis()

        // 1. Spatial Filtering Engine: Extract top critical obstacles
        val prioritizedObjects = DetectionFilter.filterAndPrioritize(
            detections = detections,
            frameWidth = frameWidth,
            frameHeight = frameHeight,
            cooldownTracker = cooldownTracker,
            currentTimestampMs = now
        )

        // Silence is a valid state: If no critical close obstacle is found, do nothing
        if (prioritizedObjects.isEmpty()) {
            return
        }

        // 2. Attempt to acquire global processing lock
        if (!isSpeakingOrProcessing.compareAndSet(false, true)) {
            return // Another thread or frame acquired the lock
        }

        _activeProcessedObjects.value = prioritizedObjects

        // 3. Dispatch LLM inference and TTS pipeline asynchronously
        scope.launch {
            try {
                // Record cooldown for detected objects to prevent immediate repeats
                for (obj in prioritizedObjects) {
                    cooldownTracker.recordSpoken(obj.label, now)
                }

                // Execute on-device LLM inference
                val warningMessage = localLlmBrain.generateAuditoryWarning(prioritizedObjects)

                if (warningMessage.isNotBlank()) {
                    _lastSpokenWarning.value = warningMessage
                    onWarningGenerated?.invoke(warningMessage, prioritizedObjects)

                    // Speak out loud through TextToSpeech
                    speakWarning(warningMessage)
                } else {
                    // Nothing to speak; release lock
                    isSpeakingOrProcessing.set(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in ObjectAudioBridge processing pipeline: ${e.message}", e)
                isSpeakingOrProcessing.set(false)
            }
        }
    }

    /**
     * Queues the synthesized warning message for immediate voice output.
     */
    private fun speakWarning(text: String) {
        if (!isTtsReady || tts == null) {
            Log.w(TAG, "TTS not ready. Dropping voice output: $text")
            isSpeakingOrProcessing.set(false)
            return
        }

        val utteranceId = "$UTTERANCE_PREFIX${UUID.randomUUID()}"
        // QUEUE_FLUSH ensures immediate interruption for safety warnings
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            Log.e(TAG, "TTS speak command failed with code: $result")
            isSpeakingOrProcessing.set(false)
        }
    }

    /**
     * Shuts down coroutines, TTS, and LLM instances gracefully.
     */
    override fun close() {
        try {
            scope.cancel()
            tts?.stop()
            tts?.shutdown()
            tts = null
            localLlmBrain.close()
            isSpeakingOrProcessing.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error during ObjectAudioBridge teardown: ${e.message}")
        }
    }
}
