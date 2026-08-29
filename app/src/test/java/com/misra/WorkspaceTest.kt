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
    fun insertAudioReplacesBlankLine() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("satır 1\nsatır 2\n\nsatır 3"))
        }
        val audio = audioBlock(
            audioId = "audio-1",
            name = "guitar-idea",
            durationMs = 34000L,
            origin = BlockPayload.Audio.ORIGIN_RECORDING
        )
        val blankLineCursor = "satır 1\nsatır 2\n".length
        val result = document.insertAudioAtCursor(textId, blankLineCursor, audio, now)
        document = result.document
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }.filter { it.content.isNotBlank() }
        assertEquals("satır 1\nsatır 2", lyrics.first().content)
        assertTrue(document.blocks.any { it.payload is BlockPayload.Audio })
        assertEquals("satır 3", lyrics.last().content)
    }

    @Test
    fun insertAudioOnFilledLineGoesAfterThatLine() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("verse\nchorus"))
        }
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        val result = document.insertAudioAtCursor(textId, 2, audio, now)
        document = result.document

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
            it.copy(payload = BlockPayload.Text("above\n\nbelow"))
        }
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        document = document.insertAudioAtCursor(textId, "above\n".length, audio, now).document
        val audioId = document.blocks.first { it.payload is BlockPayload.Audio }.id
        document = document.nudgeAudio(audioId, 1, now)
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }
        assertEquals("above", lyrics.first { it.content.isNotBlank() }.content)
        assertTrue(lyrics.any { it.content.isBlank() })
        assertEquals("below", lyrics.last { it.content.isNotBlank() }.content)
        assertTrue(document.blocks.any { it.payload is BlockPayload.Audio })
    }

    @Test
    fun nudgeAudioLandsOnBlankLineAbove() {
        val now = 1L
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        var document = SongDocument(
            id = "song",
            blocks = listOf(
                textBlock("satır 1\nsatır 2\n\nsatır 3\nsatır 4"),
                audio
            )
        ).asLyricFlow(now)
        document = document.nudgeAudio(audio.id, -1, now)
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }.filter { it.content.isNotBlank() }
        assertEquals("satır 1\nsatır 2", lyrics[0].content)
        assertTrue(document.blocks.any { it.payload is BlockPayload.Audio })
        assertEquals("satır 3\nsatır 4", lyrics[1].content)
    }

    @Test
    fun nudgeAudioLandsOnBlankLineBelow() {
        val now = 1L
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        var document = SongDocument(
            id = "song",
            blocks = listOf(
                textBlock("satır 1\nsatır 2"),
                audio,
                textBlock("satır 3\n\nsatır 4")
            )
        ).asLyricFlow(now)
        document = document.nudgeAudio(audio.id, 1, now)
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }.filter { it.content.isNotBlank() }
        assertEquals("satır 1\nsatır 2\nsatır 3", lyrics[0].content)
        assertTrue(document.blocks.any { it.payload is BlockPayload.Audio })
        assertEquals("satır 4", lyrics[1].content)
    }

    @Test
    fun removingAudioJoinsTheLyricAgain() {
        val now = 1L
        var document = emptySong(now)
        val textId = document.blocks.first().id
        document = document.update(textId, now) {
            it.copy(payload = BlockPayload.Text("hello\n\nworld"))
        }
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        document = document.insertAudioAtCursor(textId, "hello\n".length, audio, now).document
        val audioId = document.blocks.first { it.payload is BlockPayload.Audio }.id
        document = document.removeAudio(audioId, now)
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }
        assertEquals("hello", lyrics.first { it.content.isNotBlank() }.content)
        assertTrue(lyrics.any { it.content.isBlank() })
        assertEquals("world", lyrics.last { it.content.isNotBlank() }.content)
    }

    @Test
    fun draggingAudioToTheEndKeepsBlankLines() {
        val now = 1L
        val audio = audioBlock("a1", "idea", 1000L, BlockPayload.Audio.ORIGIN_IMPORT)
        var document = SongDocument(
            id = "song",
            blocks = listOf(
                audio,
                textBlock("satır 1\n\nsatır 2\n\nsatır 3")
            )
        ).asLyricFlow(now)
        repeat(8) { document = document.nudgeAudio(audio.id, 1, now) }
        val sandwichBlanks = document.blocks.indices.count { index ->
            document.blocks[index].isBlankText() &&
                document.blocks.take(index).any { it.isLyricText() } &&
                document.blocks.drop(index + 1).any { it.isLyricText() }
        }
        val lyrics = document.blocks.mapNotNull { it.payload as? BlockPayload.Text }.filter { it.content.isNotBlank() }
        assertEquals(listOf("satır 1", "satır 2", "satır 3"), lyrics.map { it.content })
        assertEquals(2, sandwichBlanks)
        assertTrue(document.blocks.indexOfFirst { it.payload is BlockPayload.Audio } >
            document.blocks.indexOfLast { it.isLyricText() })
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
