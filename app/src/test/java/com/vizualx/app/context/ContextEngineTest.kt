package com.vizualx.app.context

import com.vizualx.app.perception.models.AudioObservation
import com.vizualx.app.perception.models.ObjectObservation
import com.vizualx.app.perception.models.ObjectType
import com.vizualx.app.perception.models.ObservationSource
import com.vizualx.app.perception.models.RelativePosition
import com.vizualx.app.perception.models.SoundType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ContextEngineTest {

    private lateinit var worldState: WorldState
    private lateinit var contextEngine: ContextEngine

    @Before
    fun setUp() {
        worldState = WorldState()
        contextEngine = ContextEngine(worldState)
    }

    @Test
    fun `vehicle observation produces CRITICAL priority event with alert speech`() {
        val obs = ObjectObservation(
            id = "vehicle-1",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.95f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.VEHICLE,
            position = RelativePosition.RIGHT,
            approximateDistanceMeters = 4.0f
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.CRITICAL, event.priority)
        assertNotNull(event.spokenText)
        assertEquals("Stop immediately. Vehicle moving ahead.", event.spokenText)
    }

    @Test
    fun `stairs down observation produces CRITICAL priority`() {
        val obs = ObjectObservation(
            id = "stairs-1",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.88f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.STAIRS_DOWN,
            position = RelativePosition.AHEAD
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.CRITICAL, event.priority)
        assertEquals("Stop. Descending stairs ahead, check cane.", event.spokenText)
    }

    @Test
    fun `ambient tree observation produces LOW priority and remains silent`() {
        val obs = ObjectObservation(
            id = "tree-1",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.90f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.TREE,
            position = RelativePosition.LEFT
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.LOW, event.priority)
        assertNull(event.spokenText)
    }

    @Test
    fun `urgent horn sound produces CRITICAL priority event`() {
        val obs = AudioObservation(
            id = "audio-horn-1",
            confidence = 0.92f,
            timestampMs = System.currentTimeMillis(),
            soundType = SoundType.HORN,
            direction = RelativePosition.AHEAD,
            volumeDb = 82f
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.CRITICAL, event.priority)
        assertNotNull(event.spokenText)
        assertEquals("Emergency siren nearby. Stop and stand clear.", event.spokenText)
    }

    @Test
    fun `obstacle directly ahead within 2m produces CRITICAL priority and path alert`() {
        val obs = ObjectObservation(
            id = "chair-1",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.85f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.OBSTACLE,
            position = RelativePosition.AHEAD,
            approximateDistanceMeters = 1.5f,
            label = "Chair"
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.CRITICAL, event.priority)
        assertNotNull(event.spokenText)
        assertEquals("Obstacle close. Slow down, sweep cane.", event.spokenText)
    }

    @Test
    fun `obstacle on side remains silent and has LOW priority`() {
        val obs = ObjectObservation(
            id = "bench-left",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.85f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.OBSTACLE,
            position = RelativePosition.LEFT,
            approximateDistanceMeters = 3.5f,
            label = "Bench"
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.LOW, event.priority)
        assertNull("Side obstacles should not produce speech alerts", event.spokenText)
    }

    @Test
    fun `person on side and far away remains silent`() {
        val obs = ObjectObservation(
            id = "person-right",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.88f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.PERSON,
            position = RelativePosition.RIGHT,
            approximateDistanceMeters = 4.0f,
            label = "Person"
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.LOW, event.priority)
        assertNull("Side persons at distance should remain silent", event.spokenText)
    }

    @Test
    fun `person directly ahead produces spoken alert`() {
        val obs = ObjectObservation(
            id = "person-ahead",
            source = ObservationSource.REAR_CAMERA,
            confidence = 0.88f,
            timestampMs = System.currentTimeMillis(),
            type = ObjectType.PERSON,
            position = RelativePosition.AHEAD,
            approximateDistanceMeters = 2.0f,
            label = "Person"
        )

        val event = contextEngine.evaluate(obs)

        assertEquals(EventPriority.NORMAL, event.priority)
        assertNotNull(event.spokenText)
        assertEquals("Pedestrian ahead. Proceed cautiously.", event.spokenText)
    }
}
