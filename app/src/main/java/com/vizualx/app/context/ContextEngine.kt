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
    private val spokenHistory = mutableMapOf<String, Long>()

    fun debounceSpoken(text: String, cooldownMs: Long): String? {
        val now = System.currentTimeMillis()
        val lastSpoken = spokenHistory[text] ?: 0L
        return if (now - lastSpoken >= cooldownMs) {
            spokenHistory[text] = now
            text
        } else {
            null
        }
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
                val spoken = debounceSpoken("Stop immediately. Vehicle moving ahead.", 3500L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = spoken,
                    displayTitle = "Vehicle Alert",
                    displayDetail = "Vehicle moving ahead ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }

            ObjectType.DOOR -> {
                val spoken = debounceSpoken("Entrance ahead. Path open.", 5000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Doorway",
                    displayDetail = "Entrance / door ${formatDistance(obs.approximateDistanceMeters)} (${obs.label})"
                )
            }
            ObjectType.STAIRS_DOWN -> {
                val spoken = debounceSpoken("Stop. Descending stairs ahead, check cane.", 4000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = spoken,
                    displayTitle = "Stairs Down Hazard",
                    displayDetail = "Stairs descending ahead"
                )
            }
            ObjectType.STAIRS_UP -> {
                val spoken = debounceSpoken("Stairs ascending ahead, prepare step.", 4000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Stairs Up",
                    displayDetail = "Stairs ascending ahead"
                )
            }
            ObjectType.OBSTACLE -> {
                val isAhead = obs.position == RelativePosition.AHEAD
                val isImmediate = (obs.approximateDistanceMeters ?: 5f) < 2.0f
                if (!isAhead && !isImmediate) {
                    // Object is on the periphery - user will safely walk past it.
                    // Keep speech channel completely silent!
                    ContextEvent(
                        id = obs.id,
                        priority = EventPriority.LOW,
                        spokenText = null,
                        displayTitle = if (obs.label.isNotBlank() && obs.label != "OBSTACLE") obs.label.replaceFirstChar { it.uppercase() } else "Obstacle",
                        displayDetail = "${obs.label} ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                } else {
                    val priority = if (isImmediate) EventPriority.CRITICAL else EventPriority.HIGH
                    val rawSpoken = if (isImmediate) "Obstacle close. Slow down, sweep cane." else "Obstacle ahead. Proceed cautiously."
                    val spoken = debounceSpoken(rawSpoken, 3500L)
                    ContextEvent(
                        id = obs.id,
                        priority = priority,
                        spokenText = spoken,
                        displayTitle = if (obs.label.isNotBlank() && obs.label != "OBSTACLE") obs.label.replaceFirstChar { it.uppercase() } else "Obstacle",
                        displayDetail = "${obs.label} ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                }
            }
            ObjectType.CROSSWALK -> {
                val spoken = debounceSpoken("Crosswalk ahead. Stop, listen for traffic.", 5000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Crossing",
                    displayDetail = "Crosswalk marked ahead"
                )
            }
            ObjectType.CAMPUS_LANDMARK -> {
                val spoken = debounceSpoken(obs.label, 8000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Landmark",
                    displayDetail = obs.label
                )
            }
            ObjectType.PERSON -> {
                val isAhead = obs.position == RelativePosition.AHEAD
                val isClose = (obs.approximateDistanceMeters ?: 5f) <= 2.5f
                if (!isAhead && !isClose) {
                    // Person is on the side and not directly blocking - silent on speech channel
                    ContextEvent(
                        id = obs.id,
                        priority = EventPriority.LOW,
                        spokenText = null,
                        displayTitle = "Person",
                        displayDetail = "Person ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                } else {
                    val spoken = debounceSpoken("Pedestrian ahead. Proceed cautiously.", 4000L)
                    ContextEvent(
                        id = obs.id,
                        priority = EventPriority.NORMAL,
                        spokenText = spoken,
                        displayTitle = "Person",
                        displayDetail = "Pedestrian ${formatDistance(obs.approximateDistanceMeters)}"
                    )
                }
            }
            ObjectType.SIGN -> {
                val signText = obs.label.removePrefix("Sign: ").trim()
                val spoken = debounceSpoken("Sign ahead: $signText.", 6000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Sign Detected",
                    displayDetail = signText
                )
            }
            ObjectType.TREE, ObjectType.BENCH, ObjectType.UNKNOWN -> {
                val title = if (obs.label.isNotBlank() && obs.label != "UNKNOWN") obs.label.replaceFirstChar { it.uppercase() } else obs.type.name
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.LOW,
                    spokenText = null, // Principle: silence is valid, do not describe background clutter
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
