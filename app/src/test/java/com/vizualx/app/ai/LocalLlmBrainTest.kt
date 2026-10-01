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
            prompt.startsWith("You are the auditory brain for a blind user. Summarize these detected objects into one short, active warning under 8 words.")
        )
        assertTrue(
            "Prompt must contain instruction for blind user",
            prompt.contains("Ignore background static. Tell them only what to avoid or what is directly ahead. Input objects:")
        )
        assertTrue(
            "Prompt must format detected objects with distance",
            prompt.contains("Chair at 1 meter ahead, Person at 2 meters ahead")
        )
    }

    @Test
    fun `sanitizeResponse strips prefixes quotes and truncates to under 8 words`() {
        val rawWithPrefix = "Warning: \"Caution, chair approaching quickly ahead on your active walking corridor path!\""
        val sanitized = LocalLlmBrain.sanitizeResponse(rawWithPrefix)

        val words = sanitized.split(Regex("\\s+")).filter { it.isNotBlank() }
        assertTrue("Sanitized response must be under or equal to 8 words (got ${words.size})", words.size <= 8)
        assertFalse("Quotes should be stripped", sanitized.contains("\""))
        assertFalse("Warning prefix should be stripped", sanitized.lowercase().startsWith("warning:"))
    }

    @Test
    fun `generateDeterministicFallback returns immediate safe active alert`() {
        val objects = listOf(
            ProcessedObject(label = "Chair", relativeDistance = 1.2f, areaRatio = 0.35f, centerX = 0.5f),
            ProcessedObject(label = "Bicycle", relativeDistance = 2.8f, areaRatio = 0.15f, centerX = 0.7f)
        )

        val fallback = LocalLlmBrain.generateDeterministicFallback(objects)
        assertEquals("Caution: Chair directly ahead.", fallback)
    }
}
