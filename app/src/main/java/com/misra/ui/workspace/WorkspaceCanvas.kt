package com.misra.ui.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.misra.domain.model.BlockPayload
import com.misra.domain.model.SongDocument
import com.misra.ui.blocks.AudioBlockView
import com.misra.ui.blocks.AudioDropPlaceholder
import com.misra.ui.blocks.TextBlockView

@Composable
fun WorkspaceDocument(
    document: SongDocument,
    interaction: InteractionState,
    playback: PlaybackState,
    onDocumentTap: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onNudgeAudio: (id: String, steps: Int) -> Unit,
    onTextChange: (id: String, content: String) -> Unit,
    onCursorChange: (id: String, start: Int, end: Int) -> Unit,
    onBackspaceAtStart: (String) -> Unit,
    onAudioNameChange: (id: String, name: String) -> Unit,
    onPlayPause: (String) -> Unit,
    onSeek: (id: String, positionMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val paper = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val swapThreshold = with(density) { 36.dp.toPx() }
    val dragElevation = with(density) { 10.dp.toPx() }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    var fingerDelta by remember { mutableFloatStateOf(0f) }
    val layoutYs = remember { mutableStateMapOf<String, Float>() }
    val tapSource = remember { MutableInteractionSource() }
    val lastTextId = document.blocks.lastOrNull { it.payload is BlockPayload.Text }?.id

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(paper)
            .clickable(
                interactionSource = tapSource,
                indication = null,
                onClick = onDocumentTap
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 8.dp, end = 20.dp, top = 88.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        document.blocks.forEach { block ->
            key(block.id) {
                when (val payload = block.payload) {
                    is BlockPayload.Text -> TextBlockView(
                        payload = payload,
                        restoreFocusGen = interaction.restoreFocusGen,
                        isRestoreTarget = interaction.activeTextId == block.id,
                        fillsCanvas = block.id == lastTextId,
                        cursor = interaction.cursors[block.id]
                            ?: CursorRange(payload.content.length, payload.content.length),
                        onSelect = { onSelectText(block.id) },
                        onTextChange = { onTextChange(block.id, it) },
                        onCursorChange = { start, end -> onCursorChange(block.id, start, end) },
                        onBackspaceAtStart = { onBackspaceAtStart(block.id) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    is BlockPayload.Audio -> {
                        val dragging = draggingId == block.id
                        val translationY = if (dragging) {
                            dragStartY + fingerDelta - (layoutYs[block.id] ?: dragStartY)
                        } else {
                            0f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (dragging) 1f else 0f)
                                .onGloballyPositioned { coords ->
                                    layoutYs[block.id] = coords.positionInParent().y
                                }
                        ) {
                            if (dragging) {
                                AudioDropPlaceholder(Modifier.matchParentSize())
                            }
                            AudioBlockView(
                                blockId = block.id,
                                payload = payload,
                                selected = interaction.selectedAudioId == block.id,
                                playback = playback,
                                onSelect = { onSelectAudio(block.id) },
                                onDragStart = {
                                    onSelectAudio(block.id)
                                    draggingId = block.id
                                    dragStartY = layoutYs[block.id] ?: 0f
                                    fingerDelta = 0f
                                    dragAccum = 0f
                                },
                                onVerticalDrag = { dy ->
                                    fingerDelta += dy
                                    dragAccum += dy
                                    val steps = (dragAccum / swapThreshold).toInt()
                                    if (steps != 0) {
                                        onNudgeAudio(block.id, steps)
                                        dragAccum -= steps * swapThreshold
                                    }
                                },
                                onDragEnd = {
                                    draggingId = null
                                    fingerDelta = 0f
                                    dragAccum = 0f
                                },
                                onPlayPause = { onPlayPause(block.id) },
                                onSeek = { onSeek(block.id, it) },
                                onRename = { onAudioNameChange(block.id, it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        this.translationY = translationY
                                        shadowElevation = if (dragging) dragElevation else 0f
                                        shape = RoundedCornerShape(12.dp)
                                        clip = false
                                        alpha = if (dragging) 0.97f else 1f
                                    }
                            )
                        }
                    }
                }
            }
        }
        }
    }
}
