package com.misra.ui.workspace

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misra.R
import com.misra.domain.model.formatDuration
import com.misra.ui.theme.RecordRed

@Composable
fun WorkspaceChrome(
    title: String,
    recording: RecordingUiState,
    selectedAudioId: String?,
    userMessage: String?,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onMic: () -> Unit,
    onAddAudio: () -> Unit,
    onPauseResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val card = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(card)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = ink,
                    modifier = Modifier.size(22.dp)
                )
            }
            val titleValue = if (title.equals("Untitled", ignoreCase = true)) "" else title
            BasicTextField(
                value = titleValue,
                onValueChange = onTitleChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = ink,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (titleValue.isEmpty()) {
                            Text(
                                text = stringResource(R.string.song_title),
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Medium,
                                fontSize = 20.sp,
                                color = ink.copy(alpha = 0.32f)
                            )
                        }
                        inner()
                    }
                }
            )
            if (recording.active) {
                RecordingPill(
                    recording = recording,
                    onPauseResume = onPauseResumeRecording,
                    onStop = onStopRecording,
                    onDiscard = onDiscardRecording
                )
            } else {
                CircleButton(
                    background = RecordRed,
                    onClick = onMic
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = stringResource(R.string.record),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                CircleButton(
                    background = MaterialTheme.colorScheme.primary,
                    onClick = onAddAudio
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_audio),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = selectedAudioId != null && !recording.active,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(card)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.duplicate),
                    color = ink,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onDuplicate)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
                Text(
                    text = stringResource(R.string.delete),
                    color = RecordRed,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = userMessage != null,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Text(
                text = userMessage.orEmpty(),
                color = paper,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(bottom = if (selectedAudioId != null) 56.dp else 0.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ink.copy(alpha = 0.88f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun RecordingPill(
    recording: RecordingUiState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onDiscard: () -> Unit
) {
    val pulse by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "rec-alpha"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(RecordRed.copy(alpha = if (recording.isPaused) 0.4f else pulse))
        )
        Text(
            text = formatDuration(recording.elapsedMs),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            imageVector = if (recording.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
            contentDescription = if (recording.isPaused) stringResource(R.string.resume) else stringResource(R.string.pause),
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onPauseResume)
        )
        Icon(
            imageVector = Icons.Rounded.Stop,
            contentDescription = stringResource(R.string.stop),
            tint = RecordRed,
            modifier = Modifier
                .size(22.dp)
                .clickable(onClick = onStop)
        )
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.discard),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onDiscard)
        )
    }
}

@Composable
private fun CircleButton(
    background: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
        content = { content() }
    )
}
