package com.vizualx.app.perception

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.vizualx.app.audio.BaselineSoundClassifier
import com.vizualx.app.audio.SoundClassifier
import com.vizualx.app.context.ContextEngine
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.heuristics.ContextAwarenessEngine
import com.vizualx.app.perception.mediapipe.MediaPipeObjectDetectorHelper
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.Observation
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.perception.scene.SceneInsight
import com.vizualx.app.perception.scene.SceneUnderstandingHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

import android.util.Log
import com.google.mlkit.vision.common.InputImage

class PerceptionEngine(
    val worldState: WorldState = WorldState(),
    val contextEngine: ContextEngine = ContextEngine(worldState),
    private val soundClassifier: SoundClassifier = BaselineSoundClassifier(),
    context: Context? = null
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _eventFlow = MutableSharedFlow<ContextEvent>(replay = 10)
    val eventFlow: SharedFlow<ContextEvent> = _eventFlow.asSharedFlow()

    // Context Awareness Engine with temporal debouncing & scenario classification
    val contextAwarenessEngine = ContextAwarenessEngine(
        worldState = worldState,
        contextEngine = contextEngine,
        onHazardDetected = { event ->
            scope.launch {
                _eventFlow.emit(event)
            }
        }
    )

    // MediaPipe Tasks Object Detector Helper
    var mediaPipeHelper: MediaPipeObjectDetectorHelper? = null
        private set

    // Scene Understanding Helper (ML Kit Image Labeling for Doors/Stairs + Text OCR)
    private val sceneHelper = SceneUnderstandingHelper()

    // Dedicated Assistive Reading Engine for full document & sign reading
    val readingEngine = com.vizualx.app.perception.reading.AssistiveReadingEngine()

    // Local LLM Auditory & Reading Brain
    var localLlmBrain: com.vizualx.app.ai.LocalLlmBrain? = null
        private set

    @Volatile
    var latestBitmap: android.graphics.Bitmap? = null
        private set
    @Volatile
    var latestRotationDegrees: Int = 0
        private set

    init {
        if (context != null) {
            localLlmBrain = com.vizualx.app.ai.LocalLlmBrain(context)
            mediaPipeHelper = MediaPipeObjectDetectorHelper(
                context = context,
                threshold = 0.40f,
                maxResults = 7,
                modelName = "efficientdet_lite0.tflite",
                resultListener = { result, mpImage ->
                    processMediaPipeResult(result, mpImage.width, mpImage.height)
                },
                errorListener = { err ->
                    Log.e("PerceptionEngine", "MediaPipe error: $err")
                }
            )
        }
    }

    private var lastFrameProcessedTimestamp = 0L
    private val frameThrottleMs = 200L // 5 FPS for thermal efficiency and frame rate control

    fun processCameraFrame(imageProxy: ImageProxy, source: ObservationSource = ObservationSource.REAR_CAMERA) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastFrameProcessedTimestamp < frameThrottleMs) {
            return
        }
        lastFrameProcessedTimestamp = currentTimestamp

        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            // Convert to standalone in-memory Bitmap synchronously so ImageProxy can be safely closed
            val bitmap = imageProxy.toBitmap()
            latestBitmap = bitmap
            latestRotationDegrees = rotation

            // 1. Run Scene Understanding (Doors, Stairs, Signs)
            val inputImage = InputImage.fromBitmap(bitmap, rotation)
            sceneHelper.analyzeScene(inputImage) { insight ->
                handleSceneInsight(insight, source)
            }

            // 2. Run MediaPipe Object Detection
            mediaPipeHelper?.detectLiveStream(
                bitmap = bitmap,
                rotationDegrees = rotation,
                isFrontCamera = (source == ObservationSource.FRONT_CAMERA)
            )
        } catch (e: Exception) {
            Log.e("PerceptionEngine", "Error processing camera frame: ${e.message}")
        }
    }

    /**
     * Reads text in front of the camera on demand for assistive document/sign reading.
     * Uses on-device LocalLlmBrain (Gemma/Phi-3) or SpeechNormalizer to speak fluent words instead of spelling letters.
     */
    suspend fun readAloudCurrentView(): String {
        val bmp = latestBitmap ?: return "Camera feed warming up. Please hold steady and try again in a moment."
        return readingEngine.readTextFromBitmap(bmp, latestRotationDegrees) { rawText ->
            localLlmBrain?.synthesizeReading(rawText)
                ?: com.vizualx.app.speech.SpeechNormalizer.normalizeForSpeech(rawText)
        }
    }

    private fun handleSceneInsight(insight: SceneInsight, source: ObservationSource) {
        val now = System.currentTimeMillis()

        if (insight.detectedType == ObjectType.DOOR || 
            insight.detectedType == ObjectType.STAIRS_DOWN || 
            insight.detectedType == ObjectType.SIGN) {
            
            val obs = ObjectObservation(
                id = "scene-${insight.detectedType.name}-${UUID.randomUUID()}",
                source = source,
                confidence = insight.confidence,
                timestampMs = now,
                type = insight.detectedType,
                position = RelativePosition.AHEAD,
                approximateDistanceMeters = 3.0f,
                label = insight.label
            )
            emitObservation(obs)
        }
    }

    fun processMediaPipeResult(
        result: ObjectDetectorResult,
        imageWidth: Int,
        imageHeight: Int,
        source: ObservationSource = ObservationSource.REAR_CAMERA
    ) {
        contextAwarenessEngine.processDetectionResult(
            result = result,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            source = source
        )
    }

    fun processAudioBuffer(buffer: ShortArray, volumeDb: Float) {
        val observation = soundClassifier.classify(buffer, volumeDb) ?: return
        emitObservation(observation)
    }

    fun emitObservation(observation: Observation) {
        scope.launch {
            val event = contextEngine.evaluate(observation)
            _eventFlow.emit(event)
        }
    }

    fun close() {
        mediaPipeHelper?.close()
        sceneHelper.close()
        readingEngine.close()
        localLlmBrain?.close()
    }

    // Diagnostic & simulation helpers for testing and demonstration
    fun simulateObservation(
        type: ObjectType,
        source: ObservationSource = ObservationSource.REAR_CAMERA,
        position: RelativePosition = RelativePosition.AHEAD,
        distanceMeters: Float = 2.5f,
        confidence: Float = 0.92f,
        label: String = type.name
    ) {
        val obs = ObjectObservation(
            id = UUID.randomUUID().toString(),
            source = source,
            confidence = confidence,
            timestampMs = System.currentTimeMillis(),
            type = type,
            position = position,
            approximateDistanceMeters = distanceMeters,
            label = label
        )
        emitObservation(obs)
    }
}
