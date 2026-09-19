package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.HistogramData

@Composable
fun HistogramView(
    data: HistogramData,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("histogram_overlay")
            .size(width = 150.dp, height = 75.dp)
            .background(Color(0x990A0C10), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
            .padding(4.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val step = w / 63f

            // Grid reference lines
            val gridColor = Color(0x22FFFFFF)
            drawLine(gridColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), 1f)
            drawLine(gridColor, Offset(w * 0.50f, 0f), Offset(w * 0.50f, h), 1f)
            drawLine(gridColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), 1f)
            drawLine(gridColor, Offset(0f, h * 0.50f), Offset(w, h * 0.50f), 1f)

            // Draw Red curve
            drawHistogramCurve(data.r, Color(0xAAFF4444), step, h)
            // Draw Green curve
            drawHistogramCurve(data.g, Color(0xAA44FF44), step, h)
            // Draw Blue curve
            drawHistogramCurve(data.b, Color(0xAA4488FF), step, h)
            // Draw Luma curve (white)
            drawHistogramCurve(data.luma, Color(0xDDFFFFFF), step, h)
        }

        // Highlight clipping alert if overexposed
        if (data.highlightClippingPercent > 1.0f) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(Color(0xCCFF3333), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "ZEBRA ${data.highlightClippingPercent.toInt()}%",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Label
        Text(
            text = "RGB+Y HIST",
            color = Color(0x88FFFFFF),
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogramCurve(
    values: FloatArray,
    color: Color,
    step: Float,
    height: Float
) {
    val path = Path()
    var started = false

    for (i in values.indices) {
        val x = i * step
        val y = height - (values[i].coerceIn(0f, 1f) * (height - 2f))
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5f)
    )
}
