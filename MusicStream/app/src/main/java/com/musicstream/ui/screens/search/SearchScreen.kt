package com.musicstream.ui.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicstream.data.api.SearchFilter
import com.musicstream.domain.model.Song
import com.musicstream.ui.screens.home.SongListItem
import com.musicstream.ui.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onSongClick: (Song) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    vm: SearchViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::setQuery,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(focusRequester),
            placeholder = { Text("Songs, artists, albums…") },
            leadingIcon = { Icon(Icons.Filled.Search, "Search") },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { vm.setQuery("") }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                vm.search()
                focusManager.clearFocus()
            })
        )

        // Filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            items(SearchFilter.values()) { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { vm.setFilter(filter) },
                    label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        // Content
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (state.query.isBlank()) {
                // Search history
                if (state.history.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recent searches", style = MaterialTheme.typography.titleSmall)
                            TextButton(onClick = vm::clearHistory) { Text("Clear") }
                        }
                    }
                    items(state.history) { query ->
                        ListItem(
                            headlineContent = { Text(query) },
                            leadingContent = { Icon(Icons.Filled.History, null) },
                            trailingContent = {
                                IconButton(onClick = { vm.removeFromHistory(query) }) {
                                    Icon(Icons.Filled.Close, "Remove")
                                }
                            },
                            modifier = Modifier.clickable { vm.setQuery(query); vm.search() }
                        )
                    }
                }
                // Suggestions
                if (state.suggestions.isNotEmpty()) {
                    item { Divider() }
                    items(state.suggestions) { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion) },
                            leadingContent = { Icon(Icons.Filled.TrendingUp, null) },
                            modifier = Modifier.clickable { vm.setQuery(suggestion); vm.search() }
                        )
                    }
                }
            } else if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                // Search results
                val result = state.result

                if (result.songs.isNotEmpty()) {
                    item { SearchSectionHeader("Songs") }
                    items(result.songs) { song ->
                        SongListItem(song = song, onSongClick = { onSongClick(song) })
                    }
                }
                if (result.albums.isNotEmpty()) {
                    item { SearchSectionHeader("Albums") }
                    items(result.albums) { album ->
                        ListItem(
                            headlineContent = { Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(album.artistName) },
                            modifier = Modifier.clickable { onAlbumClick(album.id) }
                        )
                    }
                }
                if (result.artists.isNotEmpty()) {
                    item { SearchSectionHeader("Artists") }
                    items(result.artists) { artist ->
                        ListItem(
                            headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            modifier = Modifier.clickable { onArtistClick(artist.id) }
                        )
                    }
                }
                if (result.playlists.isNotEmpty()) {
                    item { SearchSectionHeader("Playlists") }
                    items(result.playlists) { playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${playlist.songCount} songs · ${playlist.author}") },
                            modifier = Modifier.clickable { onPlaylistClick(playlist.id) }
                        )
                    }
                }
                if (result.songs.isEmpty() && result.albums.isEmpty() && result.artists.isEmpty() && result.playlists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No results for \"${state.query}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )
}
