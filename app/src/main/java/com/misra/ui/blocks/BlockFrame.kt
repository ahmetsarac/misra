package com.misra.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misra.R

private val BlockHandleWidth = 22.dp
private val CardCorner = 12.dp

@Composable
fun BlockRow(
    selected: Boolean,
    ink: Color,
    onDragStart: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    fillColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.DragIndicator,
            contentDescription = stringResource(R.string.drag_block),
            tint = ink.copy(alpha = if (selected) 0.45f else 0.22f),
            modifier = Modifier
                .padding(top = 4.dp)
                .size(BlockHandleWidth, 28.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { onDragStart() },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            onVerticalDrag(dragAmount)
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd
                    )
                }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .then(
                    if (fillColor == Color.Transparent) Modifier
                    else Modifier
                        .clip(RoundedCornerShape(CardCorner))
                        .background(fillColor)
                )
        ) {
            content()
        }
    }
}

@Composable
fun AudioDropPlaceholder(
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val radius = CardCorner
    Row(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.width(BlockHandleWidth))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .drawBehind {
                    val corner = CornerRadius(radius.toPx())
                    drawRoundRect(
                        color = ink.copy(alpha = 0.06f),
                        cornerRadius = corner
                    )
                    drawRoundRect(
                        color = ink.copy(alpha = 0.28f),
                        cornerRadius = corner,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(12.dp.toPx(), 8.dp.toPx())
                            )
                        )
                    )
                }
        )
    }
}
