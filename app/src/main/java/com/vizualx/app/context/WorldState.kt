package com.vizualx.app.context

import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.Observation
import com.vizualx.app.perception.models.ObservationSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

data class WorldStateSnapshot(
    val timestampMs: Long = System.currentTimeMillis(),
    val frontObservations: List<ObjectObservation> = emptyList(),
    val rearObservations: List<ObjectObservation> = emptyList(),
    val audioObservations: List<AudioObservation> = emptyList(),
    val locationContext: String = "Campus Pedestrian Zone"
)

class WorldState(
    private val ttlMillis: Long = 8000L
) {
    private val observations = ConcurrentHashMap<String, Observation>()
    private val _snapshot = MutableStateFlow(WorldStateSnapshot())
    val snapshot: StateFlow<WorldStateSnapshot> = _snapshot.asStateFlow()

    fun updateObservation(observation: Observation) {
        pruneExpired()
        observations[observation.id] = observation
        publishSnapshot()
    }

    fun getActiveObservations(): List<Observation> {
        pruneExpired()
        return observations.values.toList()
    }

    fun pruneExpired(now: Long = System.currentTimeMillis()) {
        val cutoff = now - ttlMillis
        observations.entries.removeIf { it.value.timestampMs < cutoff }
    }

    fun clear() {
        observations.clear()
        _snapshot.value = WorldStateSnapshot()
    }

    private fun publishSnapshot() {
        val now = System.currentTimeMillis()
        val all = observations.values.toList()

        val front = all.filterIsInstance<ObjectObservation>()
            .filter { it.source == ObservationSource.FRONT_CAMERA || it.source == ObservationSource.REAR_CAMERA }
            .filter { it.source == ObservationSource.REAR_CAMERA } // Phone rear camera is "Ahead"
        val rear = all.filterIsInstance<ObjectObservation>()
            .filter { it.source == ObservationSource.FRONT_CAMERA } // Phone front camera faces user/behind
        val audio = all.filterIsInstance<AudioObservation>()

        _snapshot.value = WorldStateSnapshot(
            timestampMs = now,
            frontObservations = front,
            rearObservations = rear,
            audioObservations = audio
        )
    }
}
