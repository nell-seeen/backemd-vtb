package com.musicstream.ui.screens.playlist

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.musicstream.domain.model.Song
import com.musicstream.ui.screens.home.SongListItem
import com.musicstream.ui.viewmodel.PlaylistViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistId: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    vm: PlaylistViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.playlist?.title ?: "Playlist") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { /* more */ }) { Icon(Icons.Filled.MoreVert, "More") }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    state.playlist?.let { playlist ->
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AsyncImage(
                                    model = playlist.thumbnailUrl,
                                    contentDescription = playlist.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(playlist.title, style = MaterialTheme.typography.titleLarge)
                                if (playlist.author.isNotEmpty()) {
                                    Text(playlist.author, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Text("${playlist.songCount} songs", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    FilledTonalButton(
                                        onClick = { state.songs.firstOrNull()?.let { vm.playAll(it, state.songs) } },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play")
                                    }
                                    OutlinedButton(
                                        onClick = { /* shuffle */ },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Shuffle, null); Spacer(Modifier.width(4.dp)); Text("Shuffle")
                                    }
                                }
                            }
                            Divider()
                        }
                    }

                    items(state.songs) { song ->
                        SongListItem(
                            song = song,
                            onSongClick = { onSongClick(song) },
                            trailingContent = {
                                IconButton(onClick = { vm.removeSong(song.id) }) {
                                    Icon(Icons.Filled.MoreVert, "More", modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
