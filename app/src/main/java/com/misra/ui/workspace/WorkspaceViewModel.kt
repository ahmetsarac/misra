package com.misra.ui.workspace

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.misra.audio.AppAudioPlayer
import com.misra.audio.AppAudioRecorder
import com.misra.data.FileAudioStore
import com.misra.data.FileSongStore
import com.misra.domain.model.BlockPayload
import com.misra.domain.model.CanvasBlock
import com.misra.domain.model.SongDocument
import com.misra.domain.workspace.AudioInsertResult
import com.misra.domain.workspace.audioBlock
import com.misra.domain.workspace.block
import com.misra.domain.workspace.duplicateAudio
import com.misra.domain.workspace.emptySong
import com.misra.domain.workspace.insertAudioAfter
import com.misra.domain.workspace.insertAudioAtCursor
import com.misra.domain.workspace.nudgeAudio
import com.misra.domain.workspace.removeAudio
import com.misra.domain.workspace.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

data class CursorRange(val start: Int = 0, val end: Int = 0)

data class InteractionState(
    val selectedAudioId: String? = null,
    val activeTextId: String? = null,
    val cursors: Map<String, CursorRange> = emptyMap(),
    val restoreFocusGen: Int = 0,
    val userMessage: String? = null
)

data class PlaybackState(
    val blockId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

data class RecordingUiState(
    val active: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L
)

class WorkspaceViewModel(
    application: Application,
    private val songId: String
) : AndroidViewModel(application) {

    private val songStore = FileSongStore(
        songsDirectory = File(application.filesDir, "songs"),
        legacyFile = File(application.filesDir, "documents/song.json")
    )
    private val audioStore = FileAudioStore(application)
    private val player = AppAudioPlayer().also { engine ->
        engine.onComplete = {
            _playback.update { it.copy(isPlaying = false, positionMs = it.durationMs) }
            playbackTicker?.cancel()
        }
    }
    private val recorder = AppAudioRecorder(application)

    private val _document = MutableStateFlow(emptySong(now()).copy(id = songId))
    val document: StateFlow<SongDocument> = _document.asStateFlow()

    private val _interaction = MutableStateFlow(InteractionState())
    val interaction: StateFlow<InteractionState> = _interaction.asStateFlow()

    private val _playback = MutableStateFlow(PlaybackState())
    val playback: StateFlow<PlaybackState> = _playback.asStateFlow()

    private val _recording = MutableStateFlow(RecordingUiState())
    val recording: StateFlow<RecordingUiState> = _recording.asStateFlow()

    private var saveGeneration = 0
    private var playbackTicker: Job? = null
    private var recordingTicker: Job? = null

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { songStore.load(songId) }
            val document = loaded ?: emptySong(now()).copy(id = songId)
            _document.value = document
            val firstText = document.blocks.firstOrNull { it.payload is BlockPayload.Text }
            _interaction.update { it.copy(activeTextId = firstText?.id) }
        }
    }

    fun onDocumentTapped() {
        val textId = _document.value.blocks.lastOrNull { it.payload is BlockPayload.Text }?.id ?: return
        val content = (_document.value.block(textId)?.payload as? BlockPayload.Text)?.content.orEmpty()
        val existing = _interaction.value.cursors[textId]
        val cursor = existing ?: CursorRange(content.length, content.length)
        _interaction.update { state ->
            state.copy(
                selectedAudioId = null,
                activeTextId = textId,
                cursors = state.cursors + (textId to cursor),
                restoreFocusGen = state.restoreFocusGen + 1
            )
        }
    }

    fun onSelectAudio(id: String) {
        if (_document.value.block(id)?.payload !is BlockPayload.Audio) return
        _interaction.update { it.copy(selectedAudioId = id) }
    }

    fun onSelectText(id: String) {
        _interaction.update { it.copy(activeTextId = id, selectedAudioId = null) }
    }

    fun onNudgeAudio(id: String, steps: Int) {
        _document.value = _document.value.nudgeAudio(id, steps, now())
        scheduleSave()
    }

    fun onUpdateTitle(title: String) {
        _document.value = _document.value.copy(title = title, updatedAt = now())
        scheduleSave()
    }

    fun onUpdateText(id: String, content: String) {
        _document.value = _document.value.update(id, now()) { block ->
            val payload = block.payload as? BlockPayload.Text ?: return@update block
            block.copy(payload = payload.copy(content = content))
        }
        scheduleSave()
    }

    fun onUpdateCursor(id: String, start: Int, end: Int) {
        _interaction.update { state ->
            state.copy(
                activeTextId = id,
                cursors = state.cursors + (id to CursorRange(start, end))
            )
        }
    }

    fun onBackspaceAtStart(id: String) {
        val blocks = _document.value.blocks
        val index = blocks.indexOfFirst { it.id == id }
        if (index <= 0) return
        val previousText = blocks.take(index).lastOrNull { it.payload is BlockPayload.Text } ?: return
        val cursor = (previousText.payload as BlockPayload.Text).content.length
        _interaction.update {
            it.copy(
                activeTextId = previousText.id,
                selectedAudioId = null,
                cursors = it.cursors + (previousText.id to CursorRange(cursor, cursor)),
                restoreFocusGen = it.restoreFocusGen + 1
            )
        }
    }

    fun onUpdateAudioName(id: String, name: String) {
        _document.value = _document.value.update(id, now()) { block ->
            val payload = block.payload as? BlockPayload.Audio ?: return@update block
            block.copy(payload = payload.copy(name = name))
        }
        scheduleSave()
    }

    fun onImportAudio(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val stored = withContext(Dispatchers.IO) {
                    audioStore.importFromUri(uri, "Audio")
                }
                applyAudioInsert(
                    audioBlock(
                        audioId = stored.id,
                        name = stored.name,
                        durationMs = stored.durationMs,
                        origin = BlockPayload.Audio.ORIGIN_IMPORT
                    )
                )
            }.onFailure {
                _interaction.update { state ->
                    state.copy(userMessage = "Couldn't add that audio file.")
                }
            }
        }
    }

    fun onPermissionDenied() {
        _interaction.update { it.copy(userMessage = "Microphone access is needed to record.") }
    }

    fun consumeMessage() {
        _interaction.update { it.copy(userMessage = null) }
    }

    fun startRecording() {
        if (_recording.value.active) return
        val file = File(getApplication<Application>().cacheDir, "recording-in-progress.m4a")
        runCatching {
            recorder.start(file)
            _recording.value = RecordingUiState(active = true)
            startRecordingTicker()
        }.onFailure {
            recorder.cancel()
            _interaction.update { state -> state.copy(userMessage = "Couldn't start recording.") }
        }
    }

    fun pauseRecording() {
        if (!_recording.value.active || _recording.value.isPaused) return
        recorder.pause()
        _recording.update { it.copy(isPaused = true, elapsedMs = recorder.elapsedMs) }
    }

    fun resumeRecording() {
        if (!_recording.value.active || !_recording.value.isPaused) return
        recorder.resume()
        _recording.update { it.copy(isPaused = false) }
        startRecordingTicker()
    }

    fun discardRecording() {
        recordingTicker?.cancel()
        recorder.cancel()
        _recording.value = RecordingUiState()
    }

    fun stopRecording() {
        settleRecording()
    }

    fun onDeleteSelected() {
        val id = _interaction.value.selectedAudioId ?: return
        deleteAudio(id)
    }

    fun onDuplicateSelected() {
        val id = _interaction.value.selectedAudioId ?: return
        val before = _document.value.blocks.map { it.id }.toSet()
        _document.value = _document.value.duplicateAudio(id, now())
        val newId = _document.value.blocks.firstOrNull { it.id !in before }?.id
        if (newId != null) {
            _interaction.update { it.copy(selectedAudioId = newId) }
        }
        scheduleSave()
    }

    fun onPlayPause(blockId: String) {
        val block = _document.value.block(blockId) ?: return
        val audio = block.payload as? BlockPayload.Audio ?: return
        val file = audioStore.resolve(audio.audioId) ?: run {
            _interaction.update { it.copy(userMessage = "Audio file is missing.") }
            return
        }
        val current = _playback.value
        if (current.blockId == blockId && current.isPlaying) {
            player.pause()
            playbackTicker?.cancel()
            _playback.update { it.copy(isPlaying = false, positionMs = player.positionMs) }
            return
        }
        val startFrom = if (current.blockId == blockId) current.positionMs else 0L
        runCatching {
            player.play(file, startFrom)
            _playback.update {
                it.copy(
                    blockId = blockId,
                    isPlaying = true,
                    positionMs = player.positionMs,
                    durationMs = player.durationMs.takeIf { d -> d > 0 } ?: audio.durationMs
                )
            }
            startPlaybackTicker()
        }.onFailure {
            _interaction.update { state -> state.copy(userMessage = "Couldn't play that audio.") }
        }
    }

    fun onSeek(blockId: String, positionMs: Long) {
        val block = _document.value.block(blockId) ?: return
        val audio = block.payload as? BlockPayload.Audio ?: return
        val file = audioStore.resolve(audio.audioId) ?: return
        runCatching { player.ensure(file) }
        player.seek(positionMs)
        _playback.update {
            it.copy(
                blockId = blockId,
                positionMs = positionMs,
                durationMs = player.durationMs.takeIf { d -> d > 0 } ?: audio.durationMs
            )
        }
    }

    fun saveNow() {
        settleRecording()
        val snapshot = _document.value
        viewModelScope.launch(Dispatchers.IO) {
            songStore.save(snapshot)
        }
    }

    fun persist() {
        settleRecording()
        runCatching {
            kotlinx.coroutines.runBlocking {
                songStore.save(_document.value)
            }
        }
    }

    override fun onCleared() {
        persist()
        playbackTicker?.cancel()
        recordingTicker?.cancel()
        player.reset()
        recorder.cancel()
        super.onCleared()
    }

    private fun applyAudioInsert(audio: CanvasBlock) {
        val selectedAudio = _interaction.value.selectedAudioId
        val result: AudioInsertResult = if (selectedAudio != null) {
            _document.value.insertAudioAfter(selectedAudio, audio, now())
        } else {
            val textId = _interaction.value.activeTextId
            val cursor = textId?.let { id ->
                _interaction.value.cursors[id]?.end
                    ?: (_document.value.block(id)?.payload as? BlockPayload.Text)?.content?.length
            } ?: 0
            _document.value.insertAudioAtCursor(textId, cursor, audio, now())
        }
        _document.value = result.document
        _interaction.update {
            it.copy(
                selectedAudioId = audio.id,
                activeTextId = result.continueTextId,
                cursors = it.cursors + (result.continueTextId to CursorRange()),
                restoreFocusGen = it.restoreFocusGen + 1
            )
        }
        scheduleSave()
    }

    private fun deleteAudio(id: String) {
        val block = _document.value.block(id) ?: return
        val audio = block.payload
        if (audio !is BlockPayload.Audio) return
        if (_playback.value.blockId == id) {
            stopPlayback()
        }
        val previousText = _document.value.blocks
            .take(_document.value.blocks.indexOfFirst { it.id == id }.coerceAtLeast(0))
            .lastOrNull { it.payload is BlockPayload.Text }
        val cursor = (previousText?.payload as? BlockPayload.Text)?.content?.length ?: 0
        _document.value = _document.value.removeAudio(id, now())
        val stillUsed = _document.value.blocks.any { other ->
            val payload = other.payload as? BlockPayload.Audio
            payload?.audioId == audio.audioId
        }
        if (!stillUsed) {
            viewModelScope.launch(Dispatchers.IO) { audioStore.delete(audio.audioId) }
        }
        val focusId = previousText?.id
            ?: _document.value.blocks.lastOrNull { it.payload is BlockPayload.Text }?.id
        _interaction.update { state ->
            state.copy(
                selectedAudioId = null,
                activeTextId = focusId,
                cursors = if (focusId != null) {
                    state.cursors + (focusId to CursorRange(cursor, cursor))
                } else {
                    state.cursors
                },
                restoreFocusGen = if (focusId != null) state.restoreFocusGen + 1 else state.restoreFocusGen
            )
        }
        scheduleSave()
    }

    private fun stopPlayback() {
        playbackTicker?.cancel()
        player.reset()
        _playback.update { it.copy(blockId = null, isPlaying = false, positionMs = 0L, durationMs = 0L) }
    }

    private fun startPlaybackTicker() {
        playbackTicker?.cancel()
        playbackTicker = viewModelScope.launch {
            while (isActive && player.isPlaying) {
                _playback.update { it.copy(positionMs = player.positionMs, isPlaying = true) }
                delay(80.milliseconds)
            }
        }
    }

    private fun startRecordingTicker() {
        recordingTicker?.cancel()
        recordingTicker = viewModelScope.launch {
            while (isActive && _recording.value.active && !_recording.value.isPaused) {
                _recording.update { it.copy(elapsedMs = recorder.elapsedMs) }
                delay(100.milliseconds)
            }
        }
    }

    private fun settleRecording() {
        if (!_recording.value.active) return
        recordingTicker?.cancel()
        val elapsed = recorder.elapsedMs
        val file = runCatching { recorder.stop() }.getOrElse {
            recorder.cancel()
            _recording.value = RecordingUiState()
            _interaction.update { it.copy(userMessage = "Couldn't save the recording.") }
            return
        }
        _recording.value = RecordingUiState()
        if (elapsed < 400L) {
            file.delete()
            return
        }
        runCatching {
            val stored = kotlinx.coroutines.runBlocking {
                audioStore.saveRecording(file, defaultRecordingName())
            }
            applyAudioInsert(
                audioBlock(
                    audioId = stored.id,
                    name = stored.name,
                    durationMs = stored.durationMs.takeIf { it > 0 } ?: elapsed,
                    origin = BlockPayload.Audio.ORIGIN_RECORDING
                )
            )
        }.onFailure {
            _interaction.update { it.copy(userMessage = "Couldn't save the recording.") }
        }
    }

    private fun scheduleSave() {
        val generation = ++saveGeneration
        viewModelScope.launch {
            delay(450.milliseconds)
            if (generation == saveGeneration) {
                withContext(Dispatchers.IO) { songStore.save(_document.value) }
            }
        }
    }

    private fun defaultRecordingName(): String {
        val stamp = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        return "Idea $stamp"
    }

    private fun now(): Long = System.currentTimeMillis()

    companion object {
        fun factory(application: Application, songId: String): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkspaceViewModel(application, songId) as T
                }
            }
        }
    }
}
