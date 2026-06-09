package com.musicstream.lyrics

import com.musicstream.data.api.ApiResult
import com.musicstream.data.api.InnertubeApi
import com.musicstream.domain.model.LyricLine
import com.musicstream.domain.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LyricsEngine — fetches, caches, and syncs lyrics to the current playback position.
 *
 * Architecture:
 *   PlayerController (position)  →  LyricsEngine (sync)  →  InnertubeApi (fetch)
 *
 * Features:
 *   - Time-synced lyrics (line highlighting at current position).
 *   - Plain lyrics fallback when timestamps are unavailable.
 *   - In-memory cache for the current session.
 */
@Singleton
class LyricsEngine @Inject constructor(
    private val api: InnertubeApi
) {
    private val lyricsCache = mutableMapOf<String, Lyrics>()

    suspend fun getLyrics(songId: String, browseId: String): ApiResult<Lyrics> =
        withContext(Dispatchers.IO) {
            lyricsCache[songId]?.let { return@withContext ApiResult.Success(it) }

            api.getLyrics(browseId).also { result ->
                if (result is ApiResult.Success) {
                    lyricsCache[songId] = result.data
                }
            }
        }

    /**
     * Given the current playback position, return the index of the active lyric line.
     */
    fun getCurrentLineIndex(lyrics: Lyrics, positionMs: Long): Int {
        if (!lyrics.isTimeSynced || lyrics.lines.isEmpty()) return -1
        var result = 0
        for (i in lyrics.lines.indices) {
            if (positionMs >= lyrics.lines[i].startMs) result = i
            else break
        }
        return result
    }

    /**
     * Return the next timestamp to schedule a UI update.
     */
    fun nextSyncMs(lyrics: Lyrics, positionMs: Long): Long {
        if (!lyrics.isTimeSynced) return Long.MAX_VALUE
        return lyrics.lines.firstOrNull { it.startMs > positionMs }?.startMs ?: Long.MAX_VALUE
    }

    fun clearCache() = lyricsCache.clear()
}
