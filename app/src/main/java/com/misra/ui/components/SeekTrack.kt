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
                    onSeek(seekFromX(offset.x, size.width.toFloat(), durationMs, 5.dp.toPx()))
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onSeek(seekFromX(change.position.x, size.width.toFloat(), durationMs, 5.dp.toPx()))
                }
            }
    ) {
        val y = size.height / 2f
        val inset = 5.dp.toPx()
        val startX = inset
        val endX = size.width - inset
        val start = Offset(startX, y)
        val end = Offset(endX, y)
        drawLine(trackColor, start, end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        val thumbX = startX + (endX - startX) * fraction
        if (fraction > 0f) {
            drawLine(progressColor, start, Offset(thumbX, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(progressColor, radius = inset, center = Offset(thumbX, y))
    }
}

@Composable
fun ValueTrack(
    value: Float,
    onValue: (Float) -> Unit,
    trackColor: Color,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    val fraction = value.coerceIn(0f, 1f)
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValue(fractionFromX(offset.x, size.width.toFloat(), 5.dp.toPx()))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValue(fractionFromX(change.position.x, size.width.toFloat(), 5.dp.toPx()))
                }
            }
    ) {
        val y = size.height / 2f
        val inset = 5.dp.toPx()
        val startX = inset
        val endX = size.width - inset
        val start = Offset(startX, y)
        drawLine(trackColor, start, Offset(endX, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        val thumbX = startX + (endX - startX) * fraction
        if (fraction > 0f) {
            drawLine(progressColor, start, Offset(thumbX, y), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
        }
        drawCircle(progressColor, radius = inset, center = Offset(thumbX, y))
    }
}

private fun seekFromX(x: Float, width: Float, durationMs: Long, inset: Float): Long {
    return (fractionFromX(x, width, inset) * durationMs).toLong()
}

private fun fractionFromX(x: Float, width: Float, inset: Float): Float {
    val usable = (width - inset * 2f).coerceAtLeast(1f)
    return ((x - inset) / usable).coerceIn(0f, 1f)
}
