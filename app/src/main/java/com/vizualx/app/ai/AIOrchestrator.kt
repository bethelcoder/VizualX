package com.vizualx.app.ai

import com.vizualx.app.context.WorldState
import com.vizualx.app.perception.models.ObjectObservation

sealed class AIQueryResult {
    data class TextResponse(val text: String) : AIQueryResult()
    data class NavigationRequest(val destinationQuery: String) : AIQueryResult()
    data object TriggerAssistiveReading : AIQueryResult()
}

class AIOrchestrator(
    private val worldState: WorldState
) {

    suspend fun processQuery(query: String): AIQueryResult {
        val lower = query.lowercase().trim()

        if (lower.contains("read this") || lower.contains("read text") || 
            lower.contains("read document") || lower.contains("what does this say") ||
            lower.contains("read sign") || lower.contains("read page") ||
            lower.contains("start reading") || lower.contains("assistive reading") ||
            lower.contains("read out") || lower.contains("read menu")) {
            return AIQueryResult.TriggerAssistiveReading
        }

        if (lower.startsWith("navigate to") ||
            lower.startsWith("take me to") ||
            lower.startsWith("directions to") ||
            lower.startsWith("how to get to") ||
            lower.startsWith("where is")
        ) {
            val destination = lower
                .replace(Regex("(?i)^(navigate to|take me to|directions to|how to get to|where is)\\s+"), "")
                .trim()
            if (destination.isNotBlank()) {
                return AIQueryResult.NavigationRequest(destination)
            }
        }

        val text = answerUserQuery(query)
        return AIQueryResult.TextResponse(text)
    }

    suspend fun answerUserQuery(query: String): String {
        val lower = query.lowercase().trim()
        val snapshot = worldState.snapshot.value

        return when {
            lower.contains("what's around") || lower.contains("what is around") || lower.contains("surroundings") -> {
                describeSurroundings(snapshot.frontObservations, snapshot.rearObservations)
            }
            lower.contains("building") || lower.contains("landmark") -> {
                val landmark = (snapshot.frontObservations + snapshot.rearObservations)
                    .firstOrNull { it.type == com.vizualx.app.perception.models.ObjectType.CAMPUS_LANDMARK }
                landmark?.label ?: "You are near the central campus walkway. No specific building sign is in clear view."
            }
            lower.contains("stairs") || lower.contains("steps") -> {
                val stairs = snapshot.frontObservations
                    .firstOrNull { it.type == com.vizualx.app.perception.models.ObjectType.STAIRS_DOWN || it.type == com.vizualx.app.perception.models.ObjectType.STAIRS_UP }
                if (stairs != null) {
                    "There are stairs ${stairs.label.lowercase()} approximately ${stairs.approximateDistanceMeters ?: 3} metres ahead."
                } else {
                    "No stairs detected directly in front of you."
                }
            }
            lower.contains("be quiet") || lower.contains("quiet") -> {
                "Entering quiet monitoring mode. I will only alert you for safety-critical hazards."
            }
            else -> {
                // Generative AI fallback response based on local world state
                "I am monitoring your path. ${describeSurroundings(snapshot.frontObservations, snapshot.rearObservations)}"
            }
        }
    }

    private fun describeSurroundings(front: List<ObjectObservation>, rear: List<ObjectObservation>): String {
        val items = mutableListOf<String>()
        if (front.isNotEmpty()) {
            val frontSummary = front.joinToString(", ") { "${it.label.lowercase()} ${it.position.name.lowercase()}" }
            items.add("Ahead: $frontSummary")
        }
        if (rear.isNotEmpty()) {
            val rearSummary = rear.joinToString(", ") { "${it.label.lowercase()} behind" }
            items.add("Behind: $rearSummary")
        }

        return if (items.isEmpty()) {
            "Path appears clear directly ahead in the campus pedestrian area."
        } else {
            items.joinToString(". ")
        }
    }
}
