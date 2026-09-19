package com.example.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import com.example.model.DeviceDetectionInfo

object DeviceDetector {

    fun detectDevice(context: Context): DeviceDetectionInfo {
        val model = Build.MODEL ?: "Unknown"
        val manufacturer = Build.MANUFACTURER ?: "Unknown"
        val deviceCode = Build.DEVICE ?: "Unknown"
        val product = Build.PRODUCT ?: "Unknown"

        val isSamsung = manufacturer.equals("samsung", ignoreCase = true)
        val isS928 = model.contains("SM-S928", ignoreCase = true) ||
                model.contains("S928", ignoreCase = true) ||
                model.contains("S24 Ultra", ignoreCase = true) ||
                deviceCode.contains("e3q", ignoreCase = true) ||
                product.contains("e3q", ignoreCase = true)

        val isTargetS24Ultra = isSamsung && isS928

        var supportsRaw = false
        var supportsUltraHighRes = false
        var hardwareLevelName = "INFO_SUPPORTED_HARDWARE_LEVEL_FULL"
        var maxHardwareMp = if (isTargetS24Ultra) 200 else 12

        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraIds = cameraManager?.cameraIdList ?: emptyArray()

            for (id in cameraIds) {
                val chars = cameraManager?.getCameraCharacteristics(id) ?: continue
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
                    if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)) {
                        supportsRaw = true
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_ULTRA_HIGH_RESOLUTION_SENSOR)) {
                            supportsUltraHighRes = true
                            maxHardwareMp = 200
                        }
                    }

                    val level = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                    hardwareLevelName = when (level) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Nível 3 (PRO Manual RAW + YUV Reprocess)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Nível Full (Controle Manual de Sensor)"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Nível Limitado"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Nível Legacy"
                        else -> "Nível Avançado"
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        val sensorName = if (isTargetS24Ultra || supportsUltraHighRes) {
            "Samsung ISOCELL HP2 200MP (1/1.3\", 0.6µm, Tetra²pixel)"
        } else {
            "Sensor de Alta Resolução Compatível (Pro Mode)"
        }

        return DeviceDetectionInfo(
            isS24Ultra = isTargetS24Ultra,
            model = model,
            manufacturer = manufacturer,
            deviceCode = deviceCode,
            sensorName = sensorName,
            maxHardwareMegapixels = if (isTargetS24Ultra) 200 else maxHardwareMp.coerceAtLeast(12),
            supportsRaw = supportsRaw || isTargetS24Ultra,
            supportsUltraHighRes = supportsUltraHighRes || isTargetS24Ultra,
            hardwareLevelName = hardwareLevelName
        )
    }
}
