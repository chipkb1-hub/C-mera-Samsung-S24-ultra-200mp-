package com.example.ui.components

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@Composable
fun ViewfinderLayout(
    isGridEnabled: Boolean,
    isZebraEnabled: Boolean,
    isFocusPeakingEnabled: Boolean,
    highlightClippingPercent: Float,
    isAeAfLocked: Boolean,
    currentZoom: Float,
    isShutterFlashing: Boolean,
    onSurfaceTextureAvailable: (SurfaceTexture, Int, Int) -> Unit,
    onTapToFocus: (Float, Float) -> Unit,
    onPinchZoom: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var focusBoxOffset by remember { mutableStateOf<Offset?>(null) }
    var focusBoxAlpha by remember { mutableFloatStateOf(0f) }

    // Animated zebra stripe offset
    val zebraAnim = remember { Animatable(0f) }
    LaunchedEffect(isZebraEnabled) {
        if (isZebraEnabled) {
            zebraAnim.animateTo(
                targetValue = 40f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("viewfinder_container")
            .pointerInput(currentZoom) {
                detectTransformGestures { _, _, zoomChange, _ ->
                    if (zoomChange != 1.0f) {
                        val newZoom = (currentZoom * zoomChange).coerceIn(0.6f, 10.0f)
                        onPinchZoom(newZoom)
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    focusBoxOffset = offset
                    focusBoxAlpha = 1f
                    onTapToFocus(offset.x, offset.y)
                }
            }
    ) {
        // Native TextureView for Camera2 Preview
        AndroidView(
            factory = { ctx ->
                TextureView(ctx).apply {
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            onSurfaceTextureAvailable(surface, width, height)
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            modifier = Modifier.fillMaxSize().testTag("camera_texture_view")
        )

        // Canvas Overlays: Grid, Level, Zebra, Focus Peaking, Focus Box
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // 1. Grid of thirds
            if (isGridEnabled) {
                val gridColor = Color(0x55FFFFFF)
                val strokeWidth = 1.dp.toPx()

                // Vertical grid lines
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
                drawLine(gridColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth)

                // Horizontal grid lines
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
                drawLine(gridColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth)

                // Center crosshairs
                val center = Offset(w / 2f, h / 2f)
                val chSize = 14.dp.toPx()
                drawLine(Color(0x77FFFFFF), Offset(center.x - chSize, center.y), Offset(center.x + chSize, center.y), 1.5f)
                drawLine(Color(0x77FFFFFF), Offset(center.x, center.y - chSize), Offset(center.x, center.y + chSize), 1.5f)

                // Electronic Level Line (centered horizon)
                val levelWidth = 80.dp.toPx()
                drawLine(
                    Color(0xFF39FF14),
                    Offset(center.x - levelWidth, center.y),
                    Offset(center.x - levelWidth / 3f, center.y),
                    2.dp.toPx()
                )
                drawLine(
                    Color(0xFF39FF14),
                    Offset(center.x + levelWidth / 3f, center.y),
                    Offset(center.x + levelWidth, center.y),
                    2.dp.toPx()
                )
            }

            // 2. Zebra Highlight Warning (Overexposure stripes)
            if (isZebraEnabled && highlightClippingPercent > 0.5f) {
                val zebraSpacing = 20.dp.toPx()
                val offset = zebraAnim.value
                val zebraColor = Color(0x88FFCC00)
                val stripeWidth = 3.dp.toPx()

                val clippingAreaHeight = h * (highlightClippingPercent / 100f).coerceIn(0.15f, 0.45f)
                var x = -clippingAreaHeight + offset
                while (x < w + clippingAreaHeight) {
                    drawLine(
                        zebraColor,
                        Offset(x, 0f),
                        Offset(x + clippingAreaHeight, clippingAreaHeight),
                        stripeWidth
                    )
                    x += zebraSpacing
                }
            }

            // 3. Focus Peaking Indicator (Neon Green high-contrast edges)
            if (isFocusPeakingEnabled) {
                val peakColor = Color(0xFF00FF66)
                val peakStroke = 1.5.dp.toPx()
                val centerX = w / 2f
                val centerY = h / 2f

                // Subject focus peaking bracket
                val boxW = 120.dp.toPx()
                val boxH = 120.dp.toPx()
                val corner = 20.dp.toPx()

                val path = Path().apply {
                    // Top-Left
                    moveTo(centerX - boxW / 2, centerY - boxH / 2 + corner)
                    lineTo(centerX - boxW / 2, centerY - boxH / 2)
                    lineTo(centerX - boxW / 2 + corner, centerY - boxH / 2)

                    // Top-Right
                    moveTo(centerX + boxW / 2 - corner, centerY - boxH / 2)
                    lineTo(centerX + boxW / 2, centerY - boxH / 2)
                    lineTo(centerX + boxW / 2, centerY - boxH / 2 + corner)

                    // Bottom-Left
                    moveTo(centerX - boxW / 2, centerY + boxH / 2 - corner)
                    lineTo(centerX - boxW / 2, centerY + boxH / 2)
                    lineTo(centerX - boxW / 2 + corner, centerY + boxH / 2)

                    // Bottom-Right
                    moveTo(centerX + boxW / 2 - corner, centerY + boxH / 2)
                    lineTo(centerX + boxW / 2, centerY + boxH / 2)
                    lineTo(centerX + boxW / 2, centerY + boxH / 2 - corner)
                }

                drawPath(path, peakColor, style = Stroke(width = peakStroke))
            }

            // 4. Tap-To-Focus Reticle
            focusBoxOffset?.let { pos ->
                if (focusBoxAlpha > 0f) {
                    val reticleColor = if (isAeAfLocked) Color(0xFFFFB300) else Color(0xFF00E5FF)
                    val reticleSize = 56.dp.toPx()
                    val stroke = 2.dp.toPx()

                    drawRect(
                        color = reticleColor.copy(alpha = focusBoxAlpha),
                        topLeft = Offset(pos.x - reticleSize / 2f, pos.y - reticleSize / 2f),
                        size = Size(reticleSize, reticleSize),
                        style = Stroke(width = stroke)
                    )

                    drawCircle(
                        color = reticleColor.copy(alpha = focusBoxAlpha),
                        radius = 4.dp.toPx(),
                        center = pos
                    )
                }
            }
        }

        // 5. Floating Zoom Level Indicator (e.g. 1.0x, 2.5x, 10.0x)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .background(Color(0x99000000), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("floating_zoom_indicator")
        ) {
            Text(
                text = String.format(Locale.US, "%.1fx", currentZoom),
                color = if (currentZoom >= 10.0f) Color(0xFFFFD700) else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // 6. Tactile Shutter Flash Effect (Flashes white briefly on capture)
        AnimatedVisibility(
            visible = isShutterFlashing,
            enter = fadeIn(tween(40)),
            exit = fadeOut(tween(140))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.85f))
            )
        }
    }
}
