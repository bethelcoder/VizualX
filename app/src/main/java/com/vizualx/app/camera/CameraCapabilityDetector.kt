package com.vizualx.app.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build

data class CameraInfo(
    val id: String,
    val facing: String,
    val sensorOrientation: Int,
    val resolutions: List<String>
)

data class DeviceCapabilityReport(
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val totalCameras: Int,
    val cameras: List<CameraInfo>,
    val rearCameraId: String?,
    val frontCameraId: String?,
    val hasConcurrentCameraSupport: Boolean,
    val concurrentCameraCombinations: List<Set<String>>
)

class CameraCapabilityDetector(private val context: Context) {

    fun detectCapabilities(): DeviceCapabilityReport {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = try {
            cameraManager.cameraIdList
        } catch (e: Exception) {
            emptyArray<String>()
        }

        val cameraInfoList = mutableListOf<CameraInfo>()
        var rearId: String? = null
        var frontId: String? = null

        for (id in cameraIds) {
            try {
                val chars = cameraManager.getCameraCharacteristics(id)
                val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
                val facingStr = when (facingInt) {
                    CameraCharacteristics.LENS_FACING_BACK -> {
                        if (rearId == null) rearId = id
                        "REAR"
                    }
                    CameraCharacteristics.LENS_FACING_FRONT -> {
                        if (frontId == null) frontId = id
                        "FRONT"
                    }
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                    else -> "UNKNOWN"
                }

                val orientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val sizes = map?.getOutputSizes(android.graphics.ImageFormat.YUV_420_888)?.map {
                    "${it.width}x${it.height}"
                }?.take(4) ?: emptyList()

                cameraInfoList.add(
                    CameraInfo(
                        id = id,
                        facing = facingStr,
                        sensorOrientation = orientation,
                        resolutions = sizes
                    )
                )
            } catch (_: Exception) {}
        }

        val concurrentCombos = mutableListOf<Set<String>>()
        var supportsConcurrent = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val combos = cameraManager.concurrentCameraIds
                for (combo in combos) {
                    concurrentCombos.add(combo)
                }
                supportsConcurrent = concurrentCombos.isNotEmpty()
            } catch (e: Exception) {
                supportsConcurrent = false
            }
        }

        return DeviceCapabilityReport(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            totalCameras = cameraIds.size,
            cameras = cameraInfoList,
            rearCameraId = rearId,
            frontCameraId = frontId,
            hasConcurrentCameraSupport = supportsConcurrent,
            concurrentCameraCombinations = concurrentCombos
        )
    }
}
