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
    private var lastSpokenEventTimestamp: Long = 0L
    private var lastSpokenText: String? = null

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
            ObjectType.STAIRS_DOWN -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.CRITICAL,
                    spokenText = "Caution. Stairs going down directly ahead.",
                    displayTitle = "Stairs Down",
                    displayDetail = "Stairs descending ahead"
                )
            }
            ObjectType.STAIRS_UP -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Stairs going up ahead.",
                    displayTitle = "Stairs Up",
                    displayDetail = "Stairs ascending ahead"
                )
            }
            ObjectType.OBSTACLE -> {
                val isImmediate = (obs.approximateDistanceMeters ?: 5f) < 2.0f
                val priority = if (isImmediate) EventPriority.CRITICAL else EventPriority.HIGH
                val spoken = if (isImmediate) "Obstacle directly in your path." else "Obstacle ahead."
                ContextEvent(
                    id = obs.id,
                    priority = priority,
                    spokenText = spoken,
                    displayTitle = "Obstacle Detected",
                    displayDetail = "Obstacle ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }
            ObjectType.DOOR -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Doorway detected ahead.",
                    displayTitle = "Doorway",
                    displayDetail = "Entrance / door ${formatDistance(obs.approximateDistanceMeters)}"
                )
            }
            ObjectType.CROSSWALK -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = "Pedestrian crossing ahead.",
                    displayTitle = "Crossing",
                    displayDetail = "Crosswalk marked ahead"
                )
            }
            ObjectType.CAMPUS_LANDMARK -> {
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.HIGH,
                    spokenText = obs.label,
                    displayTitle = "Landmark",
                    displayDetail = obs.label
                )
            }
            ObjectType.PERSON -> {
                val pos = formatPosition(obs.position)
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.NORMAL,
                    spokenText = "Person $pos.",
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
                ContextEvent(
                    id = obs.id,
                    priority = EventPriority.IGNORE,
                    spokenText = null, // Principle: silence is valid, do not describe background clutter
                    displayTitle = obs.type.name,
                    displayDetail = "Ambient object ${obs.type.name}"
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
