package com.misra.domain.workspace

import com.misra.data.MisraJson
import com.misra.domain.model.BlockPayload
import com.misra.domain.model.SongDocument
import com.misra.domain.model.formatDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceOperationsTest {
    @Test
    fun insertAudioSplitsTextAtCursor() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("versechorus"))
        }
        val audio = audioBlock(
            audioId = "audio-1",
            name = "guitar-idea",
            durationMs = 34000L,
            origin = BlockPayload.Audio.ORIGIN_RECORDING
        )
        val result = document.insertAudioAtCursor(textId, 5, audio, now)
        document = result.document

        assertEquals(3, document.blocks.size)
        assertEquals("verse", (document.blocks[0].payload as BlockPayload.Text).content)
        assertTrue(document.blocks[1].payload is BlockPayload.Audio)
        assertEquals("chorus", (document.blocks[2].payload as BlockPayload.Text).content)
        assertEquals(document.blocks[2].id, result.continueTextId)
    }

    @Test
    fun adjacentTextMergesIntoOneCanvasField() {
        val document = SongDocument(
            id = "song",
            blocks = listOf(textBlock("hello"), textBlock("world"))
        ).asLyricFlow(1L)
        assertEquals(1, document.blocks.size)
        assertEquals("hello\nworld", (document.blocks[0].payload as BlockPayload.Text).content)
    }

    @Test
    fun nudgeAudioMovesThroughTheLyric() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("abovebelow"))
        }
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        document = document.insertAudioAtCursor(textId, 5, audio, now).document
        val audioId = document.blocks.first { it.payload is BlockPayload.Audio }.id
        document = document.nudgeAudio(audioId, 1, now)
        assertTrue(document.blocks[0].payload is BlockPayload.Text)
        assertEquals("above\nbelow", (document.blocks[0].payload as BlockPayload.Text).content)
        assertTrue(document.blocks[1].payload is BlockPayload.Audio)
    }

    @Test
    fun removingAudioJoinsTheLyricAgain() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("hello world"))
        }
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        document = document.insertAudioAtCursor(textId, 6, audio, now).document
        val audioId = document.blocks.first { it.payload is BlockPayload.Audio }.id
        document = document.removeAudio(audioId, now)
        assertEquals(1, document.blocks.size)
        assertEquals("hello\nworld", (document.blocks[0].payload as BlockPayload.Text).content)
    }
}

class SongDocumentSerializationTest {
    @Test
    fun roundTripsTextAndAudio() {
        val document = SongDocument(
            id = "song-1",
            title = "Chorus sketch",
            updatedAt = 99L,
            blocks = listOf(
                textBlock("hello line"),
                audioBlock(
                    audioId = "a1",
                    name = "demo",
                    durationMs = 1234L,
                    origin = BlockPayload.Audio.ORIGIN_IMPORT
                )
            )
        ).asLyricFlow(99L)
        val encoded = MisraJson.encodeToString(SongDocument.serializer(), document)
        val decoded = MisraJson.decodeFromString(SongDocument.serializer(), encoded)
        assertEquals(document, decoded)
        assertTrue(encoded.contains("\"audioId\": \"a1\""))
        assertTrue(decoded.blocks.last().payload is BlockPayload.Text)
    }

    @Test
    fun spatialDocumentsBecomeASingleLyricFlow() {
        val top = textBlock("top").copy(y = 20f)
        val bottom = textBlock("bottom").copy(y = 200f)
        val document = SongDocument(
            id = "song-2",
            blocks = listOf(bottom, top)
        ).normalizedVertical()
        assertEquals(1, document.blocks.size)
        assertEquals("top\nbottom", (document.blocks[0].payload as BlockPayload.Text).content)
    }
}

class FormatTest {
    @Test
    fun formatsMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:05", formatDuration(5000))
        assertEquals("1:02", formatDuration(62000))
    }
}
