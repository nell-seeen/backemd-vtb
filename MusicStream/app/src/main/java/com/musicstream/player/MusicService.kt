package com.musicstream.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicstream.MainActivity
import com.musicstream.data.repository.MusicRepository
import com.musicstream.domain.model.Song
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * MusicService — runs in the foreground and hosts the MediaSession.
 *
 * Architecture:
 *   - Extends MediaSessionService (Media3) for system integration.
 *   - Injects ExoPlayer and MediaSession from Hilt.
 *   - Resolves stream URLs on-demand before each track.
 */
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var player: ExoPlayer
    @Inject lateinit var mediaSession: MediaSession
    @Inject lateinit var queueManager: QueueManager
    @Inject lateinit var musicRepository: MusicRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val index = player.currentMediaItemIndex
            val item = queueManager.queue.value.getOrNull(index) ?: return
            prefetchNextStreamUrl(item.song)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                if (queueManager.hasNext()) {
                    queueManager.goToNext()?.let { playItem(it) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        player.addListener(playerListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }

    private fun playItem(item: com.musicstream.domain.model.QueueItem) {
        serviceScope.launch {
            val result = musicRepository.getStreamInfo(item.song.id)
            if (result is com.musicstream.data.api.ApiResult.Success) {
                val mediaItem = buildMediaItem(item.song, result.data.streamUrl)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
            }
        }
    }

    private fun prefetchNextStreamUrl(song: Song) {
        serviceScope.launch(Dispatchers.IO) {
            musicRepository.getStreamInfo(song.id)
        }
    }

    private fun buildMediaItem(song: Song, streamUrl: String): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artistName)
            .setAlbumTitle(song.albumTitle)
            .setArtworkUri(android.net.Uri.parse(song.thumbnailUrl))
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }

    companion object {
        const val ACTION_PLAY = "com.musicstream.PLAY"
        const val ACTION_PAUSE = "com.musicstream.PAUSE"
        const val ACTION_NEXT = "com.musicstream.NEXT"
        const val ACTION_PREV = "com.musicstream.PREV"
    }
}
