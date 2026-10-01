package com.vizualx.app.ai

import com.vizualx.app.perception.heuristics.ProcessedObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLlmBrainTest {

    @Test
    fun `formatSystemPrompt matches exact required system prompt specification`() {
        val objects = listOf(
            ProcessedObject(label = "Chair", relativeDistance = 1.2f, areaRatio = 0.35f, centerX = 0.5f),
            ProcessedObject(label = "Person", relativeDistance = 2.4f, areaRatio = 0.18f, centerX = 0.6f)
        )

        val prompt = LocalLlmBrain.formatSystemPrompt(objects)

        assertTrue(
            "Prompt must contain required system prompt start",
            prompt.startsWith("You are the mobility navigator for a blind user. Convert detected obstacles ahead into one immediate, directive action command under 8 words.")
        )
        assertTrue(
            "Prompt must contain instruction forbidding left/right and demanding action",
            prompt.contains("Never say left or right. Tell the user exactly what physical action to take")
        )
        assertTrue(
            "Prompt must format detected objects with distance",
            prompt.contains("Chair at 1 meter ahead, Person at 2 meters ahead")
        )
    }

    @Test
    fun `sanitizeResponse strips prefixes quotes and truncates to under 8 words`() {
        val rawWithPrefix = "Action: \"Obstacle close, slow down and sweep cane immediately now!\""
        val sanitized = LocalLlmBrain.sanitizeResponse(rawWithPrefix)

        val words = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        assertTrue("Sanitized response must be under or equal to 8 words (got ${words.size})", words.size <= 8)
        assertFalse("Quotes should be stripped", sanitized.contains("\""))
        assertFalse("Action prefix should be stripped", sanitized.lowercase().startsWith("action:"))
    }

    @Test
    fun `generateDeterministicFallback returns immediate directive action command`() {
        val objects = listOf(
            ProcessedObject(label = "Chair", relativeDistance = 1.2f, areaRatio = 0.35f, centerX = 0.5f),
            ProcessedObject(label = "Bicycle", relativeDistance = 2.8f, areaRatio = 0.15f, centerX = 0.7f)
        )

        val fallback = LocalLlmBrain.generateDeterministicFallback(objects)
        assertEquals("Obstacle close. Slow down, sweep cane.", fallback)
    }
}
