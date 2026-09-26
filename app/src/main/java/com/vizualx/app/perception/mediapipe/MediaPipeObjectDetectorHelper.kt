package com.vizualx.app.perception.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

/**
 * Production-grade MediaPipe Object Detector wrapper.
 * Runs in LIVE_STREAM mode with GPU acceleration (falling back to CPU).
 */
class MediaPipeObjectDetectorHelper(
    private val context: Context,
    private val threshold: Float = 0.50f,
    private val maxResults: Int = 5,
    private val modelName: String = "efficientdet_lite0.tflite",
    private val resultListener: (ObjectDetectorResult, MPImage) -> Unit,
    private val errorListener: (String) -> Unit
) {
    companion object {
        private const val TAG = "MediaPipeDetector"
    }

    private var objectDetector: ObjectDetector? = null
    var activeDelegate: Delegate = Delegate.GPU
        private set

    init {
        setupObjectDetector()
    }

    private fun setupObjectDetector() {
        // Attempt GPU first, then fallback to CPU if GPU delegate fails
        val successGpu = tryInitializeDetector(Delegate.GPU)
        if (!successGpu) {
            Log.w(TAG, "GPU delegate unavailable for MediaPipe. Falling back to CPU.")
            val successCpu = tryInitializeDetector(Delegate.CPU)
            if (!successCpu) {
                errorListener("Failed to initialize MediaPipe ObjectDetector on both GPU and CPU.")
            }
        }
    }

    private fun tryInitializeDetector(delegate: Delegate): Boolean {
        return try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath(modelName)
                .setDelegate(delegate)

            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, inputImage ->
                    resultListener(result, inputImage)
                }
                .setErrorListener { error ->
                    Log.e(TAG, "MediaPipe live stream error: ${error.message}")
                    errorListener(error.message ?: "Unknown MediaPipe error")
                }
                .build()

            objectDetector?.close()
            objectDetector = ObjectDetector.createFromOptions(context, options)
            activeDelegate = delegate
            Log.i(TAG, "MediaPipe ObjectDetector initialized successfully with $delegate")
            true
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe initialization failed with delegate $delegate: ${e.message}")
            false
        }
    }

    private var lastFrameTimeMs: Long = 0L

    /**
     * Converts a standalone in-memory Bitmap to an MPImage with correct rotation and dispatches asynchronously.
     * Guarantees strictly monotonic timestamps required by MediaPipe LIVE_STREAM mode.
     */
    fun detectLiveStream(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        isFrontCamera: Boolean = false
    ) {
        val detector = objectDetector ?: return

        try {
            var frameTime = SystemClock.uptimeMillis()
            if (frameTime <= lastFrameTimeMs) {
                frameTime = lastFrameTimeMs + 1
            }
            lastFrameTimeMs = frameTime

            val rotatedBitmap = if (rotationDegrees != 0 || isFrontCamera) {
                val matrix = Matrix().apply {
                    if (rotationDegrees != 0) {
                        postRotate(rotationDegrees.toFloat())
                    }
                    if (isFrontCamera) {
                        postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                }
                Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()
            detector.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame for MediaPipe: ${e.message}")
        }
    }

    fun close() {
        try {
            objectDetector?.close()
            objectDetector = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ObjectDetector: ${e.message}")
        }
    }
}
