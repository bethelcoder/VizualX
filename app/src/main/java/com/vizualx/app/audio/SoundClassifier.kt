package com.vizualx.app.audio

import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.perception.models.SoundType
import java.util.UUID

interface SoundClassifier {
    fun classify(audioBuffer: ShortArray, volumeDb: Float): AudioObservation?
}

class BaselineSoundClassifier : SoundClassifier {

    override fun classify(audioBuffer: ShortArray, volumeDb: Float): AudioObservation? {
        // Speculative heuristic sound classification is disabled to prevent phantom hallucinations
        // (e.g. ambient room noise falsely triggering 'footsteps behind you' or 'vehicle engine').
        // The audio capture pipeline continues monitoring live dB levels for UI monitoring,
        // while remaining silent on the speech channel until a deterministic on-device audio model
        // (such as YAMNet TFLite) is integrated.
        return null
    }
}
