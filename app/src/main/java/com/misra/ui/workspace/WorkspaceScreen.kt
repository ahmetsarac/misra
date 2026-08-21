package com.misra.ui.workspace

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misra.domain.model.SongDocument
import kotlinx.coroutines.delay

@Composable
fun WorkspaceRoute(
    songId: String,
    onBack: () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application
    val viewModel: WorkspaceViewModel = viewModel(
        key = songId,
        factory = WorkspaceViewModel.factory(application, songId)
    )
    val document by viewModel.document.collectAsStateWithLifecycle()
    val interaction by viewModel.interaction.collectAsStateWithLifecycle()
    val playback by viewModel.playback.collectAsStateWithLifecycle()
    val recording by viewModel.recording.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) viewModel.saveNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(interaction.userMessage) {
        if (interaction.userMessage != null) {
            delay(2400)
            viewModel.consumeMessage()
        }
    }

    BackHandler {
        viewModel.persist()
        onBack()
    }

    WorkspaceScreen(
        document = document,
        interaction = interaction,
        playback = playback,
        recording = recording,
        onBack = {
            viewModel.persist()
            onBack()
        },
        onDocumentTap = viewModel::onDocumentTapped,
        onSelectAudio = viewModel::onSelectAudio,
        onSelectText = viewModel::onSelectText,
        onNudgeAudio = viewModel::onNudgeAudio,
        onTitleChange = viewModel::onUpdateTitle,
        onTextChange = viewModel::onUpdateText,
        onCursorChange = viewModel::onUpdateCursor,
        onBackspaceAtStart = viewModel::onBackspaceAtStart,
        onAudioNameChange = viewModel::onUpdateAudioName,
        onPlayPause = viewModel::onPlayPause,
        onSeek = viewModel::onSeek,
        onVolume = viewModel::onVolume,
        onImportAudio = viewModel::onImportAudio,
        onStartRecording = viewModel::startRecording,
        onPermissionDenied = viewModel::onPermissionDenied,
        onPauseRecording = viewModel::pauseRecording,
        onResumeRecording = viewModel::resumeRecording,
        onStopRecording = viewModel::stopRecording,
        onDiscardRecording = viewModel::discardRecording,
        onDuplicate = viewModel::onDuplicateSelected,
        onDelete = viewModel::onDeleteSelected
    )
}

@Composable
fun WorkspaceScreen(
    document: SongDocument,
    interaction: InteractionState,
    playback: PlaybackState,
    recording: RecordingUiState,
    onBack: () -> Unit,
    onDocumentTap: () -> Unit,
    onSelectAudio: (String) -> Unit,
    onSelectText: (String) -> Unit,
    onNudgeAudio: (String, Int) -> Unit,
    onTitleChange: (String) -> Unit,
    onTextChange: (String, String) -> Unit,
    onCursorChange: (String, Int, Int) -> Unit,
    onBackspaceAtStart: (String) -> Unit,
    onAudioNameChange: (String, String) -> Unit,
    onPlayPause: (String) -> Unit,
    onSeek: (String, Long) -> Unit,
    onVolume: (Float) -> Unit,
    onImportAudio: (android.net.Uri) -> Unit,
    onStartRecording: () -> Unit,
    onPermissionDenied: () -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onImportAudio(uri)
    }
    val requestMic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onStartRecording() else onPermissionDenied()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        WorkspaceDocument(
            document = document,
            interaction = interaction,
            playback = playback,
            onDocumentTap = onDocumentTap,
            onSelectAudio = onSelectAudio,
            onSelectText = onSelectText,
            onNudgeAudio = onNudgeAudio,
            onTextChange = onTextChange,
            onCursorChange = onCursorChange,
            onBackspaceAtStart = onBackspaceAtStart,
            onAudioNameChange = onAudioNameChange,
            onPlayPause = onPlayPause,
            onSeek = onSeek,
            onVolume = onVolume,
            modifier = Modifier.fillMaxSize()
        )
        WorkspaceChrome(
            title = document.title,
            recording = recording,
            selectedAudioId = interaction.selectedAudioId,
            userMessage = interaction.userMessage,
            onBack = onBack,
            onTitleChange = onTitleChange,
            onMic = {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) onStartRecording() else requestMic.launch(Manifest.permission.RECORD_AUDIO)
            },
            onAddAudio = { pickAudio.launch("audio/*") },
            onPauseResumeRecording = {
                if (recording.isPaused) onResumeRecording() else onPauseRecording()
            },
            onStopRecording = onStopRecording,
            onDiscardRecording = onDiscardRecording,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        )
    }
}
