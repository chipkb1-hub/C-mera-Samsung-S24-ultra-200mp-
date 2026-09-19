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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveDial
import com.example.model.AwbPreset
import com.example.model.CameraLens
import com.example.model.CapturedMediaInfo
import com.example.model.Manual2Adjustments
import java.util.Locale

@Composable
fun ProDialControls(
    selectedLens: CameraLens,
    onSelectLens: (CameraLens) -> Unit,
    currentZoom: Float,
    onZoomChanged: (Float) -> Unit,
    activeDial: ActiveDial,
    onSelectDial: (ActiveDial) -> Unit,
    isIsoAuto: Boolean,
    manualIso: Int,
    onIsoChanged: (Int, Boolean) -> Unit,
    isShutterAuto: Boolean,
    manualShutterNs: Long,
    onShutterChanged: (Long, Boolean) -> Unit,
    exposureCompensationEv: Float,
    onEvChanged: (Float) -> Unit,
    isFocusAuto: Boolean,
    manualFocusDistance: Float,
    onFocusChanged: (Float, Boolean) -> Unit,
    awbPreset: AwbPreset,
    manualKelvin: Int,
    onWbPresetChanged: (AwbPreset) -> Unit,
    onKelvinChanged: (Int) -> Unit,
    manual2Adjustments: Manual2Adjustments = Manual2Adjustments(),
    onManual2AdjustmentsChanged: (Manual2Adjustments) -> Unit = {},
    isNightModeActive: Boolean,
    nightDurationSeconds: Int,
    onNightDurationChanged: (Int) -> Unit,
    onToggleNightMode: () -> Unit,
    lastMedia: CapturedMediaInfo?,
    isCapturing: Boolean,
    onShutterClick: () -> Unit,
    onOpenGalleryPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xCC08090C), Color(0xF508090C))
                )
            )
            .padding(bottom = 16.dp)
            .testTag("pro_dial_controls")
    ) {
        // 1. Lens Switcher Row: 0.6x, 1x (200MP), 3x, 5x
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CameraLens.values().forEach { lens ->
                val isSelected = selectedLens == lens
                val is200MpLens = lens == CameraLens.MAIN_WIDE || lens == CameraLens.ZOOM_10X

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("lens_${lens.id}")
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFFFFB300) else Color(0x331E2228)
                        )
                        .border(
                            1.dp,
                            if (is200MpLens) Color(0xFFFFD700) else Color(0x22FFFFFF),
                            CircleShape
                        )
                        .clickable {
                            onSelectLens(lens)
                            onZoomChanged(lens.zoomFactor)
                        }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = lens.label,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (is200MpLens) {
                            Text(
                                text = "200M",
                                color = if (isSelected) Color.Black else Color(0xFFFFD700),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Active Dial Slider / Options Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 16.dp)
        ) {
            when (activeDial) {
                ActiveDial.ISO -> {
                    IsoSliderControl(
                        isAuto = isIsoAuto,
                        currentIso = manualIso,
                        onIsoChanged = onIsoChanged
                    )
                }
                ActiveDial.SHUTTER -> {
                    ShutterSpeedControl(
                        isAuto = isShutterAuto,
                        currentShutterNs = manualShutterNs,
                        onShutterChanged = onShutterChanged
                    )
                }
                ActiveDial.EV -> {
                    EvSliderControl(
                        currentEv = exposureCompensationEv,
                        onEvChanged = onEvChanged
                    )
                }
                ActiveDial.FOCUS -> {
                    FocusSliderControl(
                        isAuto = isFocusAuto,
                        focusDistance = manualFocusDistance,
                        onFocusChanged = onFocusChanged
                    )
                }
                ActiveDial.WB -> {
                    WbKelvinControl(
                        preset = awbPreset,
                        kelvin = manualKelvin,
                        onPresetChanged = onWbPresetChanged,
                        onKelvinChanged = onKelvinChanged,
                        manual2Adjustments = manual2Adjustments,
                        onManual2AdjustmentsChanged = onManual2AdjustmentsChanged
                    )
                }
                ActiveDial.NONE -> {
                    // Default Night Mode exposure selector if night mode active, or prompt
                    if (isNightModeActive) {
                        NightExposureControl(
                            durationSeconds = nightDurationSeconds,
                            onDurationChanged = onNightDurationChanged
                        )
                    } else {
                        QuickStatsBar(
                            iso = manualIso,
                            isIsoAuto = isIsoAuto,
                            shutterNs = manualShutterNs,
                            isShutterAuto = isShutterAuto,
                            ev = exposureCompensationEv,
                            isFocusAuto = isFocusAuto,
                            focusDist = manualFocusDistance,
                            kelvin = manualKelvin
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Pro Parameter Tabs: ISO | S | EV | FOCUS | WB | NIGHT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ParamTab(
                title = "ISO",
                value = if (isIsoAuto) "AUTO ($manualIso)" else "$manualIso",
                isSelected = activeDial == ActiveDial.ISO,
                onClick = { onSelectDial(if (activeDial == ActiveDial.ISO) ActiveDial.NONE else ActiveDial.ISO) },
                testTag = "param_tab_iso"
            )

            val shutterSec = manualShutterNs.toDouble() / 1_000_000_000.0
            val shutterLabel = if (isShutterAuto) {
                if (shutterSec >= 1.0) "AUTO (${shutterSec.toInt()}s)" else "AUTO (1/${(1.0 / shutterSec).toInt()})"
            } else {
                if (shutterSec >= 1.0) "${shutterSec.toInt()}s" else "1/${(1.0 / shutterSec).toInt()}"
            }
            ParamTab(
                title = "VEL (S)",
                value = shutterLabel,
                isSelected = activeDial == ActiveDial.SHUTTER,
                onClick = { onSelectDial(if (activeDial == ActiveDial.SHUTTER) ActiveDial.NONE else ActiveDial.SHUTTER) },
                testTag = "param_tab_shutter"
            )

            ParamTab(
                title = "EV",
                value = String.format(Locale.US, "%+.1f", exposureCompensationEv),
                isSelected = activeDial == ActiveDial.EV,
                onClick = { onSelectDial(if (activeDial == ActiveDial.EV) ActiveDial.NONE else ActiveDial.EV) },
                testTag = "param_tab_ev"
            )

            ParamTab(
                title = "FOCO",
                value = if (isFocusAuto) "AF-C" else String.format(Locale.US, "MF %.1fd", manualFocusDistance),
                isSelected = activeDial == ActiveDial.FOCUS,
                onClick = { onSelectDial(if (activeDial == ActiveDial.FOCUS) ActiveDial.NONE else ActiveDial.FOCUS) },
                testTag = "param_tab_focus"
            )

            ParamTab(
                title = "WB",
                value = if (awbPreset == AwbPreset.AUTO) "AWB" else "${manualKelvin}K",
                isSelected = activeDial == ActiveDial.WB,
                onClick = { onSelectDial(if (activeDial == ActiveDial.WB) ActiveDial.NONE else ActiveDial.WB) },
                testTag = "param_tab_wb"
            )

            // Night Mode / Long Exposure Tab (1s a 120s)
            Box(
                modifier = Modifier
                    .testTag("param_tab_night")
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isNightModeActive) Color(0xFF2979FF) else Color(0x331E2228)
                    )
                    .border(
                        1.dp,
                        if (isNightModeActive) Color(0xFF82B1FF) else Color(0x22FFFFFF),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggleNightMode() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Modo Noturno",
                        tint = if (isNightModeActive) Color.White else Color(0xFF82B1FF),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isNightModeActive) "NOITE ${nightDurationSeconds}s" else "NOTURNO",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Primary Bottom Shutter Row: Gallery Thumbnail, Shutter Button, Quick Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery Thumbnail
            Box(
                modifier = Modifier
                    .testTag("gallery_preview_button")
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x44222630))
                    .border(1.5.dp, Color(0x55FFFFFF), RoundedCornerShape(12.dp))
                    .clickable { onOpenGalleryPreview() },
                contentAlignment = Alignment.Center
            ) {
                if (lastMedia != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = lastMedia.megapixels.let { String.format(Locale.US, "%.0fM", it) },
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lastMedia.format,
                            color = Color.White,
                            fontSize = 8.sp
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Última Captura",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Central Shutter Button
            Box(
                modifier = Modifier
                    .testTag("shutter_button")
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .border(3.dp, if (isNightModeActive) Color(0xFF2979FF) else Color(0xFFFFB300), CircleShape)
                    .clickable(enabled = !isCapturing) { onShutterClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCapturing) Color(0xFFFF5252)
                            else if (isNightModeActive) Color(0xFF448AFF)
                            else Color.White
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isNightModeActive) {
                        Text(
                            text = "${nightDurationSeconds}s",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Quick Status Pill
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(60.dp)
            ) {
                Text(
                    text = "OFFLINE",
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SEM GPS",
                    color = Color(0xAAFFFFFF),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun ParamTab(
    title: String,
    value: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFFFB300) else Color(0x331E2228))
            .border(
                1.dp,
                if (isSelected) Color(0xFFFFB300) else Color(0x22FFFFFF),
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                color = if (isSelected) Color.Black else Color(0x88FFFFFF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                color = if (isSelected) Color.Black else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun IsoSliderControl(
    isAuto: Boolean,
    currentIso: Int,
    onIsoChanged: (Int, Boolean) -> Unit
) {
    val isoValues = listOf(50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 6400)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Auto ISO Button
        Box(
            modifier = Modifier
                .testTag("iso_auto_button")
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) Color(0xFFFFB300) else Color(0x44222630))
                .clickable { onIsoChanged(currentIso, true) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "AUTO",
                color = if (isAuto) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Horizontal slider through ISO values
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            isoValues.forEach { iso ->
                val isSelected = !isAuto && currentIso == iso
                Box(
                    modifier = Modifier
                        .testTag("iso_val_$iso")
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFFD700) else Color(0x22FFFFFF))
                        .clickable { onIsoChanged(iso, false) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$iso",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ShutterSpeedControl(
    isAuto: Boolean,
    currentShutterNs: Long,
    onShutterChanged: (Long, Boolean) -> Unit
) {
    val speeds = listOf(
        Pair("1/12000", 1_000_000_000L / 12000),
        Pair("1/8000", 1_000_000_000L / 8000),
        Pair("1/4000", 1_000_000_000L / 4000),
        Pair("1/2000", 1_000_000_000L / 2000),
        Pair("1/1000", 1_000_000_000L / 1000),
        Pair("1/500", 1_000_000_000L / 500),
        Pair("1/250", 1_000_000_000L / 250),
        Pair("1/125", 1_000_000_000L / 125),
        Pair("1/60", 1_000_000_000L / 60),
        Pair("1/30", 1_000_000_000L / 30),
        Pair("1/15", 1_000_000_000L / 15),
        Pair("1/8", 1_000_000_000L / 8),
        Pair("1/4", 1_000_000_000L / 4),
        Pair("1/2", 1_000_000_000L / 2),
        Pair("1s", 1_000_000_000L),
        Pair("2s", 2_000_000_000L),
        Pair("4s", 4_000_000_000L),
        Pair("8s", 8_000_000_000L),
        Pair("15s", 15_000_000_000L),
        Pair("30s", 30_000_000_000L)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .testTag("shutter_auto_button")
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) Color(0xFFFFB300) else Color(0x44222630))
                .clickable { onShutterChanged(currentShutterNs, true) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "AUTO",
                color = if (isAuto) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            speeds.forEach { (display, ns) ->
                val isSelected = !isAuto && currentShutterNs == ns
                Box(
                    modifier = Modifier
                        .testTag("shutter_val_$display")
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFFD700) else Color(0x22FFFFFF))
                        .clickable { onShutterChanged(ns, false) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = display,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun EvSliderControl(
    currentEv: Float,
    onEvChanged: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "-3.0 EV", color = Color(0x88FFFFFF), fontSize = 10.sp)
            Text(
                text = String.format(Locale.US, "Compensação: %+.1f EV", currentEv),
                color = Color(0xFFFFB300),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(text = "+3.0 EV", color = Color(0x88FFFFFF), fontSize = 10.sp)
        }
        Slider(
            value = currentEv,
            onValueChange = { onEvChanged(it) },
            valueRange = -3.0f..3.0f,
            steps = 19, // 0.3 EV steps
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFB300),
                activeTrackColor = Color(0xFFFFB300),
                inactiveTrackColor = Color(0x44FFFFFF)
            ),
            modifier = Modifier.testTag("ev_slider")
        )
    }
}

@Composable
private fun FocusSliderControl(
    isAuto: Boolean,
    focusDistance: Float,
    onFocusChanged: (Float, Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .testTag("focus_auto_button")
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAuto) Color(0xFF00E5FF) else Color(0x44222630))
                .clickable { onFocusChanged(focusDistance, true) }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text(
                text = "AF-C",
                color = if (isAuto) Color.Black else Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "∞ (Infinito)", color = Color(0x88FFFFFF), fontSize = 9.sp)
                Text(
                    text = if (isAuto) "Automático Contínuo" else String.format(Locale.US, "MF: %.2f dpt", focusDistance),
                    color = Color(0xFF00E5FF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(text = "Macro (~10cm)", color = Color(0x88FFFFFF), fontSize = 9.sp)
            }
            Slider(
                value = focusDistance,
                onValueChange = { onFocusChanged(it, false) },
                valueRange = 0.0f..10.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color(0x44FFFFFF)
                ),
                modifier = Modifier.testTag("focus_slider")
            )
        }
    }
}

@Composable
private fun WbKelvinControl(
    preset: AwbPreset,
    kelvin: Int,
    onPresetChanged: (AwbPreset) -> Unit,
    onKelvinChanged: (Int) -> Unit,
    manual2Adjustments: Manual2Adjustments,
    onManual2AdjustmentsChanged: (Manual2Adjustments) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AwbPreset.values().forEach { p ->
                val isSelected = preset == p
                Box(
                    modifier = Modifier
                        .testTag("wb_preset_${p.name}")
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) {
                                if (p == AwbPreset.MANUAL_2) Color(0xFFFFD700) else Color(0xFFFFB300)
                            } else Color(0x22FFFFFF)
                        )
                        .clickable { onPresetChanged(p) }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = p.label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (preset == AwbPreset.CUSTOM_KELVIN) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "2000K", color = Color(0xFFFF8A80), fontSize = 9.sp)
                Slider(
                    value = kelvin.toFloat(),
                    onValueChange = { onKelvinChanged(it.toInt()) },
                    valueRange = 2000f..10000f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFFD54F),
                        activeTrackColor = Color(0xFFFFD54F),
                        inactiveTrackColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .testTag("kelvin_slider")
                )
                Text(text = "10000K", color = Color(0xFF80D8FF), fontSize = 9.sp)
            }
        } else if (preset == AwbPreset.MANUAL_2) {
            Spacer(modifier = Modifier.height(4.dp))
            Manual2ExposureBars(
                adjustments = manual2Adjustments,
                onAdjustmentsChanged = onManual2AdjustmentsChanged
            )
        }
    }
}

