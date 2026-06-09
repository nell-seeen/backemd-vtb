package com.musicstream.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.api.ApiResult
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumUiState(val album: Album? = null, val isLoading: Boolean = true, val error: String? = null)

@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = checkNotNull(savedStateHandle["albumId"])
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init { loadAlbum() }

    private fun loadAlbum() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = musicRepository.getAlbum(albumId)) {
            is ApiResult.Success -> _uiState.update { it.copy(album = result.data, isLoading = false) }
            is ApiResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            else -> {}
        }
    }

    fun retry() = loadAlbum()
}
