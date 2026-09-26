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
                val posText = formatPosition(obs.position)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = "Caution. Vehicle detected $posText.",
                    displayTitle = "Vehicle Alert",
                    displayDetail = "Vehicle detected $posText (confidence: ${(obs.confidence * 100).toInt()}%)"
                )
            }

            ObjectType.DOOR -> {
                val spoken = debounceSpoken("Doorway detected ahead.", 5000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Doorway",
                    displayDetail = "Entrance / door ${formatDistance(obs.approximateDistanceMeters)} (${obs.label})"
                )
            }
            ObjectType.STAIRS_DOWN -> {
                val spoken = debounceSpoken("Caution. Stairs going down directly ahead.", 4000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = spoken,
                    displayTitle = "Stairs Down Hazard",
                    displayDetail = "Stairs descending ahead"
                )
            }
            ObjectType.STAIRS_UP -> {
                val spoken = debounceSpoken("Stairs going up ahead.", 4000L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = spoken,
                    displayTitle = "Stairs Up",
                    displayDetail = "Stairs ascending ahead"
                )
            }
            ObjectType.OBSTACLE -> {
                val isImmediate = (obs.approximateDistanceMeters ?: 5f) < 2.0f
                val priority = if (isImmediate) EventPriority.CRITICAL else EventPriority.HIGH
                val rawSpoken = if (isImmediate) "Obstacle directly in your path." else "Obstacle ahead."
                val spoken = debounceSpoken(rawSpoken, 3500L)
                ContextEvent(
                    id = obs.id,
                    priority = priority,
                    spokenText = spoken,
                    displayTitle = if (obs.label.isNotBlank() && obs.label != "OBSTACLE") obs.label.replaceFirstChar { it.uppercase() } else "Obstacle",
                    displayDetail = "${obs.label} ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }
            ObjectType.CROSSWALK -> {
                val spoken = debounceSpoken("Pedestrian crossing ahead.", 5000L)
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
                val pos = formatPosition(obs.position)
                val spoken = debounceSpoken("Person $pos.", 3500L)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.NORMAL,
                    spokenText = spoken,
                    displayTitle = "Person",
                    displayDetail = "Person $pos ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }
            ObjectType.SIGN -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.NORMAL,
                    spokenText = null, // don't read every sign aloud unless queried
                    displayTitle = "Sign",
                    displayDetail = obs.label
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
                    spokenText = "Caution. Urgent alert sound detected.",
                    displayTitle = "Urgent Sound",
                    displayDetail = "${audio.soundType.name} detected"
                )
            }
            SoundType.VEHICLE_ENGINE -> {
                val pos = formatPosition(audio.direction)
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Vehicle sound $pos.",
                    displayTitle = "Vehicle Audio",
                    displayDetail = "Engine sound $pos"
                )
            }
            SoundType.FOOTSTEPS -> {
                val pos = formatPosition(audio.direction)
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.NORMAL,
                    spokenText = null,
                    displayTitle = "Footsteps",
                    displayDetail = "Footsteps $pos"
                )
            }
            SoundType.ANNOUNCEMENT -> {
                ContextEvent(
                    id = audio.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Announcement detected.",
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
