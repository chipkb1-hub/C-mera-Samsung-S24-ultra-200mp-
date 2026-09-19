package com.example.camera

import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import com.example.model.ColorProfile
import kotlin.math.ln
import kotlin.math.pow

object ToneCurveHelper {

    /**
     * Converts a correlated color temperature (Kelvin) to RGB gains
     * suitable for Camera2 CaptureRequest.COLOR_CORRECTION_GAINS.
     */
    fun kelvinToRggbGains(kelvin: Int): RggbChannelVector {
        val temp = (kelvin / 100.0).coerceIn(20.0, 100.0)

        // Tanner-Helland color temperature to RGB approximation
        val r: Double = if (temp <= 66) {
            255.0
        } else {
            329.698727446 * (temp - 60).pow(-0.1332047592)
        }.coerceIn(0.0, 255.0)

        val g: Double = if (temp <= 66) {
            99.4708025861 * ln(temp) - 161.1195681661
        } else {
            288.1221695283 * (temp - 60).pow(-0.0755148492)
        }.coerceIn(0.0, 255.0)

        val b: Double = if (temp >= 66) {
            255.0
        } else if (temp <= 19) {
            0.0
        } else {
            138.5177312231 * ln(temp - 10) - 305.0447927307
        }.coerceIn(0.0, 255.0)

        // Convert RGB values relative to green channel for white balance gains
        val greenBase = g.coerceAtLeast(1.0)
        val redGain = (255.0 / r * (greenBase / 255.0) * 1.8).toFloat().coerceIn(1.0f, 4.0f)
        val blueGain = (255.0 / b * (greenBase / 255.0) * 1.8).toFloat().coerceIn(1.0f, 4.0f)
        val greenGain = 1.0f

        return RggbChannelVector(redGain, greenGain, greenGain, blueGain)
    }

    /**
     * Builds a TonemapCurve for the selected ColorProfile.
     * TonemapCurve requires pairs of (in, out) floats in [0, 1].
     */
    fun buildTonemapCurve(profile: ColorProfile): TonemapCurve {
        val steps = 32
        val curvePoints = FloatArray(steps * 2)

        for (i in 0 until steps) {
            val inVal = i.toFloat() / (steps - 1)
            val outVal = when (profile) {
                ColorProfile.FLAT_LOG -> {
                    // Logarithmic curve: lifts deep shadows and rolls off highlights gently
                    // y = ln(1 + 15 * x) / ln(16)
                    val a = 15.0
                    (ln(1.0 + a * inVal) / ln(1.0 + a)).toFloat().coerceIn(0f, 1f)
                }
                ColorProfile.VIVID -> {
                    // S-curve contrast boost
                    // Sigmoid curve around mid-tones
                    val x = inVal.toDouble()
                    (3.0 * x.pow(2.0) - 2.0 * x.pow(3.0)).toFloat().coerceIn(0f, 1f)
                }
                ColorProfile.MONOCHROME, ColorProfile.STANDARD -> {
                    // Standard linear/sRGB default
                    inVal
                }
            }
            curvePoints[i * 2] = inVal
            curvePoints[i * 2 + 1] = outVal
        }

        return TonemapCurve(curvePoints, curvePoints, curvePoints)
    }

    /**
     * Enhances low-light photo bytes simulating long shutter opening and photon accumulation.
     */
    fun processNightExposure(jpegBytes: ByteArray, durationSeconds: Int): ByteArray {
        if (durationSeconds <= 0) return jpegBytes
        try {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
            val mutableBmp = bmp.copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            bmp.recycle()

            val exposureGain = (1.1f + ln(1.0 + durationSeconds.toDouble()) * 0.45).toFloat().coerceIn(1.2f, 3.5f)
            val shadowLift = (durationSeconds.toFloat() * 1.5f).coerceIn(10f, 45f)

            val canvas = android.graphics.Canvas(mutableBmp)
            val paint = android.graphics.Paint()
            val colorMatrix = android.graphics.ColorMatrix(floatArrayOf(
                exposureGain, 0f, 0f, 0f, shadowLift,
                0f, exposureGain, 0f, 0f, shadowLift,
                0f, 0f, exposureGain, 0f, shadowLift,
                0f, 0f, 0f, 1f, 0f
            ))
            paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(mutableBmp, 0f, 0f, paint)

            val outStream = java.io.ByteArrayOutputStream()
            mutableBmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 96, outStream)
            mutableBmp.recycle()
            return outStream.toByteArray()
        } catch (e: Exception) {
            return jpegBytes
        }
    }
}
