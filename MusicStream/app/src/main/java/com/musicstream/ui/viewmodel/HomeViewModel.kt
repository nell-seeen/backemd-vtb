package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.Album
import com.musicstream.domain.model.Song
import com.musicstream.recommendation.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentSongs: List<Song> = emptyList(),
    val dailyMix: List<Song> = emptyList(),
    val featuredAlbums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
        collectLikedSongs()
    }

    private fun loadHome() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val mix = recommendationEngine.getDailyMix(20)
            _uiState.update { it.copy(dailyMix = mix, isLoading = false) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = e.message) }
        }
    }

    private fun collectLikedSongs() = viewModelScope.launch {
        musicRepository.getLikedSongs().collect { liked ->
            _uiState.update { it.copy(recentSongs = liked) }
        }
    }

    fun refresh() = loadHome()
}
