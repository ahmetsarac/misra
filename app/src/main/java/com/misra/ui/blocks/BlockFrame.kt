package com.misra.ui.blocks

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.misra.R

@Composable
fun BlockRow(
    selected: Boolean,
    ink: Color,
    fillColor: Color = Color.Transparent,
    onSelect: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
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
                .size(22.dp, 28.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { onSelect() },
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(fillColor)
                )
        ) {
            content()
        }
    }
}
