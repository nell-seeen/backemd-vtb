package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.repository.MusicRepository
import com.musicstream.data.repository.PlaylistRepository
import com.musicstream.domain.model.*
import com.musicstream.download.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val likedSongs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val downloads: List<DownloadedSong> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> = combine(
        musicRepository.getLikedSongs(),
        playlistRepository.getLocalPlaylists(),
        downloadManager.getDownloads()
    ) { liked, playlists, downloads ->
        LibraryUiState(likedSongs = liked, playlists = playlists, downloads = downloads)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState(isLoading = true))

    fun createPlaylist(title: String) = viewModelScope.launch {
        playlistRepository.createPlaylist(title)
    }

    fun deletePlaylist(playlistId: String) = viewModelScope.launch {
        playlistRepository.deletePlaylist(playlistId)
    }

    fun deleteDownload(songId: String) = viewModelScope.launch {
        downloadManager.deleteDownload(songId)
    }
}
