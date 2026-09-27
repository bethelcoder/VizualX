package com.vizualx.app.perception

import com.vizualx.app.perception.heuristics.PerceptionFilter
import com.vizualx.app.perception.models.RelativePosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptionFilterTest {

    @Test
    fun `calculatePosition partitions screen correctly into LEFT, AHEAD, and RIGHT`() {
        // Far left
        assertEquals(RelativePosition.LEFT, PerceptionFilter.calculatePosition(0.15f))
        assertEquals(RelativePosition.LEFT, PerceptionFilter.calculatePosition(0.34f))

        // Center corridor (active walking path)
        assertEquals(RelativePosition.AHEAD, PerceptionFilter.calculatePosition(0.35f))
        assertEquals(RelativePosition.AHEAD, PerceptionFilter.calculatePosition(0.50f))
        assertEquals(RelativePosition.AHEAD, PerceptionFilter.calculatePosition(0.65f))

        // Far right
        assertEquals(RelativePosition.RIGHT, PerceptionFilter.calculatePosition(0.66f))
        assertEquals(RelativePosition.RIGHT, PerceptionFilter.calculatePosition(0.90f))
    }

    @Test
    fun `isInWalkingPath verifies center third bounds`() {
        assertTrue(PerceptionFilter.isInWalkingPath(0.35f))
        assertTrue(PerceptionFilter.isInWalkingPath(0.50f))
        assertTrue(PerceptionFilter.isInWalkingPath(0.65f))

        assertFalse(PerceptionFilter.isInWalkingPath(0.10f))
        assertFalse(PerceptionFilter.isInWalkingPath(0.90f))
    }

    @Test
    fun `isCloseEnough verifies area and height ratio proximity thresholds`() {
        // High area (15% screen space)
        assertTrue(PerceptionFilter.isCloseEnough(areaRatio = 0.15f, heightRatio = 0.20f))

        // High height (40% screen space)
        assertTrue(PerceptionFilter.isCloseEnough(areaRatio = 0.05f, heightRatio = 0.40f))

        // Small/distant object (2% area, 10% height)
        assertFalse(PerceptionFilter.isCloseEnough(areaRatio = 0.02f, heightRatio = 0.10f))
    }

    @Test
    fun `isUrgentHazard identifies vehicles correctly`() {
        assertTrue(PerceptionFilter.isUrgentHazard("car"))
        assertTrue(PerceptionFilter.isUrgentHazard("bus"))
        assertTrue(PerceptionFilter.isUrgentHazard("truck"))
        assertTrue(PerceptionFilter.isUrgentHazard("motorcycle"))

        assertFalse(PerceptionFilter.isUrgentHazard("bench"))
        assertFalse(PerceptionFilter.isUrgentHazard("chair"))
        assertFalse(PerceptionFilter.isUrgentHazard("tree"))
    }

    @Test
    fun `shouldAlert filters background clutter and passes urgent hazards or path obstacles`() {
        // Stationary bench far left (X=0.1) -> should NOT alert
        assertFalse(
            PerceptionFilter.shouldAlert(
                centerX = 0.1f,
                areaRatio = 0.15f,
                heightRatio = 0.30f,
                label = "bench"
            )
        )

        // Stationary chair directly in walking path (X=0.5) and close -> should alert!
        assertTrue(
            PerceptionFilter.shouldAlert(
                centerX = 0.5f,
                areaRatio = 0.15f,
                heightRatio = 0.40f,
                label = "chair"
            )
        )

        // Car on side (X=0.8) -> urgent hazard -> should alert!
        assertTrue(
            PerceptionFilter.shouldAlert(
                centerX = 0.8f,
                areaRatio = 0.08f,
                heightRatio = 0.20f,
                label = "car"
            )
        )
    }
}
