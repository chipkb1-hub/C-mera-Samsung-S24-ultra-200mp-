package com.example.ui

import android.app.Application
import android.graphics.SurfaceTexture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.Camera2Controller
import com.example.camera.DeviceDetector
import com.example.model.ActiveDial
import com.example.model.AwbPreset
import com.example.model.CameraLens
import com.example.model.CaptureFormat
import com.example.model.CaptureResolution
import com.example.model.CapturedMediaInfo
import com.example.model.ColorProfile
import com.example.model.DeviceDetectionInfo
import com.example.model.HistogramData
import com.example.model.ShootingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProCameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraController = Camera2Controller(application)

    private val _deviceInfo = MutableStateFlow(DeviceDetector.detectDevice(application))
    val deviceInfo = _deviceInfo.asStateFlow()

    private val _selectedLens = MutableStateFlow(CameraLens.MAIN_WIDE)
    val selectedLens = _selectedLens.asStateFlow()

    private val _selectedResolution = MutableStateFlow(CaptureResolution.RES_200MP)
    val selectedResolution = _selectedResolution.asStateFlow()

    private val _selectedFormat = MutableStateFlow(CaptureFormat.JPEG)
    val selectedFormat = _selectedFormat.asStateFlow()

    // ALWAYS DEFAULT TO AUTO MODE ON STARTUP AS REQUESTED BY USER!
    private val _shootingMode = MutableStateFlow(ShootingMode.AUTO)
    val shootingMode = _shootingMode.asStateFlow()

    private val _colorProfile = MutableStateFlow(ColorProfile.STANDARD)
    val colorProfile = _colorProfile.asStateFlow()

    private val _activeDial = MutableStateFlow(ActiveDial.NONE)
    val activeDial = _activeDial.asStateFlow()

    private val _isIsoAuto = MutableStateFlow(true)
    val isIsoAuto = _isIsoAuto.asStateFlow()

    private val _manualIso = MutableStateFlow(100)
    val manualIso = _manualIso.asStateFlow()

    private val _isShutterAuto = MutableStateFlow(true)
    val isShutterAuto = _isShutterAuto.asStateFlow()

    private val _manualShutterNs = MutableStateFlow(1_000_000_000L / 125) // 1/125s
    val manualShutterNs = _manualShutterNs.asStateFlow()

    private val _exposureCompensationEv = MutableStateFlow(0.0f)
    val exposureCompensationEv = _exposureCompensationEv.asStateFlow()

    private val _isFocusAuto = MutableStateFlow(true)
    val isFocusAuto = _isFocusAuto.asStateFlow()

    private val _manualFocusDistance = MutableStateFlow(0.0f)
    val manualFocusDistance = _manualFocusDistance.asStateFlow()

    private val _awbPreset = MutableStateFlow(AwbPreset.AUTO)
    val awbPreset = _awbPreset.asStateFlow()

    private val _manualKelvin = MutableStateFlow(5500)
    val manualKelvin = _manualKelvin.asStateFlow()

    private val _isAeAfLocked = MutableStateFlow(false)
    val isAeAfLocked = _isAeAfLocked.asStateFlow()

    private val _isPhotoLogEnabled = MutableStateFlow(false)
    val isPhotoLogEnabled = _isPhotoLogEnabled.asStateFlow()

    private val _isHistogramEnabled = MutableStateFlow(true)
    val isHistogramEnabled = _isHistogramEnabled.asStateFlow()

    private val _isZebraEnabled = MutableStateFlow(false)
    val isZebraEnabled = _isZebraEnabled.asStateFlow()

    private val _isFocusPeakingEnabled = MutableStateFlow(false)
    val isFocusPeakingEnabled = _isFocusPeakingEnabled.asStateFlow()

    private val _isGridEnabled = MutableStateFlow(true)
    val isGridEnabled = _isGridEnabled.asStateFlow()

    private val _isNightModeActive = MutableStateFlow(false)
    val isNightModeActive = _isNightModeActive.asStateFlow()

    private val _nightDurationSeconds = MutableStateFlow(5)
    val nightDurationSeconds = _nightDurationSeconds.asStateFlow()

    private val _showDeviceInfoDialog = MutableStateFlow(false)
    val showDeviceInfoDialog = _showDeviceInfoDialog.asStateFlow()

    private val _showPreviewDialog = MutableStateFlow(false)
    val showPreviewDialog = _showPreviewDialog.asStateFlow()

    // Pass-through flows from controller
    val histogramFlow = cameraController.histogramFlow
    val isCapturingFlow = cameraController.isCapturingFlow
    val nightCountdownSeconds = cameraController.nightCountdownSeconds
    val nightTotalSeconds = cameraController.nightTotalSeconds
    val lightAccumulationPercent = cameraController.lightAccumulationPercentFlow
    val lastCapturedMedia = cameraController.lastCapturedMedia
    val zoomRatio = cameraController.zoomRatioFlow
    val photoFlashTrigger = cameraController.photoFlashTrigger
    val toastMessageFlow = cameraController.toastMessageFlow

    init {
        syncSettingsToController()
    }

    private fun syncSettingsToController() {
        cameraController.currentLens = _selectedLens.value
        cameraController.currentResolution = _selectedResolution.value
        cameraController.currentFormat = _selectedFormat.value
        cameraController.currentShootingMode = _shootingMode.value
        cameraController.currentColorProfile = _colorProfile.value
        cameraController.isIsoAuto = _isIsoAuto.value
        cameraController.manualIso = _manualIso.value
        cameraController.isShutterAuto = _isShutterAuto.value
        cameraController.manualShutterNs = _manualShutterNs.value
        cameraController.exposureCompensationEv = _exposureCompensationEv.value
        cameraController.isFocusAuto = _isFocusAuto.value
        cameraController.manualFocusDistance = _manualFocusDistance.value
        cameraController.currentAwbPreset = _awbPreset.value
        cameraController.manualKelvin = _manualKelvin.value
        cameraController.isAeAfLocked = _isAeAfLocked.value
        cameraController.isPhotoLogEnabled = _isPhotoLogEnabled.value
    }

    fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        syncSettingsToController()
        cameraController.openCamera(surfaceTexture, width, height)
    }

    fun selectLens(lens: CameraLens) {
        _selectedLens.value = lens
        cameraController.currentLens = lens
        cameraController.switchLens(lens)
    }

    fun setZoomRatio(zoom: Float) {
        cameraController.setZoomRatio(zoom)
        // Update lens highlight if matching
        when {
            zoom < 0.9f -> _selectedLens.value = CameraLens.ULTRA_WIDE
            zoom in 0.9f..2.5f -> _selectedLens.value = CameraLens.MAIN_WIDE
            zoom in 2.6f..4.5f -> _selectedLens.value = CameraLens.TELE_3X
            zoom in 4.6f..8.0f -> _selectedLens.value = CameraLens.PERISCOPE_5X
            zoom > 8.0f -> _selectedLens.value = CameraLens.ZOOM_10X
        }
    }

    fun selectResolution(resolution: CaptureResolution) {
        _selectedResolution.value = resolution
        cameraController.changeResolution(resolution)
    }

    fun selectFormat(format: CaptureFormat) {
        _selectedFormat.value = format
        cameraController.currentFormat = format
    }

    fun selectShootingMode(mode: ShootingMode) {
        _shootingMode.value = mode
        cameraController.currentShootingMode = mode

        when (mode) {
            ShootingMode.AUTO -> {
                _isIsoAuto.value = true
                _isShutterAuto.value = true
                cameraController.isIsoAuto = true
                cameraController.isShutterAuto = true
            }
            ShootingMode.SHUTTER_PRIORITY -> {
                _isIsoAuto.value = true
                _isShutterAuto.value = false
                cameraController.isIsoAuto = true
                cameraController.isShutterAuto = false
            }
            ShootingMode.ISO_PRIORITY -> {
                _isIsoAuto.value = false
                _isShutterAuto.value = true
                cameraController.isIsoAuto = false
                cameraController.isShutterAuto = true
            }
            ShootingMode.MANUAL -> {
                _isIsoAuto.value = false
                _isShutterAuto.value = false
                cameraController.isIsoAuto = false
                cameraController.isShutterAuto = false
            }
        }
        cameraController.updatePreview()
    }

    fun selectDial(dial: ActiveDial) {
        _activeDial.value = dial
    }

    fun setIso(iso: Int, auto: Boolean) {
        _manualIso.value = iso
        _isIsoAuto.value = auto
        cameraController.manualIso = iso
        cameraController.isIsoAuto = auto
        cameraController.updatePreview()
    }

    fun setShutter(nanoseconds: Long, auto: Boolean) {
        _manualShutterNs.value = nanoseconds
        _isShutterAuto.value = auto
        cameraController.manualShutterNs = nanoseconds
        cameraController.isShutterAuto = auto
        cameraController.updatePreview()
    }

    fun setEv(ev: Float) {
        _exposureCompensationEv.value = ev
        cameraController.exposureCompensationEv = ev
        cameraController.updatePreview()
    }

    fun setFocus(distance: Float, auto: Boolean) {
        _manualFocusDistance.value = distance
        _isFocusAuto.value = auto
        cameraController.manualFocusDistance = distance
        cameraController.isFocusAuto = auto
        cameraController.updatePreview()
    }

    fun setWbPreset(preset: AwbPreset) {
        _awbPreset.value = preset
        cameraController.currentAwbPreset = preset
        if (preset != AwbPreset.CUSTOM_KELVIN && preset.kelvin > 0) {
            _manualKelvin.value = preset.kelvin
            cameraController.manualKelvin = preset.kelvin
        }
        cameraController.updatePreview()
    }

    fun setKelvin(kelvin: Int) {
        _manualKelvin.value = kelvin
        _awbPreset.value = AwbPreset.CUSTOM_KELVIN
        cameraController.manualKelvin = kelvin
        cameraController.currentAwbPreset = AwbPreset.CUSTOM_KELVIN
        cameraController.updatePreview()
    }

    fun toggleAeAfLock() {
        _isAeAfLocked.value = !_isAeAfLocked.value
        cameraController.isAeAfLocked = _isAeAfLocked.value
        cameraController.updatePreview()
    }

    fun togglePhotoLog() {
        _isPhotoLogEnabled.value = !_isPhotoLogEnabled.value
        cameraController.isPhotoLogEnabled = _isPhotoLogEnabled.value
        cameraController.updatePreview()
    }

    fun toggleHistogram() {
        _isHistogramEnabled.value = !_isHistogramEnabled.value
    }

    fun toggleZebra() {
        _isZebraEnabled.value = !_isZebraEnabled.value
    }

    fun toggleFocusPeaking() {
        _isFocusPeakingEnabled.value = !_isFocusPeakingEnabled.value
    }

    fun toggleGrid() {
        _isGridEnabled.value = !_isGridEnabled.value
    }

    fun toggleNightMode() {
        _isNightModeActive.value = !_isNightModeActive.value
    }

    fun setNightDuration(seconds: Int) {
        _nightDurationSeconds.value = seconds
    }

    fun setShowDeviceInfoDialog(show: Boolean) {
        _showDeviceInfoDialog.value = show
    }

    fun setShowPreviewDialog(show: Boolean) {
        _showPreviewDialog.value = show
    }

    fun capturePhoto() {
        val nightDuration = if (_isNightModeActive.value) _nightDurationSeconds.value else 0
        cameraController.capturePhoto(nightDuration)
    }

    override fun onCleared() {
        super.onCleared()
        cameraController.closeCamera()
        cameraController.stopBackgroundThread()
    }
}
