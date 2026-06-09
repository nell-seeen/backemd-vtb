package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.api.ApiResult
import com.musicstream.domain.model.Lyrics
import com.musicstream.domain.model.Song
import com.musicstream.lyrics.LyricsEngine
import com.musicstream.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val lyrics: Lyrics? = null,
    val currentLineIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricsEngine: LyricsEngine,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    val playerState = playerController.playerState

    init {
        // Observe current song and load lyrics
        viewModelScope.launch {
            playerController.playerState
                .map { it.currentSong }
                .distinctUntilChanged { old, new -> old?.id == new?.id }
                .collect { song -> song?.let { loadLyrics(it) } }
        }

        // Sync line highlight with playback position
        viewModelScope.launch {
            playerController.playerState.collect { state ->
                val lyrics = _uiState.value.lyrics ?: return@collect
                val idx = lyricsEngine.getCurrentLineIndex(lyrics, state.positionMs)
                _uiState.update { it.copy(currentLineIndex = idx) }
            }
        }
    }

    private fun loadLyrics(song: Song) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        // BrowseId for lyrics is constructed from videoId
        val browseId = "MPED${song.id}"
        when (val result = lyricsEngine.getLyrics(song.id, browseId)) {
            is ApiResult.Success -> _uiState.update { it.copy(lyrics = result.data, isLoading = false) }
            is ApiResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            else -> {}
        }
    }

    fun seekTo(lineIndex: Int) {
        val line = _uiState.value.lyrics?.lines?.getOrNull(lineIndex) ?: return
        playerController.seekTo(line.startMs)
    }
}
