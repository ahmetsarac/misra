package com.misra.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.misra.R
import com.misra.domain.model.SongSummary

@Composable
fun LibraryScreen(
    songs: List<SongSummary>,
    onOpenSong: (String) -> Unit,
    onCreateSong: () -> Unit,
    onDeleteSongs: (Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val card = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val haptic = LocalHapticFeedback.current
    var selecting by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun exitSelection() {
        selecting = false
        selectedIds = emptySet()
        confirmDelete = false
    }

    fun toggle(id: String) {
        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
    }

    BackHandler(enabled = selecting) { exitSelection() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(paper)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            if (selecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(card)
                            .clickable(onClick = { exitSelection() }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cancel),
                            tint = ink,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = pluralStringResource(
                            R.plurals.songs_selected,
                            selectedIds.size,
                            selectedIds.size
                        ),
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 22.sp,
                        color = ink,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 32.sp,
                    color = ink,
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 20.dp,
                        bottom = if (songs.isEmpty()) 4.dp else 16.dp
                    )
                )
            }
            if (songs.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty_library),
                    color = ink.copy(alpha = 0.45f),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongCard(
                        song = song,
                        cardColor = card,
                        ink = ink,
                        accent = accent,
                        selecting = selecting,
                        selected = song.id in selectedIds,
                        onClick = {
                            if (selecting) toggle(song.id) else onOpenSong(song.id)
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!selecting) {
                                selecting = true
                                selectedIds = setOf(song.id)
                            } else {
                                toggle(song.id)
                            }
                        }
                    )
                }
            }
        }
        if (selecting) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(card)
                    .clickable(enabled = selectedIds.isNotEmpty()) { confirmDelete = true }
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.delete),
                    color = if (selectedIds.isEmpty()) ink.copy(alpha = 0.35f) else ink,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(20.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(onClick = onCreateSong),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.new_song),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (confirmDelete && selectedIds.isNotEmpty()) {
            val count = selectedIds.size
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = {
                    Text(pluralStringResource(R.plurals.delete_songs_title, count, count))
                },
                text = { Text(stringResource(R.string.delete_song_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteSongs(selectedIds)
                            exitSelection()
                        }
                    ) {
                        Text(stringResource(R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCard(
    song: SongSummary,
    cardColor: Color,
    ink: Color,
    accent: Color,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(cardColor)
            .then(
                if (selected) {
                    Modifier.border(2.dp, accent, RoundedCornerShape(18.dp))
                } else {
                    Modifier
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = song.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (song.preview.isNotBlank()) {
                Text(
                    text = song.preview,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = ink.copy(alpha = 0.55f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            if (song.hasAudio) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = ink.copy(alpha = 0.35f),
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(16.dp)
                )
            }
        }
        if (selecting) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (selected) accent else ink.copy(alpha = 0.28f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
