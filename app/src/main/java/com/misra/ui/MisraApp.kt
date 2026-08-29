package com.misra.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.misra.ui.library.LibraryScreen
import com.misra.ui.library.LibraryViewModel
import com.misra.ui.settings.AppSettingsViewModel
import com.misra.ui.settings.SettingsScreen
import com.misra.ui.theme.LocalLyricFontSize
import com.misra.ui.workspace.WorkspaceRoute

@Composable
fun MisraApp(
    libraryViewModel: LibraryViewModel = viewModel(),
    settingsViewModel: AppSettingsViewModel = viewModel()
) {
    val state by libraryViewModel.state.collectAsStateWithLifecycle()
    val lyricFontSizeSp by settingsViewModel.lyricFontSizeSp.collectAsStateWithLifecycle()
    val openSongId = state.openSongId

    CompositionLocalProvider(LocalLyricFontSize provides lyricFontSizeSp) {
        when {
            openSongId != null -> WorkspaceRoute(
                songId = openSongId,
                onBack = libraryViewModel::closeSong
            )
            state.settingsOpen -> SettingsScreen(
                lyricFontSizeSp = lyricFontSizeSp,
                onLyricFontSize = settingsViewModel::setLyricFontSize,
                onBack = libraryViewModel::closeSettings
            )
            else -> LibraryScreen(
                songs = state.songs,
                onOpenSettings = libraryViewModel::openSettings,
                onOpenSong = libraryViewModel::openSong,
                onCreateSong = libraryViewModel::createSong,
                onDeleteSongs = libraryViewModel::deleteSongs
            )
        }
    }
}
