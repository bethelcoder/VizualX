package com.vizualx.app.perception

import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.perception.models.RelativePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldStateTest {

    @Test
    fun `world state retains active observations and prunes expired items`() {
        val mockTime = 10000L
        val worldState = WorldState(ttlMillis = 5000L, timeProvider = { mockTime })

        val recentObs = ObjectObservation(
            id = "obs-recent",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.9f,
            timestampMs = 8000L, // 2s old -> within 5s TTL
            type = ObjectType.PERSON,
            position = RelativePosition.AHEAD
        )

        val expiredObs = ObjectObservation(
            id = "obs-expired",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.8f,
            timestampMs = 3000L, // 7s old -> expired
            type = ObjectType.BENCH,
            position = RelativePosition.LEFT
        )

        worldState.updateObservation(recentObs, now = mockTime)
        worldState.updateObservation(expiredObs, now = mockTime)

        worldState.pruneExpired(now = mockTime)
        val active = worldState.getActiveObservations(now = mockTime)

        assertEquals(1, active.size)
        assertEquals("obs-recent", active.first().id)
    }

    @Test
    fun `clear resets all observations in world state`() {
        val worldState = WorldState()
        worldState.updateObservation(
            ObjectObservation(
                id = "obs-1",
                source = ObservationSource.REAR_CAMERA,
                confidence = 0.9f,
                timestampMs = System.currentTimeMillis(),
                type = ObjectType.PERSON,
                position = RelativePosition.AHEAD
            )
        )

        worldState.clear()
        assertTrue(worldState.getActiveObservations().isEmpty())
    }
}
