package com.musicstream.ui.screens.album

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import com.musicstream.domain.model.Song
import com.musicstream.ui.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumId: String,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onArtistClick: (String) -> Unit,
    vm: AlbumViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(state.error!!)
                }
            }
            state.album != null -> {
                val album = state.album!!
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item {
                        // Album header
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = album.thumbnailUrl,
                                contentDescription = album.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(16.dp))
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(album.title, style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = album.artistName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { onArtistClick(album.artistId) }
                            )
                            if (album.year != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${album.year} · ${album.trackCount} songs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            // Play all button
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilledTonalButton(
                                    onClick = { album.songs.firstOrNull()?.let(onSongClick) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Play")
                                }
                                OutlinedButton(
                                    onClick = { /* shuffle */ },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Shuffle, null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                        Divider()
                    }

                    itemsIndexed(album.songs) { index, song ->
                        ListItem(
                            headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.width(24.dp)
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { /* more */ }) {
                                    Icon(Icons.Filled.MoreVert, "More", modifier = Modifier.size(18.dp))
                                }
                            },
                            modifier = Modifier.clickable { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}
