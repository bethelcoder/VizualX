package com.vizualx.app.perception.mediapipe

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Custom CameraX ImageAnalysis.Analyzer that converts camera frames to MediaPipe MPImage
 * and safely ensures ImageProxy closure under all execution conditions.
 */
class CameraFrameAnalyzer(
    private val detectorHelper: MediaPipeObjectDetectorHelper,
    private val frameThrottleMs: Long = 200L, // 5 FPS for thermal efficiency
    private val isFrontCamera: Boolean = false
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L

    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        try {
            if (currentTimestamp - lastAnalyzedTimestamp >= frameThrottleMs) {
                lastAnalyzedTimestamp = currentTimestamp
                val rotation = imageProxy.imageInfo.rotationDegrees
                val bitmap = imageProxy.toBitmap()
                detectorHelper.detectLiveStream(bitmap, rotation, isFrontCamera)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // ALWAYS close the ImageProxy so CameraX backpressure does not drop subsequent frames or lock up
            imageProxy.close()
        }
    }
}
