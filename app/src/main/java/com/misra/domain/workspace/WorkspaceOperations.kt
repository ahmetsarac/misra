package com.misra.domain.workspace

import com.misra.domain.model.BlockPayload
import com.misra.domain.model.CanvasBlock
import com.misra.domain.model.SongDocument
import java.util.UUID

fun newBlockId(): String = UUID.randomUUID().toString()

fun SongDocument.block(id: String): CanvasBlock? = blocks.firstOrNull { it.id == id }

fun SongDocument.update(id: String, now: Long, transform: (CanvasBlock) -> CanvasBlock): SongDocument {
    val index = blocks.indexOfFirst { it.id == id }
    if (index < 0) return this
    val next = blocks.toMutableList()
    next[index] = transform(next[index])
    return copy(blocks = next, updatedAt = now)
}

fun SongDocument.removeAudio(id: String, now: Long): SongDocument {
    val target = block(id) ?: return this
    if (target.payload !is BlockPayload.Audio) return this
    return copy(
        blocks = blocks.filterNot { it.id == id },
        updatedAt = now
    ).asLyricFlow(now)
}

fun SongDocument.nudgeAudio(id: String, steps: Int, now: Long): SongDocument {
    if (steps == 0) return this
    val target = block(id) ?: return this
    if (target.payload !is BlockPayload.Audio) return this
    val from = blocks.indexOfFirst { it.id == id }
    val to = (from + steps).coerceIn(0, blocks.lastIndex)
    if (from == to) return this
    val next = blocks.toMutableList()
    val block = next.removeAt(from)
    next.add(to, block)
    return copy(blocks = next, updatedAt = now).asLyricFlow(now)
}

fun SongDocument.duplicateAudio(id: String, now: Long): SongDocument {
    val index = blocks.indexOfFirst { it.id == id }
    if (index < 0) return this
    val current = blocks[index]
    if (current.payload !is BlockPayload.Audio) return this
    val duplicated = current.copy(id = newBlockId())
    val next = blocks.toMutableList()
    next.add(index + 1, duplicated)
    return copy(blocks = next, updatedAt = now).asLyricFlow(now)
}

fun SongDocument.insertAudioAtCursor(
    textId: String?,
    cursor: Int,
    audio: CanvasBlock,
    now: Long
): AudioInsertResult {
    require(audio.payload is BlockPayload.Audio)
    val flow = asLyricFlow(now)
    val targetIndex = textId?.let { id -> flow.blocks.indexOfFirst { it.id == id } }
        ?.takeIf { it >= 0 && flow.blocks[it].payload is BlockPayload.Text }
        ?: flow.blocks.indexOfLast { it.payload is BlockPayload.Text }
    if (targetIndex < 0) {
        val after = textBlock()
        val next = listOf(textBlock(), audio, after)
        return AudioInsertResult(
            document = copy(blocks = next, updatedAt = now),
            continueTextId = after.id
        )
    }
    val textBlock = flow.blocks[targetIndex]
    val text = textBlock.payload as BlockPayload.Text
    val at = cursor.coerceIn(0, text.content.length)
    val after = textBlock(text.content.substring(at))
    val next = flow.blocks.toMutableList()
    next[targetIndex] = textBlock.copy(payload = BlockPayload.Text(text.content.substring(0, at)))
    next.add(targetIndex + 1, audio)
    next.add(targetIndex + 2, after)
    return AudioInsertResult(
        document = copy(blocks = next, updatedAt = now).asLyricFlow(now),
        continueTextId = after.id
    )
}

fun SongDocument.insertAudioAfter(
    audioId: String,
    audio: CanvasBlock,
    now: Long
): AudioInsertResult {
    require(audio.payload is BlockPayload.Audio)
    val index = blocks.indexOfFirst { it.id == audioId }
    if (index < 0) return insertAudioAtCursor(null, 0, audio, now)
    val next = blocks.toMutableList()
    next.add(index + 1, audio)
    val document = copy(blocks = next, updatedAt = now).asLyricFlow(now)
    val continueId = document.blocks
        .drop(document.blocks.indexOfFirst { it.id == audio.id } + 1)
        .firstOrNull { it.payload is BlockPayload.Text }
        ?.id
        ?: document.blocks.last().id
    return AudioInsertResult(document = document, continueTextId = continueId)
}

fun SongDocument.asLyricFlow(now: Long = updatedAt): SongDocument {
    val merged = mutableListOf<CanvasBlock>()
    for (block in blocks) {
        val payload = block.payload
        val last = merged.lastOrNull()
        if (payload is BlockPayload.Text && last?.payload is BlockPayload.Text) {
            val previous = last.payload as BlockPayload.Text
            merged[merged.lastIndex] = last.copy(
                payload = BlockPayload.Text(joinText(previous.content, payload.content))
            )
        } else {
            merged += block
        }
    }
    if (merged.isEmpty()) {
        merged += textBlock()
    }
    if (merged.first().payload !is BlockPayload.Text) {
        merged.add(0, textBlock())
    }
    if (merged.last().payload !is BlockPayload.Text) {
        merged += textBlock()
    }
    return copy(blocks = merged, updatedAt = now)
}

fun SongDocument.normalizedVertical(): SongDocument {
    val looksSpatial = blocks.any { it.y != 0f }
    val ordered = if (looksSpatial) copy(blocks = blocks.sortedBy { it.y }) else this
    return ordered.asLyricFlow(ordered.updatedAt)
}

fun textBlock(content: String = ""): CanvasBlock = CanvasBlock(
    id = newBlockId(),
    payload = BlockPayload.Text(content)
)

fun audioBlock(
    audioId: String,
    name: String,
    durationMs: Long,
    origin: String
): CanvasBlock = CanvasBlock(
    id = newBlockId(),
    payload = BlockPayload.Audio(
        audioId = audioId,
        name = name,
        durationMs = durationMs,
        origin = origin
    )
)

fun emptySong(now: Long): SongDocument = SongDocument(
    id = newBlockId(),
    title = "Untitled",
    updatedAt = now,
    blocks = listOf(textBlock())
)

fun joinText(first: String, second: String): String = when {
    first.isEmpty() -> second
    second.isEmpty() -> first
    else -> first.trimEnd() + "\n" + second.trimStart()
}

data class AudioInsertResult(
    val document: SongDocument,
    val continueTextId: String
)
