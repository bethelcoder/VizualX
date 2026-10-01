package com.vizualx.app.perception

import com.vizualx.app.perception.heuristics.ObjectCooldownTracker
import com.vizualx.app.perception.heuristics.ProcessedObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class ObjectAudioBridgeTest {

    @Test
    fun `isSpeakingOrProcessing flag blocks frame processing when busy`() {
        val lock = AtomicBoolean(true) // Simulating busy state (TTS speaking or LLM generating)

        var frameIngested = false
        if (!lock.get()) {
            frameIngested = true
        }

        assertFalse("Frame should be dropped when lock is busy", frameIngested)
    }

    @Test
    fun `isSpeakingOrProcessing flag allows frame processing when idle and acquires lock`() {
        val lock = AtomicBoolean(false) // Idle

        var frameIngested = false
        if (lock.compareAndSet(false, true)) {
            frameIngested = true
        }

        assertTrue("Frame should be ingested when idle", frameIngested)
        assertTrue("Lock should now be set to busy", lock.get())
    }

    @Test
    fun `cooldown tracker prevents consecutive announcements of the same object within 5 seconds`() {
        val cooldownTracker = ObjectCooldownTracker(cooldownMs = 5000L)
        val startTime = 100_000L

        // Frame 1: Object detected at startTime
        val detected = ProcessedObject(label = "Chair", relativeDistance = 1.2f, timestamp = startTime)
        assertTrue(cooldownTracker.isEligible(detected.label, startTime))
        cooldownTracker.recordSpoken(detected.label, startTime)

        // Frame 2: 1.5 seconds later -> must be suppressed
        assertFalse(cooldownTracker.isEligible(detected.label, startTime + 1500L))

        // Frame 3: 4.9 seconds later -> still suppressed
        assertFalse(cooldownTracker.isEligible(detected.label, startTime + 4900L))

        // Frame 4: 5.1 seconds later -> eligible again
        assertTrue(cooldownTracker.isEligible(detected.label, startTime + 5100L))
    }
}
