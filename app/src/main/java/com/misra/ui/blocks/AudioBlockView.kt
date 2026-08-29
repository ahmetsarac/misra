package com.misra.ui.blocks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misra.R
import com.misra.domain.model.BlockPayload
import com.misra.domain.model.formatDuration
import com.misra.ui.components.SeekTrack
import com.misra.ui.workspace.PlaybackState

@Composable
fun AudioBlockView(
    blockId: String,
    payload: BlockPayload.Audio,
    selected: Boolean,
    playback: PlaybackState,
    onSelect: () -> Unit,
    onDragStart: () -> Unit,
    onVerticalDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRename: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val card = MaterialTheme.colorScheme.surfaceVariant
    val active = playback.blockId == blockId
    val playing = active && playback.isPlaying
    val position = if (active) playback.positionMs else 0L
    val duration = when {
        active && playback.durationMs > 0L -> playback.durationMs
        else -> payload.durationMs
    }

    BlockRow(
        selected = selected,
        ink = ink,
        fillColor = card,
        onDragStart = onDragStart,
        onVerticalDrag = onVerticalDrag,
        onDragEnd = onDragEnd,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onSelect
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) stringResource(R.string.pause) else stringResource(R.string.play),
                    tint = accent,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                onSelect()
                                onPlayPause()
                            }
                        )
                )
                BasicTextField(
                    value = payload.name,
                    onValueChange = onRename,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(accent),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatDuration(if (active) position else duration),
                    color = ink.copy(alpha = 0.55f),
                    fontSize = 12.sp
                )
            }
            SeekTrack(
                modifier = Modifier.padding(start = 8.dp),
                positionMs = position,
                durationMs = duration,
                onSeek = {
                    onSelect()
                    onSeek(it)
                },
                trackColor = ink.copy(alpha = 0.16f),
                progressColor = accent
            )
        }
    }
}
