package com.misra.data

import com.misra.domain.model.BlockPayload
import com.misra.domain.model.displayTitle
import com.misra.domain.model.isBlankDraft
import com.misra.domain.model.previewText
import com.misra.domain.model.toSummary
import com.misra.domain.workspace.emptySong
import com.misra.domain.workspace.textBlock
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSongStoreTest {
    @Test
    fun createsListsAndLoadsSongs() {
        val directory = File("build/tmp/song-store-test").apply {
            deleteRecursively()
            mkdirs()
        }
        val store = FileSongStore(directory)
        val first = kotlinx.coroutines.runBlocking { store.create(10L) }
        val second = emptySong(20L).copy(title = "Chorus")
        kotlinx.coroutines.runBlocking {
            store.save(first.copy(title = "Verse"))
            store.save(second)
        }

        val listed = kotlinx.coroutines.runBlocking { store.list() }
        assertEquals(2, listed.size)
        assertEquals("Chorus", listed.first().title)

        val loaded = kotlinx.coroutines.runBlocking { store.load(first.id) }
        assertNotNull(loaded)
        assertEquals(first.id, loaded?.id)
    }

    @Test
    fun blankDraftsAreNotKept() {
        val directory = File("build/tmp/song-store-blank").apply {
            deleteRecursively()
            mkdirs()
        }
        val store = FileSongStore(directory)
        val draft = kotlinx.coroutines.runBlocking { store.create(10L) }
        kotlinx.coroutines.runBlocking { store.save(draft) }

        assertTrue(draft.isBlankDraft())
        assertEquals(0, kotlinx.coroutines.runBlocking { store.list() }.size)
        assertNull(kotlinx.coroutines.runBlocking { store.load(draft.id) })
    }

    @Test
    fun migratesLegacySingleSong() {
        val songs = File("build/tmp/song-store-migrate/songs").apply {
            deleteRecursively()
            mkdirs()
        }
        val legacyDir = File("build/tmp/song-store-migrate/documents").apply { mkdirs() }
        val legacy = File(legacyDir, "song.json")
        val original = emptySong(5L).copy(title = "Old song")
        legacy.writeText(MisraJson.encodeToString(com.misra.domain.model.SongDocument.serializer(), original))

        val store = FileSongStore(songs, legacy)
        val listed = kotlinx.coroutines.runBlocking { store.list() }
        assertEquals(1, listed.size)
        assertEquals("Old song", listed.first().title)
    }
}

class SongSummaryTest {
    @Test
    fun blankDraftIgnoresUntitledAndWhitespace() {
        assertTrue(emptySong(1L).isBlankDraft())
        assertTrue(emptySong(1L).copy(title = "  ").isBlankDraft())
        assertTrue(
            emptySong(1L).copy(blocks = listOf(textBlock("   \n"))).isBlankDraft()
        )
        assertFalse(emptySong(1L).copy(title = "Chorus").isBlankDraft())
        assertFalse(
            emptySong(1L).copy(blocks = listOf(textBlock("hello"))).isBlankDraft()
        )
    }

    @Test
    fun displayTitleFallsBackToFirstLyricLine() {
        val song = emptySong(1L).copy(
            title = "Untitled",
            blocks = listOf(textBlock("Hello midnight"))
        )
        assertEquals("Hello midnight", song.displayTitle())
        assertTrue(song.previewText().isEmpty())
    }

    @Test
    fun summaryIncludesAudioFlag() {
        val song = emptySong(1L).copy(
            title = "Demo",
            blocks = listOf(
                textBlock("verse"),
                com.misra.domain.workspace.audioBlock(
                    audioId = "a",
                    name = "riff",
                    durationMs = 1L,
                    origin = BlockPayload.Audio.ORIGIN_IMPORT
                )
            )
        )
        assertTrue(song.toSummary().hasAudio)
        assertEquals("verse", song.previewText())
    }
}
