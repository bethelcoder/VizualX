package com.vizualx.app.perception

import androidx.camera.core.ImageProxy
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
    private val frameThrottleMs = 250L // 4 FPS processing throttle for battery & thermal efficiency

    fun processCameraFrame(imageProxy: ImageProxy, source: ObservationSource = ObservationSource.REAR_CAMERA) {
        val now = System.currentTimeMillis()
        if (now - lastFrameProcessTime < frameThrottleMs) {
            return
        }
        lastFrameProcessTime = now

        // Extract frame characteristics (placeholder ready for ML Kit / LiteRT ObjectDetector)
        // For baseline/initialization, we keep frame processing decoupled and non-blocking
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
