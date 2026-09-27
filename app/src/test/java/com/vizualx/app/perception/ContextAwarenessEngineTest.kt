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
    fun `tracked detection requires at least 3 frames with average confidence above 55 percent to confirm entity`() {
        val entity = TrackedDetection(label = "car")

        // Frame 1 and 2
        entity.confidenceWindow.add(0.85f)
        entity.confidenceWindow.add(0.90f)
        assertFalse("Entity should not be confirmed with only 2 frames", entity.isConfirmed)

        // Frame 3 (high confidence -> average > 0.55)
        entity.confidenceWindow.add(0.80f)
        assertTrue("Entity should be confirmed after 3 frames with average >= 0.55", entity.isConfirmed)
    }

    @Test
    fun `tracked detection rejects entity if average confidence is below threshold`() {
        val entity = TrackedDetection(label = "car")

        // 3 frames with low average confidence
        entity.confidenceWindow.addAll(listOf(0.40f, 0.45f, 0.48f))
        assertFalse("Entity should not be confirmed when average is below 0.55", entity.isConfirmed)
    }

    @Test
    fun `tracked detection detects bounding box expansion rate`() {
        val entity = TrackedDetection(label = "car")

        // Expanding bounding box area over time
        entity.areaHistory.addAll(listOf(0.05f, 0.08f, 0.12f))
        assertTrue("Entity should detect expansion", entity.isExpanding)
    }

    @Test
    fun `relevant labels whitelist contains essential safety and navigation classes`() {
        assertTrue(ContextAwarenessEngine.RELEVANT_LABELS.contains("person"))
        assertTrue(ContextAwarenessEngine.RELEVANT_LABELS.contains("car"))
        assertTrue(ContextAwarenessEngine.RELEVANT_LABELS.contains("chair"))
        assertFalse(ContextAwarenessEngine.RELEVANT_LABELS.contains("kite"))
        assertFalse(ContextAwarenessEngine.RELEVANT_LABELS.contains("wine glass"))
    }
}
