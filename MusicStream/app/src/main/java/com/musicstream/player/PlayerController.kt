package com.musicstream.player

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.musicstream.data.api.ApiResult
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PlayerController — the single facade between the UI and the player.
 *
 * Architecture:
 *   UI ViewModel  →  PlayerController  →  ExoPlayer + QueueManager + MusicRepository
 *
 * Responsibilities:
 *   - Expose PlayerState as a StateFlow.
 *   - Delegate queue mutations to QueueManager.
 *   - Resolve stream URLs via MusicRepository before calling ExoPlayer.
 */
@Singleton
class PlayerController @Inject constructor(
    private val player: ExoPlayer,
    private val queueManager: QueueManager,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val queue: StateFlow<List<QueueItem>> = queueManager.queue
    val currentIndex: StateFlow<Int> = queueManager.currentIndex

    // ── Position ticker ──
    private var positionJob: Job? = null

    init {
        observePlayerEvents()
        startPositionTicker()
    }

    // ──────────────────────── Public API ────────────────────────

    fun playSong(song: Song, queue: List<Song> = listOf(song), startIndex: Int = 0) {
        queueManager.setQueue(queue, startIndex)
        loadAndPlay(song)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        queueManager.setQueue(songs, startIndex)
        loadAndPlay(songs[startIndex])
    }

    fun pause() = player.pause()

    fun resume() = player.play()

    fun togglePlayPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)

    fun seekBy(deltaMs: Long) = player.seekTo((player.currentPosition + deltaMs).coerceAtLeast(0))

    fun skipToNext() {
        queueManager.goToNext()?.let { loadAndPlay(it.song) }
    }

    fun skipToPrevious() {
        if (player.currentPosition > 3_000) {
            player.seekTo(0)
        } else {
            queueManager.goToPrevious()?.let { loadAndPlay(it.song) }
        }
    }

    fun jumpToQueueItem(index: Int) {
        queueManager.jumpTo(index)?.let { loadAndPlay(it.song) }
    }

    fun toggleRepeat() {
        queueManager.toggleRepeat()
        updatePlayerRepeat()
    }

    fun toggleShuffle() {
        queueManager.toggleShuffle()
        updatePlayerState()
    }

    fun addToQueue(song: Song) = queueManager.addToQueue(song)

    fun addNext(song: Song) = queueManager.addNext(song)

    fun removeFromQueue(index: Int) = queueManager.removeAt(index)

    fun clearQueue() {
        player.clearMediaItems()
        queueManager.clearQueue()
    }

    // ──────────────────────── Internal ────────────────────────

    private fun loadAndPlay(song: Song) {
        _playerState.update { it.copy(isLoading = true, currentSong = song) }
        scope.launch {
            when (val result = musicRepository.getStreamInfo(song.id)) {
                is ApiResult.Success -> {
                    val info = result.data
                    val mediaItem = androidx.media3.common.MediaItem.Builder()
                        .setMediaId(song.id)
                        .setUri(info.streamUrl)
                        .setMediaMetadata(
                            androidx.media3.common.MediaMetadata.Builder()
                                .setTitle(song.title)
                                .setArtist(song.artistName)
                                .build()
                        )
                        .build()
                    withContext(Dispatchers.Main) {
                        player.setMediaItem(mediaItem)
                        player.prepare()
                        player.play()
                    }
                }
                is ApiResult.Error -> {
                    _playerState.update { it.copy(isLoading = false) }
                }
                else -> {}
            }
        }
    }

    private fun observePlayerEvents() {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.update {
                    it.copy(
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        durationMs = player.duration.coerceAtLeast(0)
                    )
                }
            }
        })
    }

    private fun startPositionTicker() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _playerState.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                delay(500)
            }
        }
    }

    private fun updatePlayerRepeat() {
        player.repeatMode = when (queueManager.repeatMode.value) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
        }
        updatePlayerState()
    }

    private fun updatePlayerState() {
        _playerState.update {
            it.copy(
                repeatMode = queueManager.repeatMode.value,
                isShuffleEnabled = queueManager.isShuffleEnabled.value
            )
        }
    }
}