@Composable
private fun Manual2ExposureBars(
    adjustments: Manual2Adjustments,
    onAdjustmentsChanged: (Manual2Adjustments) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color(0x33FFD700), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("manual2_exposure_bars")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "BARRA MANUAL 2 • BRILHO / CONTRASTE / SOMBRAS",
                color = Color(0xFFFFD700),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
                    .clickable { onAdjustmentsChanged(Manual2Adjustments()) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "REDEFINIR",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        FineAdjustmentBar(
            label = "BRILHO",
            value = adjustments.brightness,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(brightness = it)) }
        )

        FineAdjustmentBar(
            label = "CONTRASTE",
            value = adjustments.contrast,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(contrast = it)) }
        )

        FineAdjustmentBar(
            label = "SOMBRAS",
            value = adjustments.shadows,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(shadows = it)) }
        )

        FineAdjustmentBar(
            label = "EXPOSIÇÃO",
            value = adjustments.highlights,
            onValueChange = { onAdjustmentsChanged(adjustments.copy(highlights = it)) }
        )
    }
}

@Composable
private fun FineAdjustmentBar(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xCCFFFFFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(68.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFFFD700),
                activeTrackColor = Color(0xFFFFD700),
                inactiveTrackColor = Color(0x33FFFFFF)
            ),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = String.format(Locale.US, "%+d%%", value.toInt()),
            color = if (value != 0f) Color(0xFFFFD700) else Color(0x88FFFFFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
private fun NightExposureControl(
    durationSeconds: Int,
    onDurationChanged: (Int) -> Unit
) {
    val nightSteps = listOf(1, 2, 3, 5, 8, 10, 15, 30, 60, 120)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TEMPO DE ABERTURA DO OBTURADOR (CAPTANDO LUZ):",
                color = Color(0xFF82B1FF),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "${durationSeconds}s de captação contínua",
                color = Color(0xFFFFD54F),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            nightSteps.forEach { sec ->
                val isSelected = durationSeconds == sec
                Box(
                    modifier = Modifier
                        .testTag("night_step_${sec}s")
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF2979FF) else Color(0x331E2228))
                        .border(
                            1.dp,
                            if (isSelected) Color(0xFF82B1FF) else Color(0x22FFFFFF),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onDurationChanged(sec) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${sec}s",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickStatsBar(
    iso: Int,
    isIsoAuto: Boolean,
    shutterNs: Long,
    isShutterAuto: Boolean,
    ev: Float,
    isFocusAuto: Boolean,
    focusDist: Float,
    kelvin: Int
) {
    val sec = shutterNs.toDouble() / 1_000_000_000.0
    val shutterText = if (sec >= 1.0) "${sec.toInt()}s" else "1/${(1.0 / sec).toInt()}s"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x3312151A))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "ISO", color = Color(0x88FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = if (isIsoAuto) "A $iso" else "$iso",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column {
            Text(text = "OBTURADOR", color = Color(0x88FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = if (isShutterAuto) "A $shutterText" else shutterText,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column {
            Text(text = "EV", color = Color(0x88FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = String.format(Locale.US, "%+.1f", ev),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column {
            Text(text = "FOCO", color = Color(0x88FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = if (isFocusAuto) "AF-C" else String.format(Locale.US, "%.1fd", focusDist),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        Column {
            Text(text = "WB", color = Color(0x88FFFFFF), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "${kelvin}K",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
