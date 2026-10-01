package com.vizualx.app.speech

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeechNormalizerTest {

    @Test
    fun `normalizeForSpeech converts all-caps signs to titlecase to prevent letter spelling`() {
        val input = "CAUTION: WET FLOOR"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Caution: Wet Floor", output)
    }

    @Test
    fun `normalizeForSpeech converts exit and stop signs correctly`() {
        val input = "EMERGENCY EXIT ONLY"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Emergency Exit Only", output)
    }

    @Test
    fun `normalizeForSpeech collapses spaced letters into single whole words`() {
        val input = "S T O P ahead"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Stop ahead", output)
    }

    @Test
    fun `normalizeForSpeech expands street and room abbreviations`() {
        val input = "Rm. 102 on Main St. Bldg. 4"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Room 102 on Main Street Building 4", output)
    }

    @Test
    fun `normalizeForSpeech preserves valid acronyms like ATM and GPS and SOS`() {
        val input = "ATM and GPS ahead, SOS button"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("ATM and GPS ahead, SOS button", output)
    }

    @Test
    fun `normalizeForSpeech strips OCR noise symbols`() {
        val input = "|~ RESTROOM • [MEN] ~|"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Restroom Men", output)
    }

    @Test
    fun `normalizeForSpeech converts currency to spoken words`() {
        val input = "Total: $12.50"
        val output = SpeechNormalizer.normalizeForSpeech(input)
        assertEquals("Total: 12 dollars and 50 cents", output)
    }
}
