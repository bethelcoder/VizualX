package com.vizualx.app.perception

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.vizualx.app.audio.BaselineSoundClassifier
import com.vizualx.app.audio.SoundClassifier
import com.vizualx.app.context.ContextEngine
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.Observation
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.perception.models.RelativePosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID

class PerceptionEngine(
    val worldState: WorldState = WorldState(),
    val contextEngine: ContextEngine = ContextEngine(worldState),
    private val soundClassifier: SoundClassifier = BaselineSoundClassifier()
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _eventFlow = MutableSharedFlow<ContextEvent>(replay = 10)
    val eventFlow: SharedFlow<ContextEvent> = _eventFlow.asSharedFlow()

    private var lastFrameProcessTime = 0L
    private val frameThrottleMs = 250L // 4 FPS processing throttle for thermal & battery optimization

    // On-device real-time stream object detector
    private val objectDetectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    private val objectDetector = ObjectDetection.getClient(objectDetectorOptions)

    @OptIn(ExperimentalGetImage::class)
    fun processCameraFrame(imageProxy: ImageProxy, source: ObservationSource = ObservationSource.REAR_CAMERA) {
        val now = System.currentTimeMillis()
        if (now - lastFrameProcessTime < frameThrottleMs) {
            return
        }
        lastFrameProcessTime = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) return

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val imageWidth = imageProxy.width.toFloat()
        val imageHeight = imageProxy.height.toFloat()

        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                for (obj in detectedObjects) {
                    val box = obj.boundingBox
                    val centerX = box.centerX() / imageWidth
                    val boxHeightRatio = box.height() / imageHeight

                    // Determine relative position in user's field of view
                    val position = when {
                        centerX < 0.35f -> RelativePosition.LEFT
                        centerX > 0.65f -> RelativePosition.RIGHT
                        else -> RelativePosition.AHEAD
                    }

                    // Estimate proximity based on bounding box vertical coverage
                    val approxDistance = when {
                        boxHeightRatio > 0.65f -> 1.2f // Very close (< 1.5m)
                        boxHeightRatio > 0.40f -> 2.5f // Moderate (~ 2.5m)
                        boxHeightRatio > 0.20f -> 4.5f // Mid range (~ 4.5m)
                        else -> 7.0f // Far (> 5m)
                    }

                    // Extract ML classification label or infer object type
                    val primaryLabel = obj.labels.maxByOrNull { it.confidence }
                    val labelText = primaryLabel?.text?.lowercase() ?: "obstacle"
                    val confidence = primaryLabel?.confidence ?: 0.80f

                    val objectType = when {
                        labelText.contains("person") || labelText.contains("human") -> ObjectType.PERSON
                        labelText.contains("vehicle") || labelText.contains("car") || labelText.contains("bus") -> ObjectType.VEHICLE
                        labelText.contains("door") || labelText.contains("entrance") -> ObjectType.DOOR
                        labelText.contains("stairs") || labelText.contains("step") -> ObjectType.STAIRS_DOWN
                        approxDistance < 2.0f && position == RelativePosition.AHEAD -> ObjectType.OBSTACLE
                        else -> ObjectType.OBSTACLE
                    }

                    val observation = ObjectObservation(
                        id = "ml-${obj.trackingId ?: UUID.randomUUID()}",
                        source = source,
                        confidence = confidence,
                        timestampMs = now,
                        type = objectType,
                        position = position,
                        approximateDistanceMeters = approxDistance,
                        label = primaryLabel?.text ?: objectType.name
                    )

                    emitObservation(observation)
                }
            }
            .addOnFailureListener {
                // Ignore transient frame analysis errors
            }
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
