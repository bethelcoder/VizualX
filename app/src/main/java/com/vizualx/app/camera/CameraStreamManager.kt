package com.vizualx.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ActiveCameraMode {
    REAR_AHEAD,
    FRONT_BEHIND,
    DUAL_CONCURRENT
}

class CameraStreamManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null
    var activeMode: ActiveCameraMode = ActiveCameraMode.REAR_AHEAD
        private set

    fun initialize(onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun startSingleCamera(
        previewView: PreviewView?,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onFrameAnalyzed: ((ImageProxy) -> Unit)? = null
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = previewView?.let {
            Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        }

        val imageAnalysis = onFrameAnalyzed?.let { callback ->
            ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            callback(imageProxy)
                        } catch (e: Exception) {
                            android.util.Log.e("CameraStream", "Error in analyzer callback: ${e.message}")
                        } finally {
                            imageProxy.close()
                        }
                    }
                }
        }

        try {
            if (preview != null && imageAnalysis != null) {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } else if (preview != null) {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
            } else if (imageAnalysis != null) {
                provider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis)
            }

            activeMode = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                ActiveCameraMode.REAR_AHEAD
            } else {
                ActiveCameraMode.FRONT_BEHIND
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        cameraProvider?.unbindAll()
    }

    fun shutdown() {
        stop()
        cameraExecutor.shutdown()
    }
}
