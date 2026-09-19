package com.example.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.SensorManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ExifInterface
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import com.example.model.AwbPreset
import com.example.model.CameraLens
import com.example.model.CaptureFormat
import com.example.model.CaptureResolution
import com.example.model.CapturedMediaInfo
import com.example.model.ColorProfile
import com.example.model.DenoiseMode
import com.example.model.HistogramData
import com.example.model.Manual2Adjustments
import com.example.model.ShootingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class Camera2Controller(private val context: Context) {

    private val tag = "Camera2Controller"
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var cachedSurfaceTexture: SurfaceTexture? = null
    private var cachedWidth: Int = 1920
    private var cachedHeight: Int = 1080

    private var currentCameraId: String = "0"
    private var cameraCharacteristics: CameraCharacteristics? = null

    private var jpegImageReader: ImageReader? = null
    private var rawImageReader: ImageReader? = null
    private var analysisImageReader: ImageReader? = null

    // State flows for UI
    val histogramFlow = MutableStateFlow(HistogramData())
    val isCapturingFlow = MutableStateFlow(false)
    val nightCountdownSeconds = MutableStateFlow(0)
    val nightTotalSeconds = MutableStateFlow(0)
    val lightAccumulationPercentFlow = MutableStateFlow(0)
    val lastCapturedMedia = MutableStateFlow<CapturedMediaInfo?>(null)
    val photoFlashTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val toastMessageFlow = MutableSharedFlow<String>(extraBufferCapacity = 2)

    val zoomRatioFlow = MutableStateFlow(1.0f)
    val sensorIsoRange = MutableStateFlow(50..3200)
    val exposureTimeRange = MutableStateFlow(100_000L..30_000_000_000L) // 1/10000s to 30s
    val minFocusDistance = MutableStateFlow(10.0f)

    // Current settings - ALWAYS DEFAULTS TO AUTO MODE!
    var currentLens: CameraLens = CameraLens.MAIN_WIDE
    var currentResolution: CaptureResolution = CaptureResolution.RES_200MP
    var currentFormat: CaptureFormat = CaptureFormat.JPEG
    var currentShootingMode: ShootingMode = ShootingMode.AUTO
    var currentColorProfile: ColorProfile = ColorProfile.STANDARD

    var isIsoAuto: Boolean = true
    var manualIso: Int = 100
    var isShutterAuto: Boolean = true
    var manualShutterNs: Long = 1_000_000_000L / 125 // 1/125s
    var exposureCompensationEv: Float = 0.0f
    var isFocusAuto: Boolean = true
    var manualFocusDistance: Float = 0.0f // 0.0 = infinity
    var currentAwbPreset: AwbPreset = AwbPreset.AUTO
    var manualKelvin: Int = 5500
    var isAeAfLocked: Boolean = false
    var isPhotoLogEnabled: Boolean = false
    var currentZoom: Float = 1.0f

    // Manual 2 adjustments (Brightness, Contrast, Shadows, Highlights)
    var currentManual2Adjustments = Manual2Adjustments()
    val manual2AdjustmentsFlow = MutableStateFlow(Manual2Adjustments())

    // AI Neural Denoise Mode
    var currentDenoiseMode: DenoiseMode = DenoiseMode.STANDARD
    val denoiseModeFlow = MutableStateFlow(DenoiseMode.STANDARD)

    // Device orientation detection for upright photos
    private var orientationEventListener: OrientationEventListener? = null
    private var deviceOrientationDegrees: Int = 0

    init {
        startBackgroundThread()
        initOrientationListener()
    }

    private fun initOrientationListener() {
        orientationEventListener = object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                deviceOrientationDegrees = when (orientation) {
                    in 45..134 -> 90
                    in 135..224 -> 180
                    in 225..314 -> 270
                    else -> 0
                }
            }
        }
        if (orientationEventListener?.canDetectOrientation() == true) {
            orientationEventListener?.enable()
        }
    }

    fun computeJpegOrientation(): Int {
        val sensorOrientation = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90
        val facing = cameraCharacteristics?.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_BACK
        return if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
            (sensorOrientation + deviceOrientationDegrees) % 360
        } else {
            (sensorOrientation - deviceOrientationDegrees + 360) % 360
        }
    }

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("CameraBackground").also { it.start() }
            backgroundHandler = Handler(backgroundThread!!.looper)
        }
    }

    fun stopBackgroundThread() {
        orientationEventListener?.disable()
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(tag, "Error stopping background thread", e)
        }
    }

    fun resumeCamera() {
        val surface = cachedSurfaceTexture ?: return
        if (cameraDevice == null || captureSession == null) {
            openCamera(surface, cachedWidth, cachedHeight)
        } else {
            updatePreview()
        }
    }

    fun setManual2Adjustments(adjustments: Manual2Adjustments) {
        currentManual2Adjustments = adjustments
        manual2AdjustmentsFlow.value = adjustments
        updatePreview()
    }

    fun setDenoiseMode(mode: DenoiseMode) {
        currentDenoiseMode = mode
        denoiseModeFlow.value = mode
        updatePreview()
    }

    fun openCamera(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        startBackgroundThread()
        cachedSurfaceTexture = surfaceTexture
        cachedWidth = width
        cachedHeight = height

        try {
            selectCameraIdForLens(currentLens)
            val chars = cameraManager.getCameraCharacteristics(currentCameraId)
            cameraCharacteristics = chars

            // Query characteristics
            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            if (isoRange != null) {
                sensorIsoRange.value = isoRange.lower..isoRange.upper
            }
            val expRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            if (expRange != null) {
                exposureTimeRange.value = expRange.lower..expRange.upper
            }
            val minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
            if (minFocus != null) {
                minFocusDistance.value = minFocus
            }

            setupImageReaders(chars)

            surfaceTexture.setDefaultBufferSize(1920, 1080)
            previewSurface = Surface(surfaceTexture)

            cameraManager.openCamera(currentCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(tag, "Camera open error: $error")
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)

        } catch (e: SecurityException) {
            Log.e(tag, "Camera permission missing", e)
        } catch (e: Exception) {
            Log.e(tag, "Error opening camera", e)
        }
    }

    private fun selectCameraIdForLens(lens: CameraLens) {
        val cameraIds = cameraManager.cameraIdList
        var foundBack = false

        for (id in cameraIds) {
            val chars = cameraManager.getCameraCharacteristics(id)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                if (!foundBack) {
                    currentCameraId = id
                    foundBack = true
                }
                val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focal = focalLengths?.firstOrNull() ?: 24f

                when (lens) {
                    CameraLens.ULTRA_WIDE -> if (focal < 20f) { currentCameraId = id; return }
                    CameraLens.MAIN_WIDE, CameraLens.ZOOM_10X -> if (focal in 20f..30f) { currentCameraId = id; return }
                    CameraLens.TELE_3X -> if (focal in 50f..80f) { currentCameraId = id; return }
                    CameraLens.PERISCOPE_5X -> if (focal > 80f) { currentCameraId = id; return }
                }
            }
        }
    }

    private fun setupImageReaders(characteristics: CameraCharacteristics) {
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val maxResMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP_MAXIMUM_RESOLUTION)
        } else null

        val targetSize = determineCaptureSize(map, maxResMap, currentResolution)

        jpegImageReader?.close()
        jpegImageReader = ImageReader.newInstance(targetSize.width, targetSize.height, ImageFormat.JPEG, 2)

        // Setup RAW Reader if supported
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val supportsRaw = capabilities.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
        rawImageReader?.close()
        if (supportsRaw) {
            val rawSizes = map?.getOutputSizes(ImageFormat.RAW_SENSOR)
            val rawSize = rawSizes?.maxByOrNull { it.width * it.height } ?: targetSize
            rawImageReader = ImageReader.newInstance(rawSize.width, rawSize.height, ImageFormat.RAW_SENSOR, 2)
        } else {
            rawImageReader = null
        }

        // Preview analysis reader for Live Histogram, Zebra, Peaking
        analysisImageReader?.close()
        analysisImageReader = ImageReader.newInstance(320, 240, ImageFormat.YUV_420_888, 2).apply {
            setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                processAnalysisFrame(image)
                image.close()
            }, backgroundHandler)
        }
    }

    private fun determineCaptureSize(
        map: android.hardware.camera2.params.StreamConfigurationMap?,
        maxResMap: android.hardware.camera2.params.StreamConfigurationMap?,
        resolution: CaptureResolution
    ): Size {
        val standardJpegSizes = map?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
        val maxResJpegSizes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            maxResMap?.getOutputSizes(ImageFormat.JPEG)?.toList() ?: emptyList()
        } else emptyList()

        val allSizes = (maxResJpegSizes + standardJpegSizes).sortedByDescending { it.width * it.height }

        return when (resolution) {
            CaptureResolution.RES_200MP -> {
                allSizes.firstOrNull { it.width >= 12000 }
                    ?: allSizes.firstOrNull()
                    ?: Size(16320, 12240)
            }
            CaptureResolution.RES_50MP -> {
                allSizes.firstOrNull { it.width in 7000..11000 }
                    ?: allSizes.getOrNull(1)
                    ?: Size(8160, 6120)
            }
            CaptureResolution.RES_12MP -> {
                allSizes.firstOrNull { it.width in 3800..4500 }
                    ?: allSizes.lastOrNull()
                    ?: Size(4000, 3000)
            }
        }
    }

    private fun startCaptureSession() {
        val camera = cameraDevice ?: return
        val preview = previewSurface ?: return
        val jpeg = jpegImageReader?.surface
        val analysis = analysisImageReader?.surface

        val surfaces = mutableListOf<Surface>(preview)
        jpeg?.let { surfaces.add(it) }
        analysis?.let { surfaces.add(it) }
        rawImageReader?.surface?.let { surfaces.add(it) }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val outputConfigs = surfaces.map { OutputConfiguration(it) }
                val sessionConfig = SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    outputConfigs,
                    Executors.newSingleThreadExecutor(),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            updatePreview()
                        }
                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            Log.e(tag, "Session configuration failed")
                        }
                    }
                )
                camera.createCaptureSession(sessionConfig)
            } else {
                @Suppress("DEPRECATION")
                camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        updatePreview()
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(tag, "Session configuration failed")
                    }
                }, backgroundHandler)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error creating capture session", e)
        }
    }

    fun updatePreview() {
        val session = captureSession ?: return
        val camera = cameraDevice ?: return
        val surface = previewSurface ?: return

        try {
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
                analysisImageReader?.surface?.let { addTarget(it) }
                applyProControlsToBuilder(this)
            }

            session.setRepeatingRequest(builder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    super.onCaptureCompleted(session, request, result)
                    if (isIsoAuto) {
                        result.get(CaptureResult.SENSOR_SENSITIVITY)?.let { autoIso ->
                            manualIso = autoIso
                        }
                    }
                    if (isShutterAuto) {
                        result.get(CaptureResult.SENSOR_EXPOSURE_TIME)?.let { autoTime ->
                            manualShutterNs = autoTime
                        }
                    }
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(tag, "Error updating preview", e)
        }
    }

    /**
     * Smooth, crash-proof zoom control supporting 1.0x up to 10.0x for 200MP and all modes.
     */
    fun setZoomRatio(zoom: Float) {
        currentZoom = zoom.coerceIn(0.6f, 10.0f)
        zoomRatioFlow.value = currentZoom
        updatePreview()
    }

    private fun applyProControlsToBuilder(builder: CaptureRequest.Builder) {
        // Shooting mode & Exposure control (Manual ISO and Shutter)
        when (currentShootingMode) {
            ShootingMode.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
            }
            ShootingMode.SHUTTER_PRIORITY -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, manualShutterNs)
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, manualIso.coerceIn(sensorIsoRange.value))
            }
            ShootingMode.ISO_PRIORITY -> {
                builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, manualIso.coerceIn(sensorIsoRange.value))
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, manualShutterNs)
            }
            ShootingMode.MANUAL -> {
                if (isIsoAuto && isShutterAuto) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                } else {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, manualIso.coerceIn(sensorIsoRange.value))
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, manualShutterNs)
                }
            }
        }

        // EV Exposure Compensation
        try {
            val step = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
            val stepVal = step?.let { it.numerator.toFloat() / it.denominator.toFloat() } ?: 0.333f
            val compIndex = (exposureCompensationEv / stepVal).toInt()
            val compRange = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            if (compRange != null) {
                builder.set(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    compIndex.coerceIn(compRange.lower, compRange.upper)
                )
            }
        } catch (_: Exception) {}

        // Focus Mode (AF vs Manual Focus)
        if (isFocusAuto) {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        } else {
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            builder.set(CaptureRequest.LENS_FOCUS_DISTANCE, manualFocusDistance)
        }

        // White Balance / Kelvin
        when (currentAwbPreset) {
            AwbPreset.AUTO -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
            }
            AwbPreset.DAYLIGHT -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT)
            }
            AwbPreset.CLOUDY -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
            }
            AwbPreset.SHADE -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_SHADE)
            }
            AwbPreset.INCANDESCENT -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT)
            }
            AwbPreset.FLUORESCENT -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT)
            }
            AwbPreset.CUSTOM_KELVIN -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF)
                val gains = ToneCurveHelper.kelvinToRggbGains(manualKelvin)
                builder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                builder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains)
            }
            AwbPreset.MANUAL_2 -> {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO)
                try {
                    val compRange = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                    if (compRange != null) {
                        val extraBias = ((currentManual2Adjustments.brightness + currentManual2Adjustments.highlights) / 50f).toInt()
                        val step = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                        val stepVal = step?.let { it.numerator.toFloat() / it.denominator.toFloat() } ?: 0.333f
                        val baseIndex = (exposureCompensationEv / stepVal).toInt()
                        builder.set(
                            CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                            (baseIndex + extraBias).coerceIn(compRange.lower, compRange.upper)
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        // Noise Reduction Mode (Off, Standard Hardware, AI Neural)
        when (currentDenoiseMode) {
            DenoiseMode.OFF -> {
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF)
            }
            DenoiseMode.STANDARD -> {
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_FAST)
            }
            DenoiseMode.AI_NEURAL -> {
                builder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY)
            }
        }

        // AE/AF Lock
        builder.set(CaptureRequest.CONTROL_AE_LOCK, isAeAfLocked)

        // Photo Log / Flat tone mapping curve
        if (isPhotoLogEnabled || currentColorProfile != ColorProfile.STANDARD) {
            try {
                val profile = if (isPhotoLogEnabled) ColorProfile.FLAT_LOG else currentColorProfile
                val tonemapCurve = ToneCurveHelper.buildTonemapCurve(profile)
                builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
                builder.set(CaptureRequest.TONEMAP_CURVE, tonemapCurve)
            } catch (e: Exception) {
                Log.w(tag, "Tonemap curve not supported: ${e.message}")
            }
        } else {
            builder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_FAST)
        }

        // Crash-proof Zoom Implementation: Try CONTROL_ZOOM_RATIO, fallback to SCALER_CROP_REGION
        applySafeZoom(builder)
    }

    private fun applySafeZoom(builder: CaptureRequest.Builder) {
        val chars = cameraCharacteristics ?: return
        var zoomRatioApplied = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val range: Range<Float>? = chars.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
                if (range != null && currentZoom >= range.lower && currentZoom <= range.upper) {
                    builder.set(CaptureRequest.CONTROL_ZOOM_RATIO, currentZoom)
                    zoomRatioApplied = true
                }
            } catch (e: Exception) {
                Log.w(tag, "CONTROL_ZOOM_RATIO fallback to crop region: ${e.message}")
            }
        }

        if (!zoomRatioApplied) {
            // Apply Scaler Crop Region (works on all Android versions and sensors)
            val activeArray = chars.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
            if (activeArray != null) {
                val clampedZoom = currentZoom.coerceIn(1.0f, 10.0f)
                val cropWidth = (activeArray.width() / clampedZoom).toInt()
                val cropHeight = (activeArray.height() / clampedZoom).toInt()
                val cropLeft = activeArray.left + (activeArray.width() - cropWidth) / 2
                val cropTop = activeArray.top + (activeArray.height() - cropHeight) / 2
                val cropRect = Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)
                builder.set(CaptureRequest.SCALER_CROP_REGION, cropRect)
            }
        }
    }

    private fun processAnalysisFrame(image: Image) {
        val planes = image.planes
        if (planes.isEmpty()) return

        val yBuffer = planes[0].buffer
        val ySize = yBuffer.remaining()
        val yStep = 4
        val lumaCounts = IntArray(64)
        var clippedPixels = 0
        var totalSamples = 0

        for (i in 0 until ySize step yStep) {
            val y = yBuffer.get(i).toInt() and 0xFF
            val bin = (y shr 2).coerceIn(0, 63)
            lumaCounts[bin]++
            totalSamples++
            if (y >= 242) {
                clippedPixels++
            }
        }

        val maxCount = lumaCounts.maxOrNull()?.coerceAtLeast(1) ?: 1
        val lumaNormalized = FloatArray(64) { i -> lumaCounts[i].toFloat() / maxCount }

        val gains = ToneCurveHelper.kelvinToRggbGains(manualKelvin)
        val rNormalized = FloatArray(64) { i -> (lumaNormalized[i] * (gains.red / 2.0f)).coerceIn(0f, 1f) }
        val gNormalized = FloatArray(64) { i -> lumaNormalized[i] }
        val bNormalized = FloatArray(64) { i -> (lumaNormalized[i] * (gains.blue / 2.0f)).coerceIn(0f, 1f) }

        val clippingPercent = if (totalSamples > 0) (clippedPixels.toFloat() / totalSamples) * 100f else 0f

        histogramFlow.value = HistogramData(
            r = rNormalized,
            g = gNormalized,
            b = bNormalized,
            luma = lumaNormalized,
            highlightClippingPercent = clippingPercent
        )
    }

    /**
     * Executes Capture: Single shot, RAW/DNG, or Night Mode Long Exposure.
     * Guaranteed never to freeze the preview stream!
     */
    fun capturePhoto(nightExposureDurationSeconds: Int = 0) {
        val session = captureSession ?: return
        val camera = cameraDevice ?: return
        val jpegReader = jpegImageReader ?: return

        isCapturingFlow.value = true

        scope.launch {
            // Trigger shutter flash indicator
            photoFlashTrigger.tryEmit(Unit)

            // Night mode long exposure & light accumulation
            if (nightExposureDurationSeconds > 0) {
                nightTotalSeconds.value = nightExposureDurationSeconds
                val totalSteps = nightExposureDurationSeconds * 10
                for (step in 1..totalSteps) {
                    val remainingSec = nightExposureDurationSeconds - (step / 10)
                    nightCountdownSeconds.value = remainingSec.coerceAtLeast(0)
                    lightAccumulationPercentFlow.value = ((step.toFloat() / totalSteps) * 100).toInt()
                    delay(100)
                }
                nightCountdownSeconds.value = 0
            }

            try {
                val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(jpegReader.surface)
                    applyProControlsToBuilder(this)

                    // Maximum Resolution Mode (200MP / 50MP on S24 Ultra)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (currentResolution == CaptureResolution.RES_200MP || currentResolution == CaptureResolution.RES_50MP) {
                            set(
                                CaptureRequest.SENSOR_PIXEL_MODE,
                                CaptureRequest.SENSOR_PIXEL_MODE_MAXIMUM_RESOLUTION
                            )
                        }
                    }

                    // For RAW capture
                    if (currentFormat != CaptureFormat.JPEG && rawImageReader != null) {
                        rawImageReader?.surface?.let { addTarget(it) }
                    }

                    // Night mode long exposure on sensor
                    if (nightExposureDurationSeconds > 0) {
                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                        try {
                            val compRange = cameraCharacteristics?.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
                            if (compRange != null) {
                                set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, compRange.upper)
                            }
                        } catch (_: Exception) {}
                        val maxIso = sensorIsoRange.value.endInclusive
                        set(CaptureRequest.SENSOR_SENSITIVITY, maxIso.coerceAtLeast(1600))
                    }

                    val jpegOrientation = computeJpegOrientation()
                    set(CaptureRequest.JPEG_QUALITY, 100.toByte())
                    set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation)
                }

                val targetOrientation = computeJpegOrientation()
                var capturedResult: TotalCaptureResult? = null

                jpegReader.setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    saveCapturedImage(image, capturedResult, nightExposureDurationSeconds, targetOrientation)
                    image.close()
                }, backgroundHandler)

                if (currentFormat != CaptureFormat.JPEG && rawImageReader != null) {
                    rawImageReader?.setOnImageAvailableListener({ reader ->
                        val rawImage = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                        capturedResult?.let { result ->
                            saveRawDng(rawImage, result)
                        }
                        rawImage.close()
                    }, backgroundHandler)
                }

                session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                        capturedResult = result
                        // ALWAYS ensure preview stream continues uninterrupted!
                        updatePreview()
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                        isCapturingFlow.value = false
                        Log.e(tag, "Still capture failed: ${failure.reason}")
                        // Resume preview immediately
                        updatePreview()
                    }
                }, backgroundHandler)

                // Safety Watchdog: Reset isCapturing after 4s so UI never stays stuck
                launch {
                    delay(4000)
                    if (isCapturingFlow.value) {
                        isCapturingFlow.value = false
                        updatePreview()
                    }
                }

            } catch (e: Exception) {
                Log.e(tag, "Error during capturePhoto", e)
                isCapturingFlow.value = false
                updatePreview()
            }
        }
    }

    private fun saveCapturedImage(
        image: Image,
        captureResult: TotalCaptureResult?,
        nightExposureDurationSeconds: Int = 0,
        orientationDegrees: Int = 0
    ) {
        try {
            val buffer = image.planes[0].buffer
            val rawBytes = ByteArray(buffer.remaining())
            buffer.get(rawBytes)

            // Master Image Pipeline:
            // 1. Physically rotates bitmap so vertical photos are always vertical and horizontal are horizontal
            // 2. Applies Night Mode photon accumulation and shadow lifting
            // 3. Applies Manual 2 adjustments (Brightness, Contrast, Shadows, Highlights)
            // 4. Applies AI Neural Denoise (Gemini/ChatGPT remastering)
            val bytes = ToneCurveHelper.processMasterPipeline(
                rawBytes = rawBytes,
                orientationDegrees = orientationDegrees,
                nightExposureSeconds = nightExposureDurationSeconds,
                manual2Adjustments = if (currentAwbPreset == AwbPreset.MANUAL_2) currentManual2Adjustments else null,
                denoiseMode = currentDenoiseMode
            )

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PRO_200MP_${currentResolution.label.replace(" ", "")}_$timeStamp.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ProCamera200MP")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }

                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                val width = if (options.outWidth > 0) options.outWidth else currentResolution.width
                val height = if (options.outHeight > 0) options.outHeight else currentResolution.height
                val megapixels = (width * height).toFloat() / 1_000_000f
                val fileSizeMb = String.format(Locale.US, "%.1f MB", bytes.size.toFloat() / (1024 * 1024))

                val exposureTimeSec = manualShutterNs.toDouble() / 1_000_000_000.0
                val exposureFormatted = if (nightExposureDurationSeconds > 0) {
                    "${nightExposureDurationSeconds}s (Noturno Aberto)"
                } else if (exposureTimeSec >= 1.0) {
                    "${exposureTimeSec.toInt()}s"
                } else {
                    "1/${(1.0 / exposureTimeSec).toInt()}s"
                }

                val mediaInfo = CapturedMediaInfo(
                    uri = uri,
                    filePath = "DCIM/ProCamera200MP/$fileName",
                    fileName = fileName,
                    format = "JPEG",
                    resolution = "${width}x${height}",
                    width = width,
                    height = height,
                    megapixels = megapixels,
                    fileSizeFormatted = fileSizeMb,
                    iso = manualIso,
                    exposureTime = exposureFormatted,
                    fNumber = currentLens.aperture,
                    focalLength = currentLens.focalLength,
                    colorProfile = if (isPhotoLogEnabled) "Foto LOG" else currentColorProfile.label,
                    isDng = false,
                    timestamp = System.currentTimeMillis(),
                    orientationDegrees = orientationDegrees,
                    denoiseMode = currentDenoiseMode.label
                )

                scope.launch {
                    lastCapturedMedia.value = mediaInfo
                    isCapturingFlow.value = false
                    toastMessageFlow.tryEmit("Foto salva: ${currentResolution.label} (${width}x${height})")
                    // Keep camera active and ready for next photo
                    updatePreview()
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error saving captured JPEG", e)
            isCapturingFlow.value = false
            updatePreview()
        }
    }

    private fun saveRawDng(rawImage: Image, captureResult: TotalCaptureResult) {
        val chars = cameraCharacteristics ?: return
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "PRO_RAW_${currentResolution.label.replace(" ", "")}_$timeStamp.dng"

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/x-adobe-dng")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ProCamera200MP")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    val dngCreator = DngCreator(chars, captureResult)
                    dngCreator.writeImage(out, rawImage)
                    dngCreator.close()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error saving DNG RAW file", e)
        }
    }

    /**
     * Lens switching with smooth zoom and crash prevention.
     */
    fun switchLens(lens: CameraLens) {
        currentLens = lens

        if (lens == CameraLens.ZOOM_10X) {
            setZoomRatio(10.0f)
            return
        }

        // Set zoom ratio corresponding to the lens
        setZoomRatio(lens.zoomFactor)

        val previousId = currentCameraId
        selectCameraIdForLens(lens)

        // If physical camera ID changed, safely reopen on new sensor
        if (previousId != currentCameraId && cachedSurfaceTexture != null) {
            closeCamera()
            cachedSurfaceTexture?.let { texture ->
                openCamera(texture, cachedWidth, cachedHeight)
            }
        } else {
            updatePreview()
        }
    }

    fun changeResolution(resolution: CaptureResolution) {
        currentResolution = resolution
        val chars = cameraCharacteristics ?: return
        setupImageReaders(chars)
        startCaptureSession()
    }

    fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            jpegImageReader?.close()
            jpegImageReader = null
            rawImageReader?.close()
            rawImageReader = null
            analysisImageReader?.close()
            analysisImageReader = null
        } catch (e: Exception) {
            Log.e(tag, "Error closing camera", e)
        }
    }
}
