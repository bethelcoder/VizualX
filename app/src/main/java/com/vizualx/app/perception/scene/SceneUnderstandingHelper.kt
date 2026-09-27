package com.vizualx.app.perception.scene

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vizualx.app.perception.models.ObjectType

data class SceneInsight(
    val detectedType: ObjectType,
    val label: String,
    val confidence: Float,
    val isHandOrSelf: Boolean = false,
    val detectedText: String? = null
)

/**
 * Scene Understanding Helper using ML Kit Image Labeling (400+ concepts including Door, Stairs, Hallway, Hand)
 * and ML Kit Text Recognition for reading signs.
 */
class SceneUnderstandingHelper {

    companion object {
        private const val TAG = "SceneUnderstanding"
    }

    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.50f)
            .build()
    )

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private var lastSceneAnalysisTime = 0L
    private val sceneThrottleMs = 350L // ~3 FPS for scene & OCR classification

    fun analyzeScene(
        inputImage: InputImage,
        onSceneInsight: (SceneInsight) -> Unit
    ) {
        val now = System.currentTimeMillis()
        if (now - lastSceneAnalysisTime < sceneThrottleMs) {
            return
        }
        lastSceneAnalysisTime = now

        // 1. Process Scene Labels (Doors, Stairs)
        labeler.process(inputImage)
            .addOnSuccessListener { labels ->
                var foundDoor = false
                var foundStairs = false
                var primaryDoorLabel = "Doorway"
                var primaryStairsLabel = "Stairs"
                var maxDoorConf = 0f
                var maxStairsConf = 0f

                for (label in labels) {
                    val text = label.text.lowercase()
                    val conf = label.confidence

                    if (text.contains("door") || text.contains("doorway") || text.contains("entrance") || 
                        text.contains("gate") || text.contains("portal") || text.contains("archway")) {
                        foundDoor = true
                        if (conf > maxDoorConf) {
                            maxDoorConf = conf
                            primaryDoorLabel = label.text
                        }
                    }
                    if (text.contains("stair") || text.contains("steps") || text.contains("staircase") || 
                        text.contains("escalator") || text.contains("stairway")) {
                        foundStairs = true
                        if (conf > maxStairsConf) {
                            maxStairsConf = conf
                            primaryStairsLabel = label.text
                        }
                    }
                }

                if (foundDoor) {
                    onSceneInsight(
                        SceneInsight(
                            detectedType = ObjectType.DOOR,
                            label = primaryDoorLabel.ifBlank { "Doorway" },
                            confidence = maxDoorConf.coerceAtLeast(0.70f)
                        )
                    )
                } else if (foundStairs) {
                    onSceneInsight(
                        SceneInsight(
                            detectedType = ObjectType.STAIRS_DOWN,
                            label = primaryStairsLabel.ifBlank { "Stairs" },
                            confidence = maxStairsConf.coerceAtLeast(0.70f)
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "ML Kit labeler error: ${e.message}")
            }

        // 2. Process Text Recognition for Campus Signs / Door Plaques / Room numbers
        textRecognizer.process(inputImage)
            .addOnSuccessListener { textResult ->
                val fullText = textResult.text.trim().replace("\n", " ")
                if (fullText.length in 3..50) {
                    val upper = fullText.uppercase()
                    if (upper.contains("EXIT") || upper.contains("STAIR") || upper.contains("ENTRANCE") || 
                        upper.contains("ROOM") || upper.contains("LAB") || upper.contains("LIBRARY") || 
                        upper.contains("OFFICE") || upper.contains("RESTROOM") || upper.contains("CAFE") || 
                        upper.contains("EMERGENCY") || upper.contains("PULL") || upper.contains("PUSH") ||
                        upper.contains("NO ENTRY") || upper.contains("CAUTION") || upper.contains("DANGER") ||
                        upper.contains("ELEVATOR") || upper.contains("FLOOR") || upper.contains("LEVEL") ||
                        upper.contains("HALL") || upper.contains("BUILDING") || upper.contains("WAY OUT")) {
                        
                        onSceneInsight(
                            SceneInsight(
                                detectedType = ObjectType.SIGN,
                                label = "Sign: $fullText",
                                confidence = 0.88f,
                                detectedText = fullText
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "ML Kit OCR error: ${e.message}")
            }
    }

    fun close() {
        try {
            labeler.close()
            textRecognizer.close()
        } catch (_: Exception) {}
    }
}
