package com.musicstream.ui.screens.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicstream.ui.viewmodel.LyricsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    onBack: () -> Unit,
    vm: LyricsViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsState()
    val playerState by vm.playerState.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to current line
    LaunchedEffect(state.currentLineIndex) {
        if (state.currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = (state.currentLineIndex - 2).coerceAtLeast(0)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lyrics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.LyricsOutlined, null, modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text("Lyrics not available", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                state.lyrics != null -> {
                    val lyrics = state.lyrics!!
                    if (lyrics.isTimeSynced) {
                        // Time-synced lyrics
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 64.dp, horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            itemsIndexed(lyrics.lines) { index, line ->
                                val isCurrentLine = index == state.currentLineIndex
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isCurrentLine) 22.sp else 18.sp
                                    ),
                                    color = if (isCurrentLine) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { vm.seekTo(index) }
                                )
                            }
                        }
                    } else {
                        // Plain lyrics
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(24.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(lyrics.lines) { line ->
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
