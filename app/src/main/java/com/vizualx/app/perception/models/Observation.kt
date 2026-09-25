package com.vizualx.app.perception.models

enum class ObservationSource {
    FRONT_CAMERA,
    REAR_CAMERA,
    MICROPHONE,
    LOCATION_SENSOR
}

enum class ObjectType {
    PERSON,
    VEHICLE,
    OBSTACLE,
    STAIRS_UP,
    STAIRS_DOWN,
    DOOR,
    CROSSWALK,
    CAMPUS_LANDMARK,
    SIGN,
    TREE,
    BENCH,
    UNKNOWN
}

enum class RelativePosition {
    AHEAD,
    LEFT,
    RIGHT,
    BEHIND,
    UNKNOWN
}

enum class SoundType {
    VEHICLE_ENGINE,
    HORN,
    SIREN,
    FOOTSTEPS,
    SPEECH,
    ALARM,
    ANNOUNCEMENT,
    AMBIENT,
    UNKNOWN
}

sealed class Observation(
    open val id: String,
    open val source: ObservationSource,
    open val confidence: Float,
    open val timestampMs: Long
)

data class ObjectObservation(
    override val id: String,
    override val source: ObservationSource,
    override val confidence: Float,
    override val timestampMs: Long,
    val type: ObjectType,
    val position: RelativePosition = RelativePosition.AHEAD,
    val approximateDistanceMeters: Float? = null,
    val label: String = type.name
) : Observation(id, source, confidence, timestampMs)

data class AudioObservation(
    override val id: String,
    override val source: ObservationSource = ObservationSource.MICROPHONE,
    override val confidence: Float,
    override val timestampMs: Long,
    val soundType: SoundType,
    val direction: RelativePosition = RelativePosition.UNKNOWN,
    val volumeDb: Float = 0f
) : Observation(id, source, confidence, timestampMs)

data class TextObservation(
    override val id: String,
    override val source: ObservationSource,
    override val confidence: Float,
    override val timestampMs: Long,
    val detectedText: String
) : Observation(id, source, confidence, timestampMs)
