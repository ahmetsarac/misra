package com.misra.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SongDocument(
    val id: String,
    val title: String = "Untitled",
    val updatedAt: Long = 0L,
    val blocks: List<CanvasBlock> = emptyList()
)

@Serializable
data class CanvasBlock(
    val id: String,
    val payload: BlockPayload,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val z: Int = 0
)

@Serializable
sealed interface BlockPayload {
    @Serializable
    @SerialName("text")
    data class Text(
        val content: String = ""
    ) : BlockPayload

    @Serializable
    @SerialName("audio")
    data class Audio(
        val audioId: String,
        val name: String,
        val durationMs: Long = 0L,
        val origin: String = ORIGIN_IMPORT
    ) : BlockPayload {
        companion object {
            const val ORIGIN_IMPORT = "import"
            const val ORIGIN_RECORDING = "recording"
        }
    }
}
