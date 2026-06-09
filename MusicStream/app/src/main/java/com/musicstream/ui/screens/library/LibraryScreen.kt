package com.musicstream.ui.screens.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musicstream.domain.model.Playlist
import com.musicstream.domain.model.Song
import com.musicstream.ui.screens.home.SongListItem
import com.musicstream.ui.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (String) -> Unit,
    vm: LibraryViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Playlists", "Songs", "Downloads")

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                vm.createPlaylist(title)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, "Create playlist")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab) }
                    )
                }
            }

            when (selectedTab) {
                0 -> PlaylistsTab(
                    playlists = state.playlists,
                    onPlaylistClick = onPlaylistClick,
                    onDeletePlaylist = vm::deletePlaylist
                )
                1 -> LikedSongsTab(
                    songs = state.likedSongs,
                    onSongClick = onSongClick
                )
                2 -> DownloadsTab(
                    downloads = state.downloads,
                    onSongClick = { song ->
                        // Play local file
                        val localSong = Song(
                            id = song.songId,
                            title = song.title,
                            artistId = "",
                            artistName = song.artistName,
                            albumId = "",
                            albumTitle = song.albumTitle,
                            thumbnailUrl = song.thumbnailUrl,
                            durationMs = 0L
                        )
                        onSongClick(localSong)
                    },
                    onDeleteDownload = vm::deleteDownload
                )
            }
        }
    }
}

@Composable
fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit
) {
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.PlaylistAdd, null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text("No playlists yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(playlists, key = { it.id }) { playlist ->
            ListItem(
                headlineContent = { Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text("${playlist.songCount} songs",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
                leadingContent = {
                    AsyncImage(
                        model = playlist.thumbnailUrl.ifEmpty { null },
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                    )
                },
                modifier = Modifier.clickable { onPlaylistClick(playlist.id) }
            )
        }
    }
}

@Composable
fun LikedSongsTab(songs: List<Song>, onSongClick: (Song) -> Unit) {
    if (songs.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.FavoriteBorder, null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text("No liked songs yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(songs, key = { it.id }) { song ->
            SongListItem(song = song, onSongClick = { onSongClick(song) })
        }
    }
}

@Composable
fun DownloadsTab(
    downloads: List<com.musicstream.domain.model.DownloadedSong>,
    onSongClick: (com.musicstream.domain.model.DownloadedSong) -> Unit,
    onDeleteDownload: (String) -> Unit
) {
    if (downloads.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.DownloadDone, null, modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text("No downloads yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(downloads, key = { it.songId }) { download ->
            ListItem(
                headlineContent = { Text(download.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(download.artistName) },
                leadingContent = {
                    AsyncImage(
                        model = download.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onDeleteDownload(download.songId) }) {
                        Icon(Icons.Filled.Delete, "Delete")
                    }
                },
                modifier = Modifier.clickable { onSongClick(download) }
            )
        }
    }
}

@Composable
fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Playlist") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onCreate(title) },
                enabled = title.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
