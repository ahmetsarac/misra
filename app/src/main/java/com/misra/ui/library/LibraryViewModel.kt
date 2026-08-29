package com.misra.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.misra.data.FileAudioStore
import com.misra.data.FileSongStore
import com.misra.domain.model.BlockPayload
import com.misra.domain.model.SongSummary
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryState(
    val songs: List<SongSummary> = emptyList(),
    val openSongId: String? = null,
    val settingsOpen: Boolean = false
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val songStore = FileSongStore(
        songsDirectory = File(application.filesDir, "songs"),
        legacyFile = File(application.filesDir, "documents/song.json")
    )
    private val audioStore = FileAudioStore(application)

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) { songStore.list() }
            _state.update { it.copy(songs = songs) }
        }
    }

    fun openSong(id: String) {
        _state.update { it.copy(openSongId = id, settingsOpen = false) }
    }

    fun closeSong() {
        _state.update { it.copy(openSongId = null) }
        refresh()
    }

    fun openSettings() {
        _state.update { it.copy(settingsOpen = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(settingsOpen = false) }
    }

    fun createSong() {
        viewModelScope.launch {
            val created = withContext(Dispatchers.IO) {
                songStore.create(System.currentTimeMillis())
            }
            _state.update { it.copy(openSongId = created.id) }
            refresh()
        }
    }

    fun deleteSongs(ids: Collection<String>) {
        val idSet = ids.toSet()
        if (idSet.isEmpty()) return
        _state.update { it.copy(songs = it.songs.filterNot { song -> song.id in idSet }) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                idSet.forEach { id ->
                    val document = songStore.load(id)
                    document?.blocks
                        .orEmpty()
                        .mapNotNull { (it.payload as? BlockPayload.Audio)?.audioId }
                        .toSet()
                        .forEach { audioStore.delete(it) }
                    songStore.delete(id)
                }
            }
            refresh()
        }
    }
}
