package com.example

import com.example.camera.ToneCurveHelper
import com.example.model.CameraLens
import com.example.model.CaptureResolution
import com.example.model.ShootingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun verifyShootingModeDefaultIsAuto() {
        val defaultMode = ShootingMode.AUTO
        assertEquals("AUTO", defaultMode.code)
    }

    @Test
    fun verify200MpModeAnd10xZoom() {
        assertEquals(200, CameraLens.MAIN_WIDE.defaultMegapixels)
        val zoom10 = CameraLens.ZOOM_10X
        assertEquals(10.0f, zoom10.zoomFactor, 0.001f)
        assertEquals("10x", zoom10.label)

        val res200 = CaptureResolution.RES_200MP
        assertEquals(16320, res200.width)
        assertEquals(12240, res200.height)
    }

    @Test
    fun verifyNightExposureProcessingPreservesBytes() {
        val sampleBytes = byteArrayOf(1, 2, 3, 4, 5)
        val processed = ToneCurveHelper.processNightExposure(sampleBytes, 15)
        assertNotNull(processed)
        assertTrue(processed.isNotEmpty())
    }
}
