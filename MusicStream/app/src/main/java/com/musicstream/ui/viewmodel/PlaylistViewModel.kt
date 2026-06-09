package com.musicstream.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.api.ApiResult
import com.musicstream.data.repository.PlaylistRepository
import com.musicstream.domain.model.Playlist
import com.musicstream.domain.model.Song
import com.musicstream.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val playerController: PlayerController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val playlistId: String = checkNotNull(savedStateHandle["playlistId"])
    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        loadPlaylist()
        observeLocalSongs()
    }

    private fun loadPlaylist() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = playlistRepository.getRemotePlaylist(playlistId)) {
            is ApiResult.Success -> _uiState.update { it.copy(playlist = result.data, isLoading = false) }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false) }
            else -> {}
        }
    }

    private fun observeLocalSongs() = viewModelScope.launch {
        playlistRepository.getPlaylistSongs(playlistId).collect { songs ->
            _uiState.update { it.copy(songs = songs) }
        }
    }

    fun playAll(first: Song, songs: List<Song>) = playerController.playQueue(songs, 0)

    fun removeSong(songId: String) = viewModelScope.launch {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
}
