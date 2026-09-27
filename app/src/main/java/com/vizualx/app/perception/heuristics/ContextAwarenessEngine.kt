package com.vizualx.app.perception.heuristics

import android.graphics.RectF
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.vizualx.app.context.ContextEngine
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.EventPriority
import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.perception.models.RelativePosition
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TrackedDetection(
    val label: String,
    val confidenceWindow: MutableList<Float> = mutableListOf(),
    val areaHistory: MutableList<Float> = mutableListOf(),
    var lastCenterX: Float = 0.5f,
    var lastHeightRatio: Float = 0.2f,
    var lastSeenMs: Long = System.currentTimeMillis(),
    var lastAlertMs: Long = 0L
) {
    val isConfirmed: Boolean
        get() = confidenceWindow.size >= 3 && confidenceWindow.average() >= 0.55f

    val isExpanding: Boolean
        get() = areaHistory.size >= 3 && areaHistory.last() > (areaHistory.first() * 1.25f)
}

/**
 * Context Awareness Engine with Temporal Debouncing & Heuristic Scenario Classification.
 * Filters frame noise and maps raw detections into confirmed observations evaluated by ContextEngine.
 */
class ContextAwarenessEngine(
    private val worldState: WorldState,
    private val contextEngine: ContextEngine,
    private val onHazardDetected: ((ContextEvent) -> Unit)? = null
) {
    companion object {
        private const val WINDOW_SIZE = 5
        private const val ENTITY_EXPIRY_MS = 3000L
        private const val ALERT_COOLDOWN_MS = 3500L

        val RELEVANT_LABELS = setOf(
            "person", "car", "truck", "bus", "motorcycle", "bicycle",
            "chair", "bench", "table", "couch", "bed",
            "backpack", "suitcase", "fire hydrant"
        )
    }

    private val trackedEntities = ConcurrentHashMap<String, TrackedDetection>()

    fun processDetectionResult(
        result: ObjectDetectorResult,
        imageWidth: Int = 640,
        imageHeight: Int = 480,
        source: ObservationSource = ObservationSource.REAR_CAMERA,
        isHandInView: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        pruneStaleEntities(now)

        val detections = result.detections()
        var personCountInFrame = 0

        for (detection in detections) {
            val category = detection.categories().maxByOrNull { it.score() } ?: continue
            val rawLabel = category.categoryName().lowercase().trim()
            
            // Whitelist filter: discard irrelevant classes (e.g. kite, cup, bowl, tv, vase, etc.)
            val matchedLabel = RELEVANT_LABELS.firstOrNull { rawLabel.contains(it) } ?: continue
            val score = category.score()
            val box = detection.boundingBox() ?: RectF()

            // Discard false person detections caused by user holding phone / hand in view
            if (matchedLabel.contains("person") && isHandInView) {
                continue
            }

            val centerX = box.centerX() / imageWidth.toFloat()
            val heightRatio = box.height() / imageHeight.toFloat()
            val areaRatio = (box.width() * box.height()) / (imageWidth * imageHeight).toFloat()

            val position = PerceptionFilter.calculatePosition(centerX)

            val approxDist = when {
                heightRatio > 0.60f -> 1.2f
                heightRatio > 0.35f -> 2.5f
                heightRatio > 0.18f -> 4.5f
                else -> 7.0f
            }

            if (matchedLabel.contains("person")) {
                personCountInFrame++
            }

            // Update or create tracked temporal entity using relative position bucket
            val entityKey = "$matchedLabel-${position.name}"
            val tracked = trackedEntities.getOrPut(entityKey) {
                TrackedDetection(label = matchedLabel)
            }

            tracked.lastSeenMs = now
            tracked.lastCenterX = centerX
            tracked.lastHeightRatio = heightRatio

            // Maintain rolling sliding window
            tracked.confidenceWindow.add(score)
            if (tracked.confidenceWindow.size > WINDOW_SIZE) {
                tracked.confidenceWindow.removeAt(0)
            }

            tracked.areaHistory.add(areaRatio)
            if (tracked.areaHistory.size > WINDOW_SIZE) {
                tracked.areaHistory.removeAt(0)
            }

            // Evaluate confirmed objects against ContextEngine rules
            if (tracked.isConfirmed && (now - tracked.lastAlertMs >= ALERT_COOLDOWN_MS)) {
                tracked.lastAlertMs = now

                // Emit structured observation into WorldState and evaluate event via ContextEngine
                val obs = ObjectObservation(
                    id = UUID.randomUUID().toString(),
                    source = source,
                    confidence = score,
                    timestampMs = now,
                    type = mapLabelToObjectType(matchedLabel),
                    position = position,
                    approximateDistanceMeters = approxDist,
                    label = matchedLabel.replaceFirstChar { it.uppercase() }
                )
                val event = contextEngine.evaluate(obs)
                onHazardDetected?.invoke(event)
            }
        }

        // Scenario: CROWD DETECTION
        if (personCountInFrame >= 3 && !isHandInView) {
            val crowdEvent = ContextEvent(
                id = "crowd-$now",
                priority = EventPriority.HIGH,
                spokenText = "Crowd of pedestrians ahead.",
                displayTitle = "Crowd Detected",
                displayDetail = "$personCountInFrame pedestrians in vicinity",
                timestampMs = now
            )
            onHazardDetected?.invoke(crowdEvent)
        }
    }

    private fun mapLabelToObjectType(label: String): ObjectType {
        return when {
            label.contains("person") -> ObjectType.PERSON
            label.contains("car") || label.contains("bus") || label.contains("truck") || label.contains("motorcycle") -> ObjectType.VEHICLE
            label.contains("bicycle") || label.contains("bench") || label.contains("chair") || label.contains("table") || label.contains("couch") || label.contains("bed") -> ObjectType.OBSTACLE
            label.contains("door") -> ObjectType.DOOR
            else -> ObjectType.UNKNOWN
        }
    }

    private fun pruneStaleEntities(now: Long) {
        val cutoff = now - ENTITY_EXPIRY_MS
        trackedEntities.entries.removeIf { it.value.lastSeenMs < cutoff }
    }
}
