package com.vizualx.app.audio

import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.perception.models.SoundType
import java.util.UUID

interface SoundClassifier {
    fun classify(audioBuffer: ShortArray, volumeDb: Float): AudioObservation?
}

class BaselineSoundClassifier : SoundClassifier {

    private var lastDetectionTime = 0L

    override fun classify(audioBuffer: ShortArray, volumeDb: Float): AudioObservation? {
        val now = System.currentTimeMillis()
        if (now - lastDetectionTime < 1500) {
            return null // Throttle sound classification events
        }

        // Energy and threshold heuristics (ready to be swapped with TFLite / YAMNet model)
        return when {
            volumeDb > 75f -> {
                lastDetectionTime = now
                AudioObservation(
                    id = UUID.randomUUID().toString(),
                    confidence = 0.85f,
                    timestampMs = now,
                    soundType = SoundType.HORN,
                    direction = RelativePosition.AHEAD,
                    volumeDb = volumeDb
                )
            }
            volumeDb > 60f -> {
                lastDetectionTime = now
                AudioObservation(
                    id = UUID.randomUUID().toString(),
                    confidence = 0.75f,
                    timestampMs = now,
                    soundType = SoundType.VEHICLE_ENGINE,
                    direction = RelativePosition.RIGHT,
                    volumeDb = volumeDb
                )
            }
            volumeDb > 45f -> {
                lastDetectionTime = now
                AudioObservation(
                    id = UUID.randomUUID().toString(),
                    confidence = 0.65f,
                    timestampMs = now,
                    soundType = SoundType.FOOTSTEPS,
                    direction = RelativePosition.BEHIND,
                    volumeDb = volumeDb
                )
            }
            else -> null
        }
    }
}
