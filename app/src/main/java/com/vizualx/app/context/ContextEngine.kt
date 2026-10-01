package com.vizualx.app.context

import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.Observation
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.perception.models.SoundType

enum class EventPriority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW,
    IGNORE
}

data class ContextEvent(
    val id: String,
    val priority: EventPriority,
    val spokenText: String?,
    val displayTitle: String,
    val displayDetail: String,
    val timestampMs: Long = System.currentTimeMillis()
)

class ContextEngine(
    private val worldState: WorldState
) {
    companion object {
        // Critical safety keywords that qualify a sign for automatic spoken warning
        private val HAZARD_SIGN_KEYWORDS = setOf(
            "caution", "danger", "warning", "wet floor", "do not enter",
            "no entry", "stop", "emergency exit", "keep clear", "slippery",
            "hazard", "watch your step", "high voltage", "construction"
        )
    }

    private val categorySpokenHistory = mutableMapOf<String, Long>()
    private var lastAnySpeechTimestamp = 0L

    /**
     * Semantic debounce enforcing category cooldowns and a global minimum silence interval.
     */
    fun debounceSpoken(
        categoryKey: String,
        text: String,
        categoryCooldownMs: Long,
        isCriticalHazard: Boolean = false
    ): String? {
        val now = System.currentTimeMillis()
        val lastCategorySpoken = categorySpokenHistory[categoryKey] ?: 0L

        // Non-critical speech must respect a minimum quiet buffer (5.0s) to prevent overwhelming the user
        if (!isCriticalHazard && (now - lastAnySpeechTimestamp < 5000L)) {
            return null
        }

        if (now - lastCategorySpoken >= categoryCooldownMs) {
            categorySpokenHistory[categoryKey] = now
            lastAnySpeechTimestamp = now
            return text
        }
        return null
    }

    fun evaluate(observation: Observation): ContextEvent {
        worldState.updateObservation(observation)

        return when (observation) {
            is ObjectObservation -> evaluateObject(observation)
            is AudioObservation -> evaluateAudio(observation)
            else -> ContextEvent(
                id = observation.id,
                priority = EventPriority.IGNORE,
                spokenText = null,
                displayTitle = "Observation",
                displayDetail = "Processed observation ${observation.id}"
            )
        }
    }

    private fun evaluateObject(obs: ObjectObservation): ContextEvent {
        return when (obs.type) {
            ObjectType.VEHICLE -> {
                // Critical Hazard: Always alert if vehicle is ahead
                val spoken = debounceSpoken(
                    categoryKey = "VEHICLE",
                    text = "Stop immediately. Vehicle moving ahead.",
                    categoryCooldownMs = 6000L,
                    isCriticalHazard = true
                )
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = spoken,
                    displayTitle = "Vehicle Alert",
                    displayDetail = "Vehicle moving ahead ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }

            ObjectType.STAIRS_DOWN -> {
                // Critical Hazard: Drop-offs and descending stairs
                val spoken = debounceSpoken(
                    categoryKey = "STAIRS_DOWN",
                    text = "Stop. Descending stairs ahead, check cane.",
                    categoryCooldownMs = 8000L,
                    isCriticalHazard = true
                )
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = spoken,
                    displayTitle = "Stairs Down Hazard",
                    displayDetail = "Stairs descending ahead"
                )
            }

            ObjectType.STAIRS_UP -> {
                val isClose = (obs.approximateDistanceMeters ?: 5f) <= 2.5f
                val spoken = if (isClose) {
                    debounceSpoken(
                        categoryKey = "STAIRS_UP",
                        text = "Stairs ascending ahead, prepare step.",
                        categoryCooldownMs = 10000L
                    )
                } else null

                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Stairs Up",
                    displayDetail = "Stairs ascending ahead"
                )
            }

            ObjectType.DOOR -> {
                // Only announce entrance if directly close (< 2.2m)
                val isClose = (obs.approximateDistanceMeters ?: 5f) <= 2.2f
                val spoken = if (isClose) {
                    debounceSpoken(
                        categoryKey = "DOOR",
                        text = "Entrance ahead. Path open.",
                        categoryCooldownMs = 15000L
                    )
                } else null

                ContextEvent(
                    id = obs.id,
                    priority = if (isClose) EventPriority.HIGH else EventPriority.LOW,
                    spokenText = spoken,
                    displayTitle = "Doorway",
                    displayDetail = "Entrance / door ${formatDistance(obs.approximateDistanceMeters)} (${obs.label})"
                )
            }

            ObjectType.OBSTACLE -> {
                val isAhead = obs.position == RelativePosition.AHEAD
                val distance = obs.approximateDistanceMeters ?: 5f
                val isImmediate = isAhead && distance <= 1.8f

                if (!isImmediate) {
                    // SILENCE: Object is peripheral or far enough away
                    ContextEvent(
                        id = obs.id,
                        priority = EventPriority.LOW,
                        spokenText = null,
                        displayTitle = if (obs.label.isNotBlank() && obs.label != "OBSTACLE") obs.label.replaceFirstChar { it.uppercase() } else "Obstacle",
                        displayDetail = "${obs.label} ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                } else {
                    val spoken = debounceSpoken(
                        categoryKey = "OBSTACLE_CLOSE",
                        text = "Obstacle close. Slow down, sweep cane.",
                        categoryCooldownMs = 7000L,
                        isCriticalHazard = true
                    )
                    ContextEvent(
                        id = obs.id,
                        priority = EventPriority.CRITICAL,
                        spokenText = spoken,
                        displayTitle = if (obs.label.isNotBlank() && obs.label != "OBSTACLE") obs.label.replaceFirstChar { it.uppercase() } else "Obstacle",
                        displayDetail = "${obs.label} ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                }
            }

            ObjectType.CROSSWALK -> {
                val spoken = debounceSpoken(
                    categoryKey = "CROSSWALK",
                    text = "Crosswalk ahead. Stop, listen for traffic.",
                    categoryCooldownMs = 15000L
                )
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Crossing",
                    displayDetail = "Crosswalk marked ahead"
                )
            }

            ObjectType.PERSON -> {
                val isAhead = obs.position == RelativePosition.AHEAD
                val isVeryClose = (obs.approximateDistanceMeters ?: 5f) <= 1.5f

                // Gating: Only speak if pedestrian is directly in immediate walking trajectory
                val spoken = if (isAhead && isVeryClose) {
                    debounceSpoken(
                        categoryKey = "PEDESTRIAN_CLOSE",
                        text = "Pedestrian directly ahead. Proceed cautiously.",
                        categoryCooldownMs = 8000L
                    )
                } else null

                ContextEvent(
                    id = obs.id,
                    priority = if (spoken != null) EventPriority.NORMAL else EventPriority.LOW,
                    spokenText = spoken,
                    displayTitle = "Person",
                    displayDetail = "Pedestrian ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }

            ObjectType.SIGN -> {
                val rawSignText = obs.label.removePrefix("Sign: ").trim()
                val lowerSign = rawSignText.lowercase()

                // Gating Rule: Only speak hazard/safety signs automatically (e.g. Caution Wet Floor, Do Not Enter).
                // Ambient informative signs (room numbers, menus, store names) stay silent on auto-stream
                // to avoid chatter, but can be read on-demand by tapping the screen or asking voice AI.
                val isHazard = HAZARD_SIGN_KEYWORDS.any { lowerSign.contains(it) }

                val spoken = if (isHazard) {
                    debounceSpoken(
                        categoryKey = "HAZARD_SIGN",
                        text = "Caution, sign indicates: $rawSignText.",
                        categoryCooldownMs = 12000L,
                        isCriticalHazard = true
                    )
                } else {
                    null // Silence is a valid state for non-hazard ambient signs
                }

                ContextEvent(
                    id = obs.id,
                    priority = if (isHazard) EventPriority.CRITICAL else EventPriority.LOW,
                    spokenText = spoken,
                    displayTitle = if (isHazard) "Hazard Sign" else "Sign Detected",
                    displayDetail = rawSignText
                )
            }

            ObjectType.CAMPUS_LANDMARK, ObjectType.TREE, ObjectType.BENCH, ObjectType.UNKNOWN -> {
                val title = if (obs.label.isNotBlank() && obs.label != "UNKNOWN") obs.label.replaceFirstChar { it.uppercase() } else obs.type.name
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.LOW,
                    spokenText = null, // SILENCE is valid: never speak background objects unprompted
                    displayTitle = title,
                    displayDetail = "Surrounding: $title ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }
        }
    }

    private fun evaluateAudio(audio: AudioObservation): ContextEvent {
        return when (audio.soundType) {
            SoundType.HORN, SoundType.SIREN, SoundType.ALARM -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = "Emergency siren nearby. Stop and stand clear.",
                    displayTitle = "Urgent Sound",
                    displayDetail = "${audio.soundType.name} detected"
                )
            }
            SoundType.VEHICLE_ENGINE -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Engine sound nearby. Caution.",
                    displayTitle = "Vehicle Audio",
                    displayDetail = "Engine sound nearby"
                )
            }
            SoundType.FOOTSTEPS -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.NORMAL,
                    spokenText = null,
                    displayTitle = "Footsteps",
                    displayDetail = "Footsteps nearby"
                )
            }
            SoundType.ANNOUNCEMENT -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Public announcement sound.",
                    displayTitle = "Announcement",
                    displayDetail = "Public announcement sound"
                )
            }
            SoundType.SPEECH, SoundType.AMBIENT, SoundType.UNKNOWN -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.IGNORE,
                    spokenText = null,
                    displayTitle = "Ambient Audio",
                    displayDetail = "Ambient sound level ${audio.volumeDb.toInt()} dB"
                )
            }
        }
    }

    private fun formatPosition(pos: RelativePosition): String {
        return when (pos) {
            RelativePosition.AHEAD -> "ahead"
            RelativePosition.LEFT -> "on your left"
            RelativePosition.RIGHT -> "on your right"
            RelativePosition.BEHIND -> "behind you"
            RelativePosition.UNKNOWN -> "nearby"
        }
    }

    private fun formatDistance(dist: Float?): String {
        return if (dist != null) "~${String.format("%.1f", dist)}m" else ""
    }
}
