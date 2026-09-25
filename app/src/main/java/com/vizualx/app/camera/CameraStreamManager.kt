package com.vizualx.app.camera

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ConcurrentCamera.SingleCameraConfig
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class ActiveCameraMode {
    DUAL_CONCURRENT,
    REAR_AHEAD,
    FRONT_BEHIND
}

class CameraStreamManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var cameraExecutor: ExecutorService = Executors.newFixedThreadPool(2)
    private var cameraProvider: ProcessCameraProvider? = null
    var activeMode: ActiveCameraMode = ActiveCameraMode.DUAL_CONCURRENT
        private set

    fun initialize(onReady: () -> Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            cameraProvider = future.get()
            onReady()
        }, ContextCompat.getMainExecutor(context))
    }

    fun isConcurrentCameraSupported(): Boolean {
        val provider = cameraProvider ?: return false
        return try {
            provider.availableConcurrentCameraInfos.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    fun startDualConcurrentCameras(
        rearPreviewView: PreviewView?,
        frontPreviewView: PreviewView?,
        onRearFrame: ((ImageProxy) -> Unit)? = null,
        onFrontFrame: ((ImageProxy) -> Unit)? = null,
        onFallbackToSingle: (() -> Unit)? = null
    ): Boolean {
        val provider = cameraProvider ?: return false
        provider.unbindAll()

        val rearSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val frontSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()

        // Check if concurrent cameras are advertised by hardware
        val supportsConcurrent = isConcurrentCameraSupported()

        if (supportsConcurrent) {
            try {
                // Build Rear UseCaseGroup
                val rearGroupBuilder = UseCaseGroup.Builder()
                rearPreviewView?.let {
                    val rearPreview = Preview.Builder().build().also { p ->
                        p.setSurfaceProvider(it.surfaceProvider)
                    }
                    rearGroupBuilder.addUseCase(rearPreview)
                }
                onRearFrame?.let { callback ->
                    val rearAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { img ->
                                callback(img)
                                img.close()
                            }
                        }
                    rearGroupBuilder.addUseCase(rearAnalysis)
                }

                // Build Front UseCaseGroup
                val frontGroupBuilder = UseCaseGroup.Builder()
                frontPreviewView?.let {
                    val frontPreview = Preview.Builder().build().also { p ->
                        p.setSurfaceProvider(it.surfaceProvider)
                    }
                    frontGroupBuilder.addUseCase(frontPreview)
                }
                onFrontFrame?.let { callback ->
                    val frontAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(640, 480))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { img ->
                                callback(img)
                                img.close()
                            }
                        }
                    frontGroupBuilder.addUseCase(frontAnalysis)
                }

                val rearConfig = SingleCameraConfig(
                    rearSelector,
                    rearGroupBuilder.build(),
                    lifecycleOwner
                )
                val frontConfig = SingleCameraConfig(
                    frontSelector,
                    frontGroupBuilder.build(),
                    lifecycleOwner
                )

                provider.bindToLifecycle(listOf(rearConfig, frontConfig))
                activeMode = ActiveCameraMode.DUAL_CONCURRENT
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                // If concurrent binding threw (e.g. UnsupportedOperationException or ISP limit), fallback to primary rear
            }
        }

        // Graceful fallback to primary single camera if hardware doesn't support dual concurrent streams
        onFallbackToSingle?.invoke()
        startSingleCamera(rearPreviewView, CameraSelector.LENS_FACING_BACK, onRearFrame)
        return false
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
                        callback(imageProxy)
                        imageProxy.close()
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
