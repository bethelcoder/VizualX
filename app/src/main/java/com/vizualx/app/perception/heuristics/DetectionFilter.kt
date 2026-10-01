package com.vizualx.app.perception.heuristics

import android.graphics.RectF
import com.google.mediapipe.tasks.components.containers.Detection
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Spatial Filtering Engine for assistive smart glasses vision.
 *
 * Implements low-latency spatial and geometric filtering on raw MediaPipe object detections:
 * 1. Confidence thresholding: Discards noisy, low-confidence predictions (score < 0.65).
 * 2. Proximity/Area thresholding: Discards distant background objects occupying less than 12%
 *    of the visual field to prevent cognitive and audio fatigue.
 * 3. Central corridor and proximity priority sorting: Prioritizes immediate obstacles directly
 *    in the walking path over peripheral objects.
 * 4. Limits output to the top 3 most critical items.
 */
object DetectionFilter {

    /**
     * Minimum detection confidence score (0.65) to eliminate false positives.
     */
    const val MIN_CONFIDENCE_SCORE = 0.65f

    /**
     * Minimum screen surface area fraction (12%) occupied by a bounding box.
     * Objects smaller than 12% of the frame are treated as too far away to pose immediate collision risk.
     */
    const val MIN_FRAME_AREA_PERCENTAGE = 0.12f

    /**
     * Maximum number of critical objects passed downstream to the LLM.
     */
    const val MAX_CRITICAL_ITEMS = 3

    /**
     * Processes raw MediaPipe detections, filters out distant or low-confidence artifacts,
     * sorts by proximity and walking corridor centrality, and returns the top 3 critical items.
     *
     * @param detections Raw list of detections from MediaPipe Object Detector.
     * @param frameWidth Width of the image frame (in pixels), defaults to 640.
     * @param frameHeight Height of the image frame (in pixels), defaults to 480.
     * @param cooldownTracker Optional cooldown tracker to suppress objects spoken in the last 5s.
     * @param currentTimestampMs Timestamp of detection in ms.
     * @return Prioritized list of at most 3 critical [ProcessedObject] instances.
     */
    fun filterAndPrioritize(
        detections: List<Detection>,
        frameWidth: Int = 640,
        frameHeight: Int = 480,
        cooldownTracker: ObjectCooldownTracker? = null,
        currentTimestampMs: Long = System.currentTimeMillis()
    ): List<ProcessedObject> {
        if (detections.isEmpty()) return emptyList()

        val totalFrameArea = (frameWidth * frameHeight).toFloat().coerceAtLeast(1.0f)

        return detections.mapNotNull { detection ->
            // 1. Extract top-confidence category
            val topCategory = detection.categories()
                .maxByOrNull { it.score() }
                ?: return@mapNotNull null

            val score = topCategory.score()
            // Filter 1: Confidence threshold check
            if (score < MIN_CONFIDENCE_SCORE) {
                return@mapNotNull null
            }

            val rawLabel = topCategory.categoryName() ?: "object"
            val cleanLabel = rawLabel.trim().replaceFirstChar { it.uppercase() }

            // Optional Cooldown Filter: Suppress repeated announcements within cooldown window
            if (cooldownTracker != null && !cooldownTracker.isEligible(cleanLabel, currentTimestampMs)) {
                return@mapNotNull null
            }

            // 2. Calculate bounding box metrics
            val box = detection.boundingBox() ?: RectF()
            val (areaPercentage, normalizedCenterX) = calculateSpatialMetrics(
                box = box,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                totalFrameArea = totalFrameArea
            )

            // Filter 2: Screen surface area threshold (minimum 12% of frame)
            if (areaPercentage < MIN_FRAME_AREA_PERCENTAGE) {
                return@mapNotNull null
            }

            // 3. Derive relative distance in meters from bounding box area
            val relativeDistMeters = deriveRelativeDistanceMeters(areaPercentage)

            // 4. Calculate composite ranking score: Blend 60% proximity (area) + 40% centrality
            // Centrality is 1.0 at center (x=0.5) and decays linearly toward screen edges (x=0.0 / 1.0)
            val centralityScore = 1.0f - (2.0f * abs(normalizedCenterX - 0.5f)).coerceIn(0.0f, 1.0f)
            val proximityScore = areaPercentage.coerceIn(0.0f, 1.0f)
            val priorityRank = (proximityScore * 0.60f) + (centralityScore * 0.40f)

            RankedCandidate(
                processedObject = ProcessedObject(
                    label = cleanLabel,
                    relativeDistance = relativeDistMeters,
                    areaRatio = areaPercentage,
                    centerX = normalizedCenterX,
                    confidence = score,
                    timestamp = currentTimestampMs
                ),
                priorityRank = priorityRank
            )
        }
            // Sort remaining objects: highest priority score (closest & most central) first
            .sortedByDescending { it.priorityRank }
            .take(MAX_CRITICAL_ITEMS)
            .map { it.processedObject }
    }

    /**
     * Computes the normalized area percentage and horizontal center coordinate of a bounding box.
     */
    internal fun calculateSpatialMetrics(
        box: RectF,
        frameWidth: Int,
        frameHeight: Int,
        totalFrameArea: Float
    ): Pair<Float, Float> {
        val boxWidth = (box.right - box.left).coerceAtLeast(0.0f)
        val boxHeight = (box.bottom - box.top).coerceAtLeast(0.0f)
        val boxCenterX = (box.left + box.right) / 2.0f

        val isNormalized = boxWidth <= 1.0f && boxHeight <= 1.0f && box.right <= 1.0f && box.bottom <= 1.0f

        val areaPercentage: Float
        val normalizedCenterX: Float

        if (isNormalized) {
            areaPercentage = (boxWidth * boxHeight).coerceIn(0.0f, 1.0f)
            normalizedCenterX = boxCenterX.coerceIn(0.0f, 1.0f)
        } else {
            val boxArea = boxWidth * boxHeight
            areaPercentage = (boxArea / totalFrameArea).coerceIn(0.0f, 1.0f)
            normalizedCenterX = (boxCenterX / frameWidth.toFloat()).coerceIn(0.0f, 1.0f)
        }

        return Pair(areaPercentage, normalizedCenterX)
    }

    /**
     * Converts a bounding box area fraction into an intuitive estimated relative distance in meters.
     * Uses inverse square-root optical scaling calibrated for mobile lenses:
     * - 50% screen coverage -> ~1.4 meters
     * - 25% screen coverage -> ~2.0 meters
     * - 12% screen coverage -> ~2.9 meters
     */
    fun deriveRelativeDistanceMeters(areaFraction: Float): Float {
        val safeArea = areaFraction.coerceIn(0.01f, 1.0f).toDouble()
        val estimatedMeters = (1.0 / sqrt(safeArea)).toFloat()
        return (estimatedMeters * 10f).toInt() / 10f // Round to 1 decimal place
    }

    private data class RankedCandidate(
        val processedObject: ProcessedObject,
        val priorityRank: Float
    )
}
