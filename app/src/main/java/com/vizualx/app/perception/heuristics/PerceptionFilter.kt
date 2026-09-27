package com.vizualx.app.perception.heuristics

import com.vizualx.app.perception.models.RelativePosition

/**
 * Intent-Driven Path of Travel Geometry Filter.
 *
 * Visually impaired users require safe progression forward. The camera view is visualized
 * as a normalized coordinate grid from (0.0, 0.0) top-left to (1.0, 1.0) bottom-right.
 *
 * This filter discards peripheral background objects (e.g. benches or trees on the far left or right)
 * that the user will safely walk past, while surfacing obstacles directly in the walking corridor
 * and critical moving hazards.
 */
object PerceptionFilter {

    /**
     * Normalized X-axis boundary for the active walking corridor.
     * The center-third of the screen (0.35 to 0.65) represents the immediate path of travel.
     */
    const val PATH_LEFT_BOUND = 0.35f
    const val PATH_RIGHT_BOUND = 0.65f

    /**
     * Minimum screen area ratio (12%) for an object to be considered close enough
     * to warrant immediate verbal collision warning.
     */
    const val MIN_PROXIMITY_AREA_RATIO = 0.12f

    /**
     * Minimum bounding box height ratio (35% of frame) indicating near proximity.
     */
    const val MIN_PROXIMITY_HEIGHT_RATIO = 0.35f

    /**
     * Computes the relative position based on normalized center X coordinate.
     */
    fun calculatePosition(centerX: Float): RelativePosition {
        return when {
            centerX < PATH_LEFT_BOUND -> RelativePosition.LEFT
            centerX > PATH_RIGHT_BOUND -> RelativePosition.RIGHT
            else -> RelativePosition.AHEAD
        }
    }

    /**
     * Returns true if the object's center X coordinate falls in the walking corridor.
     */
    fun isInWalkingPath(centerX: Float): Boolean {
        return centerX in PATH_LEFT_BOUND..PATH_RIGHT_BOUND
    }

    /**
     * Returns true if the object occupies significant screen space indicating close proximity.
     */
    fun isCloseEnough(areaRatio: Float, heightRatio: Float): Boolean {
        return areaRatio >= MIN_PROXIMITY_AREA_RATIO || heightRatio >= MIN_PROXIMITY_HEIGHT_RATIO
    }

    /**
     * Determines whether the detected object label is an urgent moving safety hazard (e.g. vehicles).
     */
    fun isUrgentHazard(label: String): Boolean {
        val lower = label.lowercase()
        return lower.contains("car") || lower.contains("bus") || 
               lower.contains("truck") || lower.contains("motorcycle") ||
               lower.contains("train")
    }

    /**
     * Master semantic filter:
     * Only triggers spoken announcements if:
     * 1. The object is in the immediate walking path AND close enough, OR
     * 2. The object is an urgent safety hazard (vehicle), regardless of position.
     */
    fun shouldAlert(
        centerX: Float,
        areaRatio: Float,
        heightRatio: Float,
        label: String
    ): Boolean {
        val inPath = isInWalkingPath(centerX)
        val close = isCloseEnough(areaRatio, heightRatio)
        val hazard = isUrgentHazard(label)

        return (inPath && close) || hazard
    }
}
