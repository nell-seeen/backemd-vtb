package com.musicstream.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.*
import com.musicstream.player.PlayerController
import com.musicstream.player.QueueManager
import com.musicstream.recommendation.RecommendationEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository,
    private val recommendationEngine: RecommendationEngine
) : ViewModel() {

    val playerState: StateFlow<PlayerState> = playerController.playerState
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerState())

    val queue: StateFlow<List<QueueItem>> = queueManager.queue
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val currentIndex: StateFlow<Int> = queueManager.currentIndex
        .stateIn(viewModelScope, SharingStarted.Eagerly, -1)

    // ── Playback controls ──

    fun playSong(song: Song) = playerController.playSong(song)

    fun playQueue(songs: List<Song>, startIndex: Int = 0) =
        playerController.playQueue(songs, startIndex)

    fun togglePlayPause() = playerController.togglePlayPause()

    fun skipToNext() = playerController.skipToNext()

    fun skipToPrevious() = playerController.skipToPrevious()

    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)

    fun toggleRepeat() = playerController.toggleRepeat()

    fun toggleShuffle() = playerController.toggleShuffle()

    fun jumpToQueueItem(index: Int) = playerController.jumpToQueueItem(index)

    fun removeFromQueue(index: Int) = playerController.removeFromQueue(index)

    fun addToQueue(song: Song) = playerController.addToQueue(song)

    fun addNext(song: Song) = playerController.addNext(song)

    // ── Like / Unlike ──

    val isCurrentSongLiked: StateFlow<Boolean> = playerState
        .flatMapLatest { state ->
            val id = state.currentSong?.id ?: return@flatMapLatest flowOf(false)
            musicRepository.isLiked(id)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun toggleLike() = viewModelScope.launch {
        val song = playerState.value.currentSong ?: return@launch
        if (isCurrentSongLiked.value) {
            musicRepository.unlikeSong(song.id)
        } else {
            musicRepository.likeSong(song)
        }
    }

    // ── Radio / Auto-queue ──

    fun startRadio(song: Song) = viewModelScope.launch {
        val recommendations = recommendationEngine.recommend(song.id)
        if (recommendations.isNotEmpty()) {
            playerController.playQueue(listOf(song) + recommendations, 0)
        } else {
            playerController.playSong(song)
        }
    }
}
