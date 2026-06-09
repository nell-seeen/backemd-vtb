package com.musicstream.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.api.ApiResult
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.Artist
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistUiState(val artist: Artist? = null, val isLoading: Boolean = true, val error: String? = null)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val artistId: String = checkNotNull(savedStateHandle["artistId"])
    private val _uiState = MutableStateFlow(ArtistUiState())
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init { loadArtist() }

    private fun loadArtist() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        when (val result = musicRepository.getArtist(artistId)) {
            is ApiResult.Success -> _uiState.update { it.copy(artist = result.data, isLoading = false) }
            is ApiResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
            else -> {}
        }
    }
}
