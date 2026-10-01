package com.vizualx.app.perception.heuristics

/**
 * Data class representing a processed and spatially verified visual object.
 *
 * @property label The detected object class name (e.g. "Chair", "Person", "Vehicle").
 * @property relativeDistance Estimated relative distance in meters derived from bounding box screen coverage.
 * @property areaRatio Screen surface area fraction occupied by the bounding box (0.0 to 1.0).
 * @property centerX Normalized horizontal center coordinate (0.0 = left edge, 0.5 = center, 1.0 = right edge).
 * @property confidence Confidence score of the detection (0.0 to 1.0).
 * @property timestamp Epoch timestamp (in milliseconds) when the object was detected.
 */
data class ProcessedObject(
    val label: String,
    val relativeDistance: Float,
    val areaRatio: Float = 0.0f,
    val centerX: Float = 0.5f,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Thread-safe cooldown manager to prevent audio fatigue by suppressing duplicate
 * auditory announcements for the same object within a configurable time window (default 5000ms).
 */
class ObjectCooldownTracker(
    val cooldownMs: Long = 5000L
) {
    /**
     * Map storing the last timestamp (in ms) when an object label was verbally spoken.
     */
    private val lastSpokenObjects = HashMap<String, Long>()

    /**
     * Checks whether an object label is eligible for speech announcement based on cooldown.
     *
     * @param label The object label to verify.
     * @param now Current timestamp in milliseconds.
     * @return true if the object has never been spoken or if cooldown has elapsed.
     */
    @Synchronized
    fun isEligible(label: String, now: Long = System.currentTimeMillis()): Boolean {
        val key = label.lowercase().trim()
        val lastSpoken = lastSpokenObjects[key] ?: return true
        return (now - lastSpoken) >= cooldownMs
    }

    /**
     * Records that an object label was spoken out loud at the given timestamp.
     *
     * @param label The object label.
     * @param now Current timestamp in milliseconds.
     */
    @Synchronized
    fun recordSpoken(label: String, now: Long = System.currentTimeMillis()) {
        val key = label.lowercase().trim()
        lastSpokenObjects[key] = now
    }

    /**
     * Prunes expired records older than the cooldown window to free memory.
     */
    @Synchronized
    fun pruneExpired(now: Long = System.currentTimeMillis()) {
        val cutoff = now - cooldownMs
        lastSpokenObjects.entries.removeIf { it.value < cutoff }
    }

    /**
     * Clears all recorded spoken timestamps.
     */
    @Synchronized
    fun clear() {
        lastSpokenObjects.clear()
    }

    /**
     * Returns an immutable copy of the current cooldown state.
     */
    @Synchronized
    fun getSnapshot(): Map<String, Long> {
        return HashMap(lastSpokenObjects)
    }
}
