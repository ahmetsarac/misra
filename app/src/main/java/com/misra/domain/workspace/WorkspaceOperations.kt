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
    val direction = if (steps > 0) 1 else -1
    var document = asLyricFlow(now)
    repeat(kotlin.math.abs(steps)) {
        val next = document.nudgeAudioByOne(id, direction, now)
        if (next.blocks.map { it.id } == document.blocks.map { it.id }) return document
        document = next
    }
    return document
}

private fun SongDocument.nudgeAudioByOne(id: String, direction: Int, now: Long): SongDocument {
    val flow = asLyricFlow(now)
    val from = flow.blocks.indexOfFirst { it.id == id }
    if (from < 0) return this
    val destination = if (direction > 0) flow.nextNudgeIndex(from) else flow.prevNudgeIndex(from)
    if (destination == null || destination == from) return flow
    val audio = flow.blocks[from]
    val remaining = flow.blocks.filterNot { it.id == id }.toMutableList()
    val insertAt = if (destination > from) destination - 1 else destination
    remaining.add(insertAt.coerceIn(0, remaining.size), audio)
    return copy(blocks = remaining, updatedAt = now).asLyricFlow(now)
}

private fun SongDocument.nextNudgeIndex(from: Int): Int? {
    val nextSlot = sandwichEmptyIndices().firstOrNull { it > from + 1 }
    if (nextSlot != null) return nextSlot
    val lastLyric = blocks.indices.lastOrNull { blocks[it].isLyricText() } ?: return null
    return (lastLyric + 1).takeIf { it != from && it > from }
}

private fun SongDocument.prevNudgeIndex(from: Int): Int? {
    val prevSlot = sandwichEmptyIndices().lastOrNull { it < from - 1 }
    if (prevSlot != null) return prevSlot
    val firstLyric = blocks.indices.firstOrNull { blocks[it].isLyricText() } ?: return null
    return firstLyric.takeIf { it < from }
}

private fun SongDocument.sandwichEmptyIndices(): List<Int> {
    return blocks.indices.filter { index ->
        blocks[index].isBlankText() &&
            blocks.take(index).any { it.isLyricText() } &&
            blocks.drop(index + 1).any { it.isLyricText() }
    }
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
    val nextIsBlank = flow.blocks.getOrNull(targetIndex + 1)?.isBlankText() == true
    if (text.content.isBlank() || (at >= text.content.length && nextIsBlank)) {
        val occupyAt = if (text.content.isBlank()) targetIndex else targetIndex + 1
        val next = flow.blocks.toMutableList()
        next.add(occupyAt, audio)
        val document = copy(blocks = next, updatedAt = now).asLyricFlow(now)
        val continueId = document.blocks
            .drop(document.blocks.indexOfFirst { it.id == audio.id } + 1)
            .firstOrNull { it.payload is BlockPayload.Text }
            ?.id
            ?: document.blocks.last().id
        return AudioInsertResult(document = document, continueTextId = continueId)
    }
    val (before, afterText) = splitTextAtLine(text.content, cursor)
    val after = textBlock(afterText)
    val next = flow.blocks.toMutableList()
    next[targetIndex] = textBlock.copy(payload = BlockPayload.Text(before))
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
    val exploded = mutableListOf<CanvasBlock>()
    for (block in blocks) {
        val payload = block.payload
        if (payload is BlockPayload.Text) {
            explodeText(payload.content).forEachIndexed { index, part ->
                exploded += if (index == 0) {
                    block.copy(payload = BlockPayload.Text(part))
                } else {
                    textBlock(part)
                }
            }
        } else {
            exploded += block
        }
    }
    val merged = mutableListOf<CanvasBlock>()
    for (block in exploded) {
        val payload = block.payload
        val last = merged.lastOrNull()
        val lastText = last?.payload as? BlockPayload.Text
        val thisText = payload as? BlockPayload.Text
        if (
            thisText != null &&
            lastText != null &&
            thisText.content.isNotBlank() &&
            lastText.content.isNotBlank()
        ) {
            merged[merged.lastIndex] = last.copy(
                payload = BlockPayload.Text(joinText(lastText.content, thisText.content))
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

internal fun splitTextAtLine(content: String, cursor: Int): Pair<String, String> {
    val at = cursor.coerceIn(0, content.length)
    val lineStart = content.lastIndexOf('\n', at - 1).let { if (it < 0) 0 else it + 1 }
    val breakAt = content.indexOf('\n', at)
    val lineEnd = if (breakAt < 0) content.length else breakAt
    val afterStart = if (breakAt < 0) content.length else breakAt + 1
    val lineIsBlank = content.substring(lineStart, lineEnd).isBlank()
    val before = if (lineIsBlank) {
        content.substring(0, lineStart).trimEnd('\n')
    } else {
        content.substring(0, lineEnd)
    }
    return before to content.substring(afterStart)
}

internal fun explodeText(content: String): List<String> {
    if (content.isEmpty()) return listOf("")
    val parts = mutableListOf<String>()
    val buffer = mutableListOf<String>()
    fun flush() {
        if (buffer.isNotEmpty()) {
            parts += buffer.joinToString("\n")
            buffer.clear()
        }
    }
    for (line in content.split('\n')) {
        if (line.isBlank()) {
            flush()
            parts += ""
        } else {
            buffer += line
        }
    }
    flush()
    if (parts.isEmpty()) parts += ""
    return parts
}

fun CanvasBlock.isBlankText(): Boolean {
    val text = payload as? BlockPayload.Text ?: return false
    return text.content.isBlank()
}

fun CanvasBlock.isLyricText(): Boolean {
    val text = payload as? BlockPayload.Text ?: return false
    return text.content.isNotBlank()
}

fun SongDocument.hidesBlankSlot(index: Int): Boolean {
    if (index !in blocks.indices || !blocks[index].isBlankText()) return false
    val adjacentAudio =
        blocks.getOrNull(index - 1)?.payload is BlockPayload.Audio ||
            blocks.getOrNull(index + 1)?.payload is BlockPayload.Audio
    if (!adjacentAudio) return false
    val lyricBefore = blocks.take(index).any { it.isLyricText() }
    val lyricAfter = blocks.drop(index + 1).any { it.isLyricText() }
    return lyricBefore && lyricAfter
}

fun joinText(first: String, second: String): String = when {
    first.isEmpty() -> second
    second.isEmpty() -> first
    else -> first.trimEnd('\n') + "\n" + second.trimStart('\n')
}

data class AudioInsertResult(
    val document: SongDocument,
    val continueTextId: String
)
