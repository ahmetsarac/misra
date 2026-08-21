package com.misra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misra.ui.library.LibraryScreen
import com.misra.ui.library.LibraryViewModel
import com.misra.ui.workspace.WorkspaceRoute

@Composable
fun MisraApp(
    libraryViewModel: LibraryViewModel = viewModel()
) {
    val state by libraryViewModel.state.collectAsStateWithLifecycle()
    val openSongId = state.openSongId

    if (openSongId == null) {
        LibraryScreen(
            songs = state.songs,
            onOpenSong = libraryViewModel::openSong,
            onCreateSong = libraryViewModel::createSong,
            onDeleteSongs = libraryViewModel::deleteSongs
        )
    } else {
        WorkspaceRoute(
            songId = openSongId,
            onBack = libraryViewModel::closeSong
        )
    }
}
