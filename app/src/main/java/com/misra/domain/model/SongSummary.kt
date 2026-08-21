package com.misra.domain.model

data class SongSummary(
    val id: String,
    val title: String,
    val preview: String,
    val updatedAt: Long,
    val hasAudio: Boolean
)

fun SongDocument.lyricLines(): List<String> = blocks
    .mapNotNull { (it.payload as? BlockPayload.Text)?.content }
    .flatMap { it.lineSequence() }
    .map { it.trim() }
    .filter { it.isNotEmpty() }

fun SongDocument.hasNamedTitle(): Boolean {
    val named = title.trim()
    return named.isNotEmpty() && !named.equals("Untitled", ignoreCase = true)
}

fun SongDocument.isBlankDraft(): Boolean {
    if (hasNamedTitle()) return false
    if (blocks.any { it.payload is BlockPayload.Audio }) return false
    return lyricLines().isEmpty()
}

fun SongDocument.displayTitle(): String {
    if (hasNamedTitle()) return title.trim()
    return lyricLines().firstOrNull() ?: "Untitled"
}

fun SongDocument.previewText(): String {
    val title = displayTitle()
    val lines = lyricLines()
    val body = if (lines.firstOrNull() == title) lines.drop(1) else lines
    return body.take(3).joinToString("\n")
}

fun SongDocument.toSummary(): SongSummary = SongSummary(
    id = id,
    title = displayTitle(),
    preview = previewText(),
    updatedAt = updatedAt,
    hasAudio = blocks.any { it.payload is BlockPayload.Audio }
)
