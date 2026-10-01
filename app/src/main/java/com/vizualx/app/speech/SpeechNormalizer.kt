package com.vizualx.app.speech

import java.util.Locale

/**
 * Phonetic & Text Normalizer for Speech Synthesis and OCR Cleanup.
 *
 * Solves the critical problem where TextToSpeech engines spell out ALL-CAPS words
 * (e.g. "CAUTION", "EXIT", "STOP", "ROOM") letter-by-letter as acronyms.
 *
 * Normalization stages:
 * 1. Merges spaced-out OCR characters ("S T O P" -> "Stop").
 * 2. Cleans OCR noise glyphs (| , _ , ~ , ^ , { , } , \ , • , bullet points).
 * 3. Expands common building, navigational, and street abbreviations.
 * 4. Normalizes all-caps words to titlecase/lowercase so TTS pronounces words fluently.
 * 5. Expands currency and measurement units for natural voice output.
 */
object SpeechNormalizer {

    // Whitelist of acronyms that SHOULD be spelled letter-by-letter
    private val PRESERVED_ACRONYMS = setOf(
        "ATM", "GPS", "VIP", "SOS", "ID", "TV", "PIN", "USB", "AI", "LED", "AM", "PM", "USA", "UK"
    )

    // Standard street, building, and institutional abbreviations
    private val ABBREVIATIONS = mapOf(
        Regex("(?i)\\bSt\\b\\.?(?=\\s|$)") to "Street",
        Regex("(?i)\\bAve\\b\\.?(?=\\s|$)") to "Avenue",
        Regex("(?i)\\bBlvd\\b\\.?(?=\\s|$)") to "Boulevard",
        Regex("(?i)\\bRd\\b\\.?(?=\\s|$)") to "Road",
        Regex("(?i)\\bDr\\b\\.?(?=\\s|$)") to "Drive",
        Regex("(?i)\\bLn\\b\\.?(?=\\s|$)") to "Lane",
        Regex("(?i)\\bCt\\b\\.?(?=\\s|$)") to "Court",
        Regex("(?i)\\bPl\\b\\.?(?=\\s|$)") to "Place",
        Regex("(?i)\\bBldg\\b\\.?(?=\\s|$)") to "Building",
        Regex("(?i)\\bFlr?\\b\\.?(?=\\s|$)") to "Floor",
        Regex("(?i)\\bRm\\b\\.?(?=\\s|$)") to "Room",
        Regex("(?i)\\bApt\\b\\.?(?=\\s|$)") to "Apartment",
        Regex("(?i)\\bDept\\b\\.?(?=\\s|$)") to "Department",
        Regex("(?i)\\bPkg\\b\\.?(?=\\s|$)") to "Parking",
        Regex("(?i)\\bNo\\b\\.?(?=\\s*\\d)") to "Number",
        Regex("(?i)\\bHrs\\b\\.?(?=\\s|$)") to "Hours",
        Regex("(?i)\\bTel\\b\\.?(?=\\s|$)") to "Telephone",
        Regex("(?i)\\bPh\\b\\.?(?=\\s|$)") to "Phone",
        Regex("(?i)\\bExt\\b\\.?(?=\\s|$)") to "Extension",
        Regex("(?i)\\bMon-Fri\\b") to "Monday to Friday",
        Regex("(?i)\\bMon-Sat\\b") to "Monday to Saturday",
        Regex("(?i)\\b24/7\\b") to "twenty-four seven"
    )

    /**
     * Primary normalization function for all TTS and voice output.
     */
    fun normalizeForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText.trim()

        // 1. Remove OCR noise symbols and non-pronounceable glyphs
        text = text.replace(Regex("[|~_^\\{\\}\\\\\\[\\]<>•▪►★☆©®™]+"), " ")

        // 2. Fix broken hyphenated line breaks (e.g. "emer-\ngency" -> "emergency")
        text = text.replace(Regex("(\\w+)-\\s+(\\w+)"), "$1$2")

        // 3. Collapse spaced-out letters into single words (e.g. "S T O P" -> "STOP", "E X I T" -> "EXIT")
        text = collapseSpacedLetters(text)

        // 4. Expand abbreviations to full spoken words
        for ((regex, replacement) in ABBREVIATIONS) {
            text = text.replace(regex, replacement)
        }

        // 5. Expand currency symbols
        text = text.replace(Regex("\\$(\\d+)(?:\\.(\\d{2}))?")) { match ->
            val dollars = match.groupValues[1]
            val cents = match.groupValues.getOrNull(2)
            if (cents.isNullOrBlank() || cents == "00") {
                "$dollars dollars"
            } else {
                "$dollars dollars and $cents cents"
            }
        }

        // 6. Convert ALL-CAPS words to TitleCase so Android TTS doesn't spell them as acronyms
        text = normalizeCapitalization(text)

        // 7. Clean up multiple spaces and trailing punctuation
        text = text.replace(Regex("\\s+"), " ").trim()

        return text
    }

    /**
     * Merges spaced out letters such as "S T O P" -> "STOP", "E X I T" -> "EXIT"
     */
    private fun collapseSpacedLetters(input: String): String {
        // Match sequence of single letters separated by space (min 2 letters)
        val spacedLetterPattern = Regex("(?<![A-Za-z])([A-Za-z](?:\\s+[A-Za-z]){1,7})(?![A-Za-z])")
        return spacedLetterPattern.replace(input) { match ->
            val sequence = match.value
            // If the sequence is single-character words separated by space, join them
            val collapsed = sequence.replace(Regex("\\s+"), "")
            // Avoid collapsing single "a" or "I" if followed by real words
            if (collapsed.length >= 2) collapsed else sequence
        }
    }

    /**
     * Normalizes uppercase words into TitleCase/LowerCase.
     * Prevents TTS engines from pronouncing words like "CAUTION", "EXIT", "STOP", "ROOM"
     * as "C-A-U-T-I-O-N", "E-X-I-T", etc.
     */
    private fun normalizeCapitalization(input: String): String {
        val words = input.split(Regex("(?<=\\s)|(?=\\s)"))

        return words.joinToString("") { word ->
            val cleanWord = word.trim()
            if (cleanWord.length > 1 && cleanWord.all { it.isUpperCase() || !it.isLetter() }) {
                // If it's a preserved acronym (e.g. ATM, GPS, VIP, SOS), leave it in uppercase
                if (PRESERVED_ACRONYMS.contains(cleanWord.filter { it.isLetter() })) {
                    cleanWord
                } else {
                    // Convert to TitleCase (e.g. "CAUTION" -> "Caution", "EXIT" -> "Exit")
                    cleanWord.lowercase(Locale.US).replaceFirstChar { it.titlecase(Locale.US) }
                }
            } else {
                word
            }
        }
    }
}
