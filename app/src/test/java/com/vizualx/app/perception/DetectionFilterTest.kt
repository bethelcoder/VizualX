package com.vizualx.app.perception

import android.graphics.RectF
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Detection
import com.vizualx.app.perception.heuristics.DetectionFilter
import com.vizualx.app.perception.heuristics.ObjectCooldownTracker
import com.vizualx.app.perception.heuristics.ProcessedObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Optional

class DetectionFilterTest {

    private fun createDetection(
        label: String,
        score: Float,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Detection {
        val category = Category.create(score, 0, label, label)
        val rect = RectF().apply {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }
        return Detection.create(listOf(category), rect, Optional.empty())
    }

    @Test
    fun `filterAndPrioritize discards detections below 0_65 confidence score`() {
        val lowConfDetection = createDetection(
            label = "chair",
            score = 0.60f, // Below 0.65 threshold
            left = 100f,
            top = 100f,
            right = 400f,
            bottom = 400f
        )
        val highConfDetection = createDetection(
            label = "table",
            score = 0.85f, // Above threshold
            left = 100f,
            top = 100f,
            right = 400f,
            bottom = 400f
        )

        val results = DetectionFilter.filterAndPrioritize(
            detections = listOf(lowConfDetection, highConfDetection),
            frameWidth = 640,
            frameHeight = 480
        )

        assertEquals(1, results.size)
        assertEquals("Table", results[0].label)
    }

    @Test
    fun `filterAndPrioritize discards objects occupying less than 12 percent of frame`() {
        // Frame area = 640 * 480 = 307,200 px
        // 12% frame area = ~36,864 px (e.g. 192 x 192)

        // Small object: 100 x 100 = 10,000 px (< 3.3% of frame)
        val tinyDetection = createDetection(
            label = "bench",
            score = 0.90f,
            left = 200f,
            top = 200f,
            right = 300f,
            bottom = 300f
        )

        // Large close object: 300 x 300 = 90,000 px (~29.3% of frame)
        val largeDetection = createDetection(
            label = "chair",
            score = 0.90f,
            left = 170f,
            top = 90f,
            right = 470f,
            bottom = 390f
        )

        val results = DetectionFilter.filterAndPrioritize(
            detections = listOf(tinyDetection, largeDetection),
            frameWidth = 640,
            frameHeight = 480
        )

        assertEquals(1, results.size)
        assertEquals("Chair", results[0].label)
        assertTrue("Relative distance should be close", results[0].relativeDistance <= 2.5f)
    }

    @Test
    fun `filterAndPrioritize ranks closest and most central objects highest`() {
        // Object 1: Peripheral left (centerX ~ 0.15), area 20%
        val peripheralLeft = createDetection(
            label = "pole",
            score = 0.80f,
            left = 0f,
            top = 100f,
            right = 200f,
            bottom = 400f
        )

        // Object 2: Dead center (centerX = 0.5), large area 35%
        val centerObstacle = createDetection(
            label = "bollard",
            score = 0.92f,
            left = 160f,
            top = 60f,
            right = 480f,
            bottom = 420f
        )

        // Object 3: Moderate center-right (centerX = 0.6), area 15%
        val rightObstacle = createDetection(
            label = "person",
            score = 0.88f,
            left = 320f,
            top = 100f,
            right = 500f,
            bottom = 380f
        )

        val results = DetectionFilter.filterAndPrioritize(
            detections = listOf(peripheralLeft, centerObstacle, rightObstacle),
            frameWidth = 640,
            frameHeight = 480
        )

        // Center large obstacle must be ranked 1st
        assertEquals("Bollard", results[0].label)
    }

    @Test
    fun `filterAndPrioritize limits output to maximum 3 items`() {
        val detections = (1..6).map { idx ->
            createDetection(
                label = "obstacle_$idx",
                score = 0.85f,
                left = 100f + (idx * 20),
                top = 50f,
                right = 400f + (idx * 20),
                bottom = 400f
            )
        }

        val results = DetectionFilter.filterAndPrioritize(
            detections = detections,
            frameWidth = 640,
            frameHeight = 480
        )

        assertTrue(results.size <= 3)
    }

    @Test
    fun `cooldown tracker suppresses repeated objects within 5000ms window`() {
        val tracker = ObjectCooldownTracker(cooldownMs = 5000L)
        val initialTimestamp = 10_000L

        // Initially eligible
        assertTrue(tracker.isEligible("chair", initialTimestamp))

        // Record spoken
        tracker.recordSpoken("chair", initialTimestamp)

        // After 2 seconds (still inside 5s cooldown) -> NOT eligible
        assertFalse(tracker.isEligible("chair", initialTimestamp + 2000L))

        // Different object -> eligible
        assertTrue(tracker.isEligible("person", initialTimestamp + 2000L))

        // After 5001 ms -> eligible again
        assertTrue(tracker.isEligible("chair", initialTimestamp + 5001L))
    }

    @Test
    fun `deriveRelativeDistanceMeters calculates inverse area scaling`() {
        val closeDistance = DetectionFilter.deriveRelativeDistanceMeters(0.49f) // sqrt(0.49) = 0.7 -> ~1.4m
        val farDistance = DetectionFilter.deriveRelativeDistanceMeters(0.12f)   // sqrt(0.12) = 0.346 -> ~2.8m

        assertTrue("Closer object should have smaller distance value", closeDistance < farDistance)
        assertEquals(1.4f, closeDistance, 0.1f)
        assertEquals(2.8f, farDistance, 0.2f)
    }
}
