package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeviceInfoDialog
import com.example.ui.components.HistogramView
import com.example.ui.components.NightCaptureOverlay
import com.example.ui.components.PhotoPreviewDialog
import com.example.ui.components.ProDialControls
import com.example.ui.components.TopStatusBar
import com.example.ui.components.ViewfinderLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

@Composable
fun ProCameraScreen(
    viewModel: ProCameraViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val selectedLens by viewModel.selectedLens.collectAsState()
    val selectedResolution by viewModel.selectedResolution.collectAsState()
    val selectedFormat by viewModel.selectedFormat.collectAsState()
    val shootingMode by viewModel.shootingMode.collectAsState()
    val activeDial by viewModel.activeDial.collectAsState()

    val isIsoAuto by viewModel.isIsoAuto.collectAsState()
    val manualIso by viewModel.manualIso.collectAsState()
    val isShutterAuto by viewModel.isShutterAuto.collectAsState()
    val manualShutterNs by viewModel.manualShutterNs.collectAsState()
    val exposureCompensationEv by viewModel.exposureCompensationEv.collectAsState()
    val isFocusAuto by viewModel.isFocusAuto.collectAsState()
    val manualFocusDistance by viewModel.manualFocusDistance.collectAsState()
    val awbPreset by viewModel.awbPreset.collectAsState()
    val manualKelvin by viewModel.manualKelvin.collectAsState()

    val isAeAfLocked by viewModel.isAeAfLocked.collectAsState()
    val isPhotoLogEnabled by viewModel.isPhotoLogEnabled.collectAsState()
    val isHistogramEnabled by viewModel.isHistogramEnabled.collectAsState()
    val isZebraEnabled by viewModel.isZebraEnabled.collectAsState()
    val isFocusPeakingEnabled by viewModel.isFocusPeakingEnabled.collectAsState()
    val isGridEnabled by viewModel.isGridEnabled.collectAsState()
    val isNightModeActive by viewModel.isNightModeActive.collectAsState()
    val nightDurationSeconds by viewModel.nightDurationSeconds.collectAsState()

    val showDeviceInfoDialog by viewModel.showDeviceInfoDialog.collectAsState()
    val showPreviewDialog by viewModel.showPreviewDialog.collectAsState()

    val lastCapturedMedia by viewModel.lastCapturedMedia.collectAsState()
    val isCapturing by viewModel.isCapturingFlow.collectAsState()
    val nightCountdown by viewModel.nightCountdownSeconds.collectAsState()
    val nightTotal by viewModel.nightTotalSeconds.collectAsState()
    val lightAccumulationPercent by viewModel.lightAccumulationPercent.collectAsState()
    val histogramData by viewModel.histogramFlow.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()

    // Tactile shutter flash
    var isShutterFlashing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.photoFlashTrigger.collect {
            isShutterFlashing = true
            delay(120)
            isShutterFlashing = false
        }
    }

    // Floating saved photo banner
    var toastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        viewModel.toastMessageFlow.collect { msg ->
            toastMessage = msg
            delay(2800)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("pro_camera_root")
    ) {
        // 1. Live Camera Viewfinder Layer
        ViewfinderLayout(
            isGridEnabled = isGridEnabled,
            isZebraEnabled = isZebraEnabled,
            isFocusPeakingEnabled = isFocusPeakingEnabled,
            highlightClippingPercent = histogramData.highlightClippingPercent,
            isAeAfLocked = isAeAfLocked,
            currentZoom = zoomRatio,
            isShutterFlashing = isShutterFlashing,
            onSurfaceTextureAvailable = { surface, w, h ->
                viewModel.onSurfaceTextureAvailable(surface, w, h)
            },
            onTapToFocus = { _, _ ->
                viewModel.cameraController.updatePreview()
            },
            onPinchZoom = { newZoom ->
                viewModel.setZoomRatio(newZoom)
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top Status & Quick Control HUD
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            TopStatusBar(
                deviceInfo = deviceInfo,
                currentResolution = selectedResolution,
                onResolutionSelected = { viewModel.selectResolution(it) },
                currentFormat = selectedFormat,
                onFormatSelected = { viewModel.selectFormat(it) },
                shootingMode = shootingMode,
                onShootingModeSelected = { viewModel.selectShootingMode(it) },
                isPhotoLogEnabled = isPhotoLogEnabled,
                onTogglePhotoLog = { viewModel.togglePhotoLog() },
                isHistogramEnabled = isHistogramEnabled,
                onToggleHistogram = { viewModel.toggleHistogram() },
                isZebraEnabled = isZebraEnabled,
                onToggleZebra = { viewModel.toggleZebra() },
                isFocusPeakingEnabled = isFocusPeakingEnabled,
                onToggleFocusPeaking = { viewModel.toggleFocusPeaking() },
                isGridEnabled = isGridEnabled,
                onToggleGrid = { viewModel.toggleGrid() },
                isAeAfLocked = isAeAfLocked,
                onToggleAeAfLock = { viewModel.toggleAeAfLock() },
                onOpenDeviceInfo = { viewModel.setShowDeviceInfoDialog(true) }
            )

            // Floating Real-time Histogram Overlay (Top-Right)
            if (isHistogramEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp, end = 12.dp)
                ) {
                    HistogramView(data = histogramData)
                }
            }
        }

        // 3. Floating Saved Photo Confirmation Toast
        AnimatedVisibility(
            visible = toastMessage != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -40 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -40 }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(Color(0xEE141820), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("photo_saved_toast")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = toastMessage ?: "",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 4. Bottom Professional Dial & Controls
        ProDialControls(
            selectedLens = selectedLens,
            onSelectLens = { viewModel.selectLens(it) },
            currentZoom = zoomRatio,
            onZoomChanged = { viewModel.setZoomRatio(it) },
            activeDial = activeDial,
            onSelectDial = { viewModel.selectDial(it) },
            isIsoAuto = isIsoAuto,
            manualIso = manualIso,
            onIsoChanged = { iso, auto -> viewModel.setIso(iso, auto) },
            isShutterAuto = isShutterAuto,
            manualShutterNs = manualShutterNs,
            onShutterChanged = { ns, auto -> viewModel.setShutter(ns, auto) },
            exposureCompensationEv = exposureCompensationEv,
            onEvChanged = { viewModel.setEv(it) },
            isFocusAuto = isFocusAuto,
            manualFocusDistance = manualFocusDistance,
            onFocusChanged = { dist, auto -> viewModel.setFocus(dist, auto) },
            awbPreset = awbPreset,
            manualKelvin = manualKelvin,
            onWbPresetChanged = { viewModel.setWbPreset(it) },
            onKelvinChanged = { viewModel.setKelvin(it) },
            isNightModeActive = isNightModeActive,
            nightDurationSeconds = nightDurationSeconds,
            onNightDurationChanged = { viewModel.setNightDuration(it) },
            onToggleNightMode = { viewModel.toggleNightMode() },
            lastMedia = lastCapturedMedia,
            isCapturing = isCapturing,
            onShutterClick = { viewModel.capturePhoto() },
            onOpenGalleryPreview = {
                if (lastCapturedMedia != null) {
                    viewModel.setShowPreviewDialog(true)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // 5. Fullscreen Night Mode / Long Exposure Overlay
        NightCaptureOverlay(
            isCapturing = isCapturing,
            remainingSeconds = nightCountdown,
            totalSeconds = nightTotal,
            lightAccumulationPercent = lightAccumulationPercent
        )

        // 6. Photo Details Dialog (EXIF & Metadata)
        if (showPreviewDialog && lastCapturedMedia != null) {
            PhotoPreviewDialog(
                media = lastCapturedMedia!!,
                onDismiss = { viewModel.setShowPreviewDialog(false) }
            )
        }

        // 7. Device Hardware Verification Dialog
        if (showDeviceInfoDialog) {
            DeviceInfoDialog(
                deviceInfo = deviceInfo,
                onDismiss = { viewModel.setShowDeviceInfoDialog(false) }
            )
        }
    }
}
