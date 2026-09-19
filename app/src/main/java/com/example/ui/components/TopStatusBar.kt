package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CaptureFormat
import com.example.model.CaptureResolution
import com.example.model.DeviceDetectionInfo
import com.example.model.ShootingMode

@Composable
fun TopStatusBar(
    deviceInfo: DeviceDetectionInfo,
    currentResolution: CaptureResolution,
    onResolutionSelected: (CaptureResolution) -> Unit,
    currentFormat: CaptureFormat,
    onFormatSelected: (CaptureFormat) -> Unit,
    shootingMode: ShootingMode,
    onShootingModeSelected: (ShootingMode) -> Unit,
    isPhotoLogEnabled: Boolean,
    onTogglePhotoLog: () -> Unit,
    isHistogramEnabled: Boolean,
    onToggleHistogram: () -> Unit,
    isZebraEnabled: Boolean,
    onToggleZebra: () -> Unit,
    isFocusPeakingEnabled: Boolean,
    onToggleFocusPeaking: () -> Unit,
    isGridEnabled: Boolean,
    onToggleGrid: () -> Unit,
    isAeAfLocked: Boolean,
    onToggleAeAfLock: () -> Unit,
    onOpenDeviceInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xEE0B0C0E), Color(0x990B0C0E), Color.Transparent)
                )
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("top_status_bar")
    ) {
        // Row 1: Device Badge & Primary Toggles (Resolution, Format, Shooting Mode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // S24 Ultra Detection Badge
            Row(
                modifier = Modifier
                    .testTag("device_badge_button")
                    .background(Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33F5A623), RoundedCornerShape(16.dp))
                    .clickable { onOpenDeviceInfo() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF39FF14), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (deviceInfo.isS24Ultra) "SM-S928B • 200MP" else "S24U PRO 200MP",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Mode Selector: AUTO, S, A, M
            Row(
                modifier = Modifier
                    .background(Color(0x441E2228), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                ShootingMode.values().forEach { mode ->
                    val isSelected = shootingMode == mode
                    Box(
                        modifier = Modifier
                            .testTag("mode_${mode.code}")
                            .background(
                                if (isSelected) Color(0xFFFFB300) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onShootingModeSelected(mode) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = mode.code,
                            color = if (isSelected) Color.Black else Color(0xBBFFFFFF),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Lock Indicator button
            IconButton(
                onClick = onToggleAeAfLock,
                modifier = Modifier
                    .testTag("lock_ae_af_button")
                    .size(32.dp)
                    .background(
                        if (isAeAfLocked) Color(0xCCFFB300) else Color(0x33FFFFFF),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isAeAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Bloqueio AE/AF",
                    tint = if (isAeAfLocked) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Row 2: Resolution (12MP / 50MP / 200MP) & Format (JPG / RAW / RAW+JPG) & Overlays
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Resolution Selector
            Row(
                modifier = Modifier
                    .background(Color(0x441E2228), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                CaptureResolution.values().forEach { res ->
                    val isSelected = currentResolution == res
                    Box(
                        modifier = Modifier
                            .testTag("res_${res.label}")
                            .background(
                                if (isSelected) Color(0xFF00E5FF) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onResolutionSelected(res) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = res.label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Format Selector: JPEG, RAW, RAW+JPEG
            Row(
                modifier = Modifier
                    .background(Color(0x441E2228), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(2.dp)
            ) {
                CaptureFormat.values().forEach { format ->
                    val isSelected = currentFormat == format
                    Box(
                        modifier = Modifier
                            .testTag("format_${format.name}")
                            .background(
                                if (isSelected) Color(0xFFFF4081) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onFormatSelected(format) }
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = when (format) {
                                CaptureFormat.JPEG -> "JPG"
                                CaptureFormat.RAW_DNG -> "RAW"
                                CaptureFormat.RAW_PLUS_JPEG -> "RAW+JPG"
                            },
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Toggle Button: FOTO LOG
            PillToggleButton(
                label = "LOG",
                isActive = isPhotoLogEnabled,
                activeColor = Color(0xFF76FF03),
                onClick = onTogglePhotoLog,
                testTag = "toggle_log"
            )

            // Toggle Button: HIST
            PillToggleButton(
                label = "HIST",
                isActive = isHistogramEnabled,
                activeColor = Color(0xFF00E5FF),
                onClick = onToggleHistogram,
                testTag = "toggle_hist"
            )

            // Toggle Button: ZEBRA
            PillToggleButton(
                label = "ZEBRA",
                isActive = isZebraEnabled,
                activeColor = Color(0xFFFFD600),
                onClick = onToggleZebra,
                testTag = "toggle_zebra"
            )

            // Toggle Button: FOCUS PEAKING
            PillToggleButton(
                label = "PEAK",
                isActive = isFocusPeakingEnabled,
                activeColor = Color(0xFF00FF88),
                onClick = onToggleFocusPeaking,
                testTag = "toggle_peak"
            )

            // Toggle Button: GRID
            PillToggleButton(
                label = "GRID",
                isActive = isGridEnabled,
                activeColor = Color.White,
                onClick = onToggleGrid,
                testTag = "toggle_grid"
            )
        }
    }
}

@Composable
private fun PillToggleButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .background(
                if (isActive) activeColor else Color(0x33252A32),
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isActive) activeColor else Color(0x22FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (isActive) Color.Black else Color(0xDDFFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
