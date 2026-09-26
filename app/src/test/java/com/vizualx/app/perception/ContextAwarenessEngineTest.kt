package com.vizualx.app.perception

import com.vizualx.app.context.ContextEngine
import com.vizualx.app.context.ContextEvent
import com.vizualx.app.context.EventPriority
import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.heuristics.ContextAwarenessEngine
import com.vizualx.app.perception.heuristics.TrackedDetection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextAwarenessEngineTest {

    private lateinit var worldState: WorldState
    private lateinit var contextEngine: ContextEngine
    private lateinit var awarenessEngine: ContextAwarenessEngine
    private val emittedEvents = mutableListOf<ContextEvent>()

    @Before
    fun setUp() {
        worldState = WorldState()
        contextEngine = ContextEngine(worldState)
        emittedEvents.clear()
        awarenessEngine = ContextAwarenessEngine(
            worldState = worldState,
            contextEngine = contextEngine,
            onHazardDetected = { event ->
                emittedEvents.add(event)
            }
        )
    }

    @Test
    fun `tracked detection requires at least 2 confirmed frames to confirm entity`() {
        val entity = TrackedDetection(label = "car")

        // Frame 1
        entity.confidenceWindow.add(0.85f)
        assertFalse("Entity should not be confirmed with only 1 frame", entity.isConfirmed)

        // Frame 2
        entity.confidenceWindow.add(0.92f)
        assertTrue("Entity should be confirmed after 2 consecutive frames", entity.isConfirmed)
    }

    @Test
    fun `tracked detection detects bounding box expansion rate`() {
        val entity = TrackedDetection(label = "car")

        // Expanding bounding box area over time
        entity.areaHistory.addAll(listOf(0.05f, 0.08f, 0.12f))
        assertTrue("Entity should detect expansion", entity.isExpanding)
    }
}
