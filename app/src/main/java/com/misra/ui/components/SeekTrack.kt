package com.misra.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun SeekTrack(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    val fraction = if (durationMs <= 0L) 0f else (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    onSeek(seekFromX(offset.x, size.width.toFloat(), durationMs))
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onSeek(seekFromX(change.position.x, size.width.toFloat(), durationMs))
                }
            }
    ) {
        val y = size.height / 2f
        val start = Offset(0f, y)
        val end = Offset(size.width, y)
        drawLine(trackColor, start, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        val thumbX = size.width * fraction
        if (thumbX > 0f) {
            drawLine(progressColor, start, Offset(thumbX, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(progressColor, radius = 5.dp.toPx(), center = Offset(thumbX, y))
    }
}

@Composable
fun VolumeTrack(
    volume: Float,
    onVolume: (Float) -> Unit,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .height(20.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onVolume((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onVolume((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
    ) {
        val y = size.height / 2f
        val start = Offset(0f, y)
        drawLine(trackColor, start, Offset(size.width, y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        val thumbX = size.width * volume.coerceIn(0f, 1f)
        drawLine(progressColor, start, Offset(thumbX, y), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(progressColor, radius = 4.dp.toPx(), center = Offset(thumbX, y))
    }
}

private fun seekFromX(x: Float, width: Float, durationMs: Long): Long {
    val fraction = if (width <= 0f) 0f else (x / width).coerceIn(0f, 1f)
    return (fraction * durationMs).toLong()
}
